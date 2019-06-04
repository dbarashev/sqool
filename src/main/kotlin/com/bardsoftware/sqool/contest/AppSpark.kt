package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.admin.*
import com.google.common.io.ByteStreams
import com.google.common.net.HttpHeaders
import com.google.common.net.MediaType
import com.xenomachina.argparser.ArgParser
import com.zaxxer.hikari.HikariDataSource
import org.apache.http.client.utils.URIBuilder
import org.jetbrains.exposed.sql.Database
import spark.ModelAndView
import spark.Request
import spark.Response
import spark.Session
import spark.kotlin.ignite
import spark.template.freemarker.FreeMarkerEngine
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.MultipartConfigElement
import kotlin.reflect.KClass

typealias SessionProvider = (create: Boolean) -> Session?

/**
 * @author dbarashev@bardsoftware.com
 */
class Http(val request : Request,
           val response: Response,
           val session: SessionProvider,
           private val templateEngine: FreeMarkerEngine) : HttpApi {
  override fun url(): String {
    return URIBuilder(request.url()).apply { scheme = "https" }.build().toASCIIString()
  }

  override fun urlAndQuery(): String {
    val urlAndQuery = "${request.url()}${request.queryString()?.let { if (it != "") "?$it" else it }}"
    return URIBuilder(urlAndQuery).apply {
      scheme = "https"
    }.build().toASCIIString()
  }

  override fun header(name: String): String? {
    return request.headers(name)
  }

  override fun hasSession(): Boolean {
    return session(false) != null
  }

  override fun sessionId(): String? {
    return session(false)?.id()
  }

  override fun session(name: String): String? {
    return session(false)?.attribute(name)
  }

  override fun formValue(key: String): String? {
    return when (request.requestMethod()) {
      "POST" -> {
        if (request.contentType().contains("multipart/form-data")) {
          request.attribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement(""))
          val part = request.raw().getPart(key) ?: return null
          part.inputStream.use {
            ByteStreams.toByteArray(it).toString(Charsets.UTF_8)
          }
        } else {
          return request.raw().getParameter(key)
        }
      }
      "GET" -> {
        request.queryParams(key)
      }
      else -> {
        null
      }
    }
  }

  override fun clearSession() {
    session(false)?.invalidate()
  }

  override fun cookie(name: String): String? = request.cookie(name)

  override fun header(name: String, value: String): HttpResponse {
    return { response.header(name, value) }
  }

  override fun mediaType(mediaType: MediaType): HttpResponse {
    return { response.type(mediaType.toString()) }
  }

  override fun json(model: Any): HttpResponse {
    return { response.type(MediaType.JSON_UTF_8.toString()); toJson<Any>(model, null)}
  }

  override fun <T: Any> json(model: Any, view: KClass<T>): HttpResponse {
    return { response.type(MediaType.JSON_UTF_8.toString()); toJson(model, view) }
  }

  override fun binaryBase64(bytes: ByteArray): HttpResponse {
    return {
      response.type(MediaType.OCTET_STREAM.toString())
      response.header(HttpHeaders.TRANSFER_ENCODING, "base64")
      response.raw().outputStream.use {
        ByteStreams.copy(Base64.getEncoder().encode(bytes).inputStream(), it)
      }
      response.status(200)
    }
  }

  override fun binaryRaw(bytes: ByteArray): HttpResponse {
    return {
      response.header(HttpHeaders.TRANSFER_ENCODING, "binaryRaw")
      response.raw().outputStream.use {
        ByteStreams.copy(bytes.inputStream(), it)
      }
      response.status(200)
    }
  }

  override fun session(name: String, value: String?) {
    if (value != null) {
      session(true)!!.attribute(name, value)
    } else {
      session(false)?.removeAttribute(name)
    }
  }

  override fun render(template: String, model: Any): HttpResponse {
    return { templateEngine.render(ModelAndView(model, template)) }
  }

  override fun redirect(location: String): HttpResponse {
    return { response.redirect(location) }
  }

  override fun error(status: Int, message: String?, cause: Throwable?): HttpResponse {
    if (status >= 500) {
      Logger.getLogger("Http").log(Level.SEVERE, "Sending error $status: $message", cause ?: Exception())
    }
    return {
      if (message != null) {
        response.raw().sendError(status, message)
      } else {
        response.status(status)
      }
    }
  }

  override fun ok(): HttpResponse {
    return { response.status(200) }
  }

  override fun attribute(name: String, value: String) {
    request.attribute(name, value)
  }

  override fun chain(body: HttpApi.() -> Unit): HttpResponse {
    val chainedApi = ChainedHttpApi(this)

    chainedApi.body()
    return { chainedApi.lastResult }
  }
}


/**
 * @author dbarashev@bardsoftware.com
 */
fun main(args: Array<String>) {
  val flags = Flags(ArgParser(args, ArgParser.Mode.POSIX))
  val dataSource = HikariDataSource().apply {
    username = flags.postgresUser
    password = flags.postgresPassword
    jdbcUrl = "jdbc:postgresql://${flags.postgresAddress}:${flags.postgresPort}/${flags.postgresDatabase.ifEmpty { flags.postgresUser }}"
  }
  Database.connect(dataSource)
  val assessor = if (flags.taskQueue == "") {
    AssessorApiVoid()
  } else {
    AssessorPubSub("assessment-tasks") {
      ChallengeHandler().handleAssessmentResponse(it)
    }
  }

  val adminContestAllHandler = ContestAllHandler()
  val adminContestNewHandler = ContestEditHandler(ContestEditMode.INSERT)
  val adminContestUpdateHandler = ContestEditHandler(ContestEditMode.UPDATE)

  val adminScriptAllHandler = ScriptAllHandler()
  val adminTaskAllHandler = TaskAllHandler(flags)
  val adminTaskEditHandler = TaskEditHandler(flags)
  val adminVariantNewHandler = VariantNewHandler(flags)
  val adminSubmissionGetHandler = SubmissionGetHandler()
  val adminSubmissionListHandler = SubmissionListHandler()
  val adminReviewGetHandler = ReviewGetHandler()
  val adminReviewSaveHandler = ReviewSaveHandler()
  val challengeHandler = ChallengeHandler()
  val authDevHandler = AuthDevHandler()

  ignite().apply {
    port(8080)
    staticFiles.location("/public")

    Routes(this, freemarker).apply {
      GET("/admin/contest/all" BY adminContestAllHandler)
      POST("/admin/contest/new" BY adminContestNewHandler ARGS mapOf(
          "code" to ContestEditArgs::code,
          "name" to ContestEditArgs::name,
          "start_ts" to ContestEditArgs::start_ts,
          "end_ts" to ContestEditArgs::end_ts
      ))
      POST("/admin/contest/update" BY adminContestUpdateHandler ARGS mapOf(
          "code" to ContestEditArgs::code,
          "name" to ContestEditArgs::name,
          "start_ts" to ContestEditArgs::start_ts,
          "end_ts" to ContestEditArgs::end_ts
      ))

      GET("/admin/script/all" BY adminScriptAllHandler)

      GET("/admin/task/all" BY adminTaskAllHandler)
      POST("/admin/task/new" BY adminTaskEditHandler ARGS mapOf(
          "result"      to TaskEditArgs::result,
          "name"        to TaskEditArgs::name,
          "description" to TaskEditArgs::description,
          "solution"    to TaskEditArgs::solution
      ))
      POST("/admin/task/update" BY adminTaskEditHandler ARGS mapOf(
          "id"          to TaskEditArgs::id,
          "result"      to TaskEditArgs::result,
          "name"        to TaskEditArgs::name,
          "description" to TaskEditArgs::description,
          "solution"    to TaskEditArgs::solution
      ))

      POST("/admin/variant/new" BY adminVariantNewHandler ARGS mapOf(
          "course"  to VariantNewArgs::course,
          "module"  to VariantNewArgs::module,
          "variant" to VariantNewArgs::variant,
          "schema"  to VariantNewArgs::schema,
          "tasks"   to VariantNewArgs::tasks
      ))
      GET("/"          TEMPLATE "index.ftl")
      GET("/dashboard" TEMPLATE "dashboard.ftl")
      GET("/admin/submission/get" BY adminSubmissionGetHandler ARGS mapOf(
              "task_id" to SubmissionGetArgs::task_id,
              "user_id" to SubmissionGetArgs::user_id,
              "reviewer_id" to SubmissionGetArgs::reviewer_id
      ))
      GET("/admin/submission/list" BY adminSubmissionListHandler ARGS mapOf(
             "task_id" to SubmissionListArgs::task_id
      ))
      GET("/admin/review/get" BY adminReviewGetHandler ARGS mapOf(
              "task_id" to ReviewGetArgs::task_id,
              "user_id" to ReviewGetArgs::user_id
      ))
      POST("/admin/review/save" BY adminReviewSaveHandler ARGS mapOf(
              "task_id" to ReviewSaveArgs::task_id,
              "user_id" to ReviewSaveArgs::user_id,
              "solution_review" to ReviewSaveArgs::solution_review
      ))
      GET("/auth/dev" BY authDevHandler ARGS mapOf(
              "user_id" to AuthDevArgs::user_id
      ))
    }
    get("/login") {
      freemarker.render(ModelAndView(emptyMap<String, String>(), "login.ftl"))
    }
    post("/login.do") {
      val handler = LoginHandler()
      val loginResp = handler.handle(
          Http(request, response, { session() }, freemarker),
          parseDto(request.body()))
      loginResp()
    }

    get("/me") {
      UserDashboardHandler().handle(Http(request, response, { session() }, freemarker))()
    }

    get("/getAcceptedChallenges") {
      UserDashboardHandler().handleAttempts(Http(request, response, { session() }, freemarker))()
    }

    get("/try") {
      challengeHandler.handleMaybeTry(Http(request, response, { session() }, freemarker))()
    }

    get("try.do") {
      challengeHandler.handleDoTry(Http(request, response, { session() }, freemarker))()
    }

    get("/submit") {
      challengeHandler.handleSubmissionPage(Http(request, response, { session() }, freemarker), flags.contestId)()
    }

    post("/submit.do") {
      challengeHandler.handleSubmit(Http(request, response, { session() }, freemarker), assessor)()
    }

    get("/getAttemptStatus") {
      challengeHandler.handleAttemptStatus(Http(request, response, { session() }, freemarker))()
    }
  }
}
