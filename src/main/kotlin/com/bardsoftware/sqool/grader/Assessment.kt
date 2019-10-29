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

package com.bardsoftware.sqool.grader

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.api.core.ApiService
import com.google.cloud.ServiceOptions
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.common.util.concurrent.MoreExecutors
import com.google.pubsub.v1.ProjectSubscriptionName
import java.util.concurrent.CompletableFuture

data class TaskId(
    @JsonProperty("course") val course: String,
    @JsonProperty("module") val module: String,
    @JsonProperty("task") val task: String,
    @JsonProperty("dbms") val dbms: String = "postgres")

data class AssessmentPubSubTask(
    @JsonProperty("id") val id: TaskId,
    @JsonProperty("submission") val submission: String,
    @JsonProperty("isDdl")      val isDdl: Boolean
)

data class AssessmentPubSubResp(
    @JsonProperty("requestId") val requestId: String,
    @JsonProperty("score") val score: Int,
    @JsonProperty("errors") val errors: String = "",
    @JsonProperty("resultLines") val resultLines: List<Map<String, Any>> = emptyList())

open class PubSubSubscriber(subscription: String, val receiver: MessageReceiver) {
  val subscriptionFullName = ProjectSubscriptionName.of(ServiceOptions.getDefaultProjectId(), subscription)
  fun listen(onShutdown: CompletableFuture<Any>) {
    var subscriber: Subscriber? = null
    try {
      subscriber = Subscriber.newBuilder(subscriptionFullName, receiver).build()
      subscriber.addListener(object : ApiService.Listener() {
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

