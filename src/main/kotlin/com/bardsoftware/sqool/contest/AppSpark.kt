package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.admin.TaskAllHandler
import com.xenomachina.argparser.ArgParser
import com.zaxxer.hikari.HikariDataSource
import org.apache.http.client.utils.URLEncodedUtils
import org.jetbrains.exposed.sql.Database
import spark.ModelAndView
import spark.Request
import spark.Response
import spark.Session
import spark.kotlin.get
import spark.kotlin.port
import spark.kotlin.post
import spark.kotlin.staticFiles
import spark.template.freemarker.FreeMarkerEngine
import java.nio.charset.Charset

class Http(val request : Request, val response: Response, val session: Session, val freemarker: FreeMarkerEngine) : HttpApi {
  override fun session(name: String): String? {
    return session.attribute(name)
  }

  override fun formValue(key: String): String? {
    return when (request.requestMethod()) {
      "POST" -> {
        val parsed = URLEncodedUtils.parse(request.body(), Charset.defaultCharset())
        return parsed.filter { pair -> pair.name == key }.map { pair -> pair.value }.lastOrNull()
      }
      "GET" -> {
        request.queryParams(key)
      }
      else -> {
        null
      }
    }
  }

  override fun clearSession(): HttpResponse {
    return { -> session.invalidate() }
  }

  override fun json(model: Any): HttpResponse {
    return { -> response.type("application/json"); toJson(model) }
  }

  override fun session(name: String, value: String): HttpResponse {
    return { -> session.attribute(name, value) }
  }

  override fun render(template: String, model: Any): HttpResponse {
    return { -> freemarker.render(ModelAndView(model, template)) }
  }

  override fun redirect(location: String): HttpResponse {
    return { -> response.redirect(location) }
  }

  override fun error(status: Int): HttpResponse {
    return { -> response.status(status) }
  }

  override fun chain(body: HttpApi.() -> Unit): HttpResponse {
    val chainedApi = ChainedHttpApi(this)
    chainedApi.body()
    return { -> chainedApi.lastResult }
  }
}
/**
 * @author dbarashev@bardsoftware.com
 */
fun main(args: Array<String>) {
  val flags = Flags(ArgParser(args, ArgParser.Mode.POSIX))
  Database.connect(HikariDataSource().apply {
    username = flags.postgresUser
    password = flags.postgresPassword
    jdbcUrl = "jdbc:postgresql://${flags.postgresAddress}:${flags.postgresPort}/${flags.postgresDatabase ?: flags.postgresUser}"
  })
  val assessor = if (flags.taskQueue == "") {
    AssessorApiVoid()
  } else {
    AssessorPubSub("assessment-tasks") {
      ChallengeHandler().handleAssessmentResponse(it)
    }
  }
  port(8080)
  staticFiles.location("/public")

  val adminTaskAllHandler = TaskAllHandler(flags)
  get("/admin/task/all") {
    adminTaskAllHandler.handle(Http(request, response, session(), freemarker))()
  }
  val challengeHandler = ChallengeHandler()
  get("/") {
    freemarker.render(ModelAndView(emptyMap<String,String>(), "index.ftl"))
  }
  get("/login") {
    freemarker.render(ModelAndView(emptyMap<String,String>(), "login.ftl"))
  }
  get("/dashboard") {
    freemarker.render(ModelAndView(emptyMap<String,String>(), "dashboard.ftl"))
  }
  post("/login.do") {
    val handler = LoginHandler()
    val loginResp = handler.handle(
        Http(request, response, session(), freemarker),
        parseDto(request.body()))
    loginResp()
  }

  get("/me") {
    UserDashboardHandler().handle(Http(request, response, session(), freemarker))()
  }

  get("/getAcceptedChallenges") {
    UserDashboardHandler().handleAttempts(Http(request, response, session(), freemarker))()
  }

  get("/try") {
    challengeHandler.handleMaybeTry(Http(request, response, session(), freemarker))()
  }

  get("try.do") {
    challengeHandler.handleDoTry(Http(request, response, session(), freemarker))()
  }

  get("/submit") {
    challengeHandler.handleSubmissionPage(Http(request, response, session(), freemarker), flags.contestId)()
  }

  post("/submit.do") {
    challengeHandler.handleSubmit(Http(request, response, session(), freemarker), assessor)()
  }

  get("/getAttemptStatus") {
    challengeHandler.handleAttemptStatus(Http(request, response, session(), freemarker))()
  }
}
