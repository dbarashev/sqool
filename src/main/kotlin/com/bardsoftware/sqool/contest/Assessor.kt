/*
 * Copyright (c) BarD Software s.r.o 2019
 *
 * This file is a part of SQooL, a service for running SQL contests.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  fun submit(contestCode: String, variantName: String, taskName: String, solution: String, consumer: (String) -> Unit)
}

class AssessorApiVoid : AssessorApi {
  override fun submit(contestCode: String, variantName: String, taskName: String, solution: String, consumer: (String) -> Unit) {
    println("""
      Submitting solution of task $taskName in variant $variantName of contest $contestCode
      This is an assessor stub. It will not do anything""".trimIndent())
    consumer("${taskName}_${System.currentTimeMillis().toString(16)}")
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

class AssessorPubSub(private val topicId: String,
                     private val subscriptionId: String,
                     private val responseConsumer: (AssessmentPubSubResp) -> Unit) : AssessorApi {
  private val executor = Executors.newSingleThreadExecutor()
  private val timeoutScheduler = Executors.newScheduledThreadPool(1)
  private val receiver = ResultMessageReceiver(responseConsumer)

  init {
    val subscriber = PubSubSubscriber(subscriptionId, receiver)
    val onShutdown = CompletableFuture<Any>()
    Runtime.getRuntime().addShutdownHook(Thread(Runnable {
      onShutdown.complete(null)
    }))
    subscriber.listen(onShutdown)
  }

  override fun submit(contestCode: String, variantName: String, taskName: String, solution: String, consumer: (String) -> Unit) {
    try {
      val id = TaskId(course = contestCode, module = variantName, task = taskName)
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
