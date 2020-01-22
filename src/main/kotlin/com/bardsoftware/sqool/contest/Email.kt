/*
 * Copyright (c) BarD Software s.r.o 2020
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

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import okhttp3.*
import okhttp3.Route
import java.io.IOException

private var httpClient: OkHttpClient? = null

fun initEmail(apiKey: String) {
  httpClient = OkHttpClient.Builder().authenticator(object : Authenticator {
    @Throws(IOException::class)
    override fun authenticate(route: Route?, response: Response): Request? {
      if (response.request.header("Authorization") != null) {
        return null // Give up, we've already attempted to authenticate.
      }
      return response.request.newBuilder()
          .header("Authorization", Credentials.basic("api", apiKey))
          .build()
    }
  }).build()

}
/**
 * @author dbarashev@bardsoftware.com
 */
fun sendEmail(markdown: String, kv: Map<String, String>) {
  val html = """
${markdown2html(markdown)}""".trimIndent()

  val formBuilder = FormBody.Builder()
  kv.forEach {(key, value) ->
    formBuilder.add(key, value)
  }
  val req = Request.Builder()
      .url("https://api.mailgun.net/v3/mg.barashev.net/messages")
      .post(formBuilder.add("html", html).build())
      .build()
  httpClient?.let {
    it.newCall(req).execute().use { response ->
      if (!response.isSuccessful) throw IOException("Unexpected code $response")
      println(response.body!!.string())
    }
  }
}

private val ourParser = Parser.builder(MutableDataSet()).build()
private val ourRenderer = HtmlRenderer.builder(MutableDataSet()).build()

fun markdown2html(mdReview: String): String {
  val document = ourParser.parse(mdReview)
  return ourRenderer.render(document)
}
