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
  val pubTasksTopic by parser.storing("--pub", help = "").default("")
  val subResultsSubscription by parser.storing("--sub", help = "").default("")
}

val freemarkerConfig = Configuration(Configuration.getVersion()).apply {
  templateLoader = ClassTemplateLoader(Flags::class.java, "/tmpl/")
  defaultEncoding = "UTF-8"
}

val freemarker = FreeMarkerEngine(freemarkerConfig)
