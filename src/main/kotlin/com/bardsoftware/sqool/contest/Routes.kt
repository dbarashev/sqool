package com.bardsoftware.sqool.contest

import org.apache.http.client.utils.URIBuilder
import org.slf4j.LoggerFactory.getLogger
import spark.template.freemarker.FreeMarkerEngine
import kotlin.reflect.KMutableProperty1
import spark.kotlin.Http as SparkHttp

typealias Route = String
open class RequestArgs
typealias UrlClosure<T> = T.() -> Unit

const val CSP_SELF = """'self'"""
const val CSP_UNSAFE_INLINE = """'unsafe-inline'"""

abstract class RequestHandler<T: RequestArgs> {
  private val scriptOrigins = mutableListOf(
      CSP_SELF,
      "https://www.google-analytics.com"
  )
  fun getScriptOrigins(): String = scriptOrigins.joinToString(" ")

  private val styleOrigins = mutableListOf(
      CSP_SELF,
      CSP_UNSAFE_INLINE,
      "https://use.fontawesome.com"
  )
  fun getStyleOrigins(): String = styleOrigins.joinToString(" ")

  private val imgOrigins = mutableListOf(
      CSP_SELF,
      "https://www.google-analytics.com"
  )
  fun getImgOrigins(): String = imgOrigins.joinToString(" ")

  private val fontOrigins = mutableListOf(
      "https://use.fontawesome.com"
  )
  fun getFontOrigins(): String = fontOrigins.joinToString(" ")

  var route: RouteHandler<T>? = null
    set(value) {
      if (value == null) {
        throw IllegalArgumentException("NULL route is passed to the route setter")
      }
      verify(value.route)
      field = value
    }

  private fun verify(value: Route?) {
    println("Route ${value} is handled by ${this.javaClass}")
  }

  fun url(closure: UrlClosure<T>?): Route {
    var args : T?
    if (closure != null) {
      args = args()
      args.closure()

      val builder = URIBuilder(this.route!!.route)
      this.route!!.argsMap.forEach { arg, property ->
        val value = property.getter.invoke(args)
        if (value != "") {
          builder.addParameter(arg, value)
        }
      }
      return builder.build().toASCIIString()
    } else {
      return this.route!!.route
    }
  }

  override fun toString(): String {
    return url(null)
  }

  protected fun addScriptOrigin(origin: String) = this.scriptOrigins.add(origin)
  protected fun addImgOrigin(origin: String) = this.imgOrigins.add(origin)
  protected fun addFontOrigin(origin: String) = this.fontOrigins.add(origin)
  protected fun addStyleOrigin(origin: String) = this.styleOrigins.add(origin)

//  protected fun securityHeaders(http: HttpApi, body: HttpApi.() -> Unit): HttpResponse {
//    return http.chain {
//      header(HttpHeaders.CONTENT_SECURITY_POLICY, createMarkupCspHeader(this@RequestHandler))
//      header(HttpHeaders.X_FRAME_OPTIONS, "DENY")
//      if (!isLocalDevelopmentEnvironment()) {
//        header(HttpHeaders.STRICT_TRANSPORT_SECURITY, "max-age=2592000; preload")
//      }
//      body(this)
//    }
//  }
  abstract fun handle(http: HttpApi, argValues: T): HttpResponse
  abstract fun args(): T
}

data class BaseContext(
    val http: HttpApi,
    val templateName: String){}

open class TemplateRequestHandler(val templateName: String) : RequestHandler<RequestArgs>() {
  override fun args(): RequestArgs = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs): HttpResponse {
    val context = BaseContext(
        http = http,
        templateName = this.templateName
    )
    return http.render(templateName, context)
  }
}


typealias ArgsResult<T> = Map<String, KMutableProperty1<T, String>>
data class RouteHandler<T: RequestArgs>(val route: Route, val handler: RequestHandler<T>) {
  var argsMap: ArgsResult<T> = mapOf()
}

val LOGGER = getLogger("Routes")!!

inline fun httpWrapExceptions(http: HttpApi, work: () -> HttpResponse): HttpResponse {
  return try {
    work()
  } catch (e: Exception) {
    LOGGER.error(e.message, e)
    http.error(500, e.message, e)
  } catch (e: HttpException) {
    LOGGER.error(e.message, e)

    http.error(e.code, e.message, e)
  } catch (e: Exception) {
    LOGGER.error(e.message, e)
    http.error(INTERNAL_SERVER_ERROR, e.message, e)
  }
}

open class Routes(private val sparkHttp: SparkHttp,
                  private val templateEngine: FreeMarkerEngine) {
  fun <T: RequestArgs> GET(routeHandler: RouteHandler<T>) {
    sparkHttp.get(routeHandler.route) {
      return@get serve(routeHandler, this)
    }
  }
  fun <T: RequestArgs> POST(routeHandler: RouteHandler<T>) {
    sparkHttp.post(routeHandler.route) {
      return@post serve(routeHandler, this)
    }
  }
  fun <T: RequestArgs> NOT_FOUND(routeHandler: RouteHandler<T>) {
    sparkHttp.notFound {
      return@notFound serve(routeHandler, this)
    }
  }

  fun <T: RequestArgs> serve(routeHandler: RouteHandler<T>, sparkRouteHandler: spark.kotlin.RouteHandler): Any {
    val http = Http(sparkRouteHandler.request, sparkRouteHandler.response,
        { create: Boolean -> sparkRouteHandler.session(create)},
        templateEngine)
    val argValues = routeHandler.handler.args()
    routeHandler.argsMap.forEach { arg, property ->
      val argValue = http.formValue(arg)
      if (argValue != null) {
        property.setter.invoke(argValues, argValue)
      }
    }

    val result = httpWrapExceptions(http) {
      routeHandler.handler.handle(http, argValues)
    }()
    return if (result is Unit) { "" } else { result }
  }
}

infix fun <T : RequestArgs> Route.BY(handler: RequestHandler<T>): RouteHandler<T> {
  val result = RouteHandler(this, handler)
  handler.route = result
  return result
}

infix fun Route.TEMPLATE(template: String): RouteHandler<RequestArgs> {
  val handler = TemplateRequestHandler(template)
  return RouteHandler(this, handler).also { handler.route = it }

}
infix fun <T : RequestArgs> RouteHandler<T>.ARGS(mapping: ArgsResult<T>): RouteHandler<T> {
  this.argsMap = mapping
  return this
}


private var ourHost = "sqool.compscicenter.ru"
var ourWebsocketHost = "sqool.compscicenter.ru"
private var ourScheme = "https"

fun getHost(): String {
  return ourHost
}

fun setHosts(host: String, websocketHost: String) {
  ourHost = host
  ourWebsocketHost = websocketHost
}

fun Route.absolute(): Route {
  return "$ourScheme://$ourHost${this}"
}
