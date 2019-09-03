package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.grader.AssessmentPubSubResp
import com.bardsoftware.sqool.grader.AssessmentPubSubTask
import com.bardsoftware.sqool.grader.PubSubSubscriber
import com.bardsoftware.sqool.grader.TaskId
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.ServiceOptions
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import com.google.pubsub.v1.TopicName
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val MAPPER = ObjectMapper()

/**
 * @author dbarashev@bardsoftware.com
 */
interface AssessorApi {
  fun submit(contestId: String, taskId: String, solution: String, consumer: (String) -> Unit)
}

class AssessorApiVoid : AssessorApi {
  override fun submit(contestId: String, taskId: String, solution: String, consumer: (String) -> Unit) {
    println("""
      Submitting solution of task $taskId in contest $contestId
      This is an assessor stub. It will not do anything""".trimIndent())
    consumer("${taskId}_${System.currentTimeMillis().toString(16)}")
  }
}

class ResultMessageReceiver(val responseConsumer: (AssessmentPubSubResp) -> Unit) : MessageReceiver {
  private val responseIds = mutableSetOf<String>()
  override fun receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
    try {
      val parsedMsg = MAPPER.readValue(message.data.toByteArray(), AssessmentPubSubResp::class.java)
      responseIds.add(parsedMsg.requestId)
      responseConsumer(parsedMsg)
      consumer.ack()
    } catch (ex: Exception) {
      consumer.nack()
      println(ex)
    }
  }

  fun hasResponse(msgId: String): Boolean {
    return this.responseIds.contains(msgId)
  }
}

class AssessorPubSub(val topicId: String, private val responseConsumer: (AssessmentPubSubResp) -> Unit) : AssessorApi {
  private val executor = Executors.newSingleThreadExecutor()
  private val timeoutScheduler = Executors.newScheduledThreadPool(1)
  private val receiver = ResultMessageReceiver(responseConsumer)

  init {
    val subscriber = PubSubSubscriber("dbms2019-frontend", receiver)
    val onShutdown = CompletableFuture<Any>()
    Runtime.getRuntime().addShutdownHook(Thread(Runnable {
      onShutdown.complete(null)
    }))
    subscriber.listen(onShutdown)
  }

  override fun submit(contestId: String, taskId: String, solution: String, consumer: (String) -> Unit) {
    try {
      val id = TaskId(course = contestId, module = "Variant_A", task = taskId)
      val pubsubTask = AssessmentPubSubTask(id = id, submission = solution)
      val data = MAPPER.writeValueAsBytes(pubsubTask)
      val message = PubsubMessage.newBuilder().setData(ByteString.copyFrom(data)).build()
      println("============ Publishing message")
      val topicName = TopicName.create(ServiceOptions.getDefaultProjectId(), topicId)
      val publisher = Publisher.defaultBuilder(topicName).build()
      val future = publisher.publish(message)
      future.addListener(Runnable {
        val msgId = future.get()
        consumer(msgId)
        timeoutScheduler.schedule(this.createTimeoutCommand(msgId), 30, TimeUnit.SECONDS)
      }, executor)
    } catch (ex: Exception) {
      ex.printStackTrace()
    }
  }

  private fun createTimeoutCommand(msgId: String): Runnable {
    return Runnable {
      if (!this.receiver.hasResponse(msgId)) {
        this.responseConsumer(AssessmentPubSubResp(msgId, 0, "Кажется, что-то отвалилось"))
      }
    }
  }
}
