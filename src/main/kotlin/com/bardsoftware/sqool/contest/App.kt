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

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import spark.template.freemarker.FreeMarkerEngine

class Flags(parser: ArgParser) {
  val postgresAddress by parser.storing("--pg-address", help = "").default("localhost")
  val postgresUser by parser.storing("--pg-user", help = "").default("postgres")
  val postgresPort by parser.storing("--pg-port", help = "").default("5432")
  val postgresDatabase by parser.storing("--pg-database", help = "").default("")
  val postgresPassword by parser.storing("--pg-password", help = "").default("")
  val postgresQaContainer by parser.storing("--pg-qa-container", help = "Postgres container which is used for contest image QA check.").default("")
  val pubTasksTopic by parser.storing("--pub", help = "").default("")
  val subResultsSubscription by parser.storing("--sub", help = "").default("")
}

val freemarkerConfig = Configuration(Configuration.getVersion()).apply {
  templateLoader = ClassTemplateLoader(Flags::class.java, "/tmpl/")
  defaultEncoding = "UTF-8"
}

val freemarker = FreeMarkerEngine(freemarkerConfig)
