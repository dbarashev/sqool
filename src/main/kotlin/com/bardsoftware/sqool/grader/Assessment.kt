package com.bardsoftware.sqool.grader

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.api.core.ApiService
import com.google.cloud.ServiceOptions
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.common.io.Files
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.pubsub.v1.SubscriptionName
import java.io.File
import java.util.concurrent.CompletableFuture

data class TaskId(
    @JsonProperty("course") val course: String,
    @JsonProperty("module") val module: String,
    @JsonProperty("task")   val task: String,
    @JsonProperty("dbms")   val dbms: String = "postgres")

data class AssessmentPubSubTask(
    @JsonProperty("id")         val id: TaskId,
    @JsonProperty("submission") val submission: String)

data class AssessmentPubSubResp(
    @JsonProperty("requestId")   val requestId: String,
    @JsonProperty("score")       val score: Int,
    @JsonProperty("errors")      val errors: String = "",
    @JsonProperty("resultLines") val resultLines: List<Map<String, Any>> = emptyList())

open class PubSubSubscriber(subscription: String, val receiver: MessageReceiver) {
  val subscriptionFullName = SubscriptionName.create(ServiceOptions.getDefaultProjectId(), subscription)
  fun listen(onShutdown: CompletableFuture<Any>) {
    var subscriber : Subscriber? = null
    try {
      subscriber = Subscriber.defaultBuilder(subscriptionFullName, receiver).build()
      subscriber.addListener( object : ApiService.Listener() {
        override fun failed(from: ApiService.State?, failure: Throwable?) {
          println("FAILED!")
        }
      }, MoreExecutors.directExecutor())
      subscriber.startAsync().awaitRunning()
      println("Started subscriber on subscription $subscriptionFullName")
      onShutdown.thenRun { subscriber?.stopAsync() }
    } catch (ex: Exception) {
      subscriber?.stopAsync()
    }
  }
}

