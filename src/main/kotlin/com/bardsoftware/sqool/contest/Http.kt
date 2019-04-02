/*
 * Copyright (c) 2018 BarD Software s.r.o
 *
 * Author: Dmitry Barashev (dbarashev@bardsoftware.com)
 *
 * This package provides interfaces for the request handlers
 * which allow for handlers being HTTP framework agnostic.
 */

package com.bardsoftware.sqool.contest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.net.MediaType
import org.apache.http.client.utils.URLEncodedUtils
import java.nio.charset.Charset
import kotlin.reflect.KClass


typealias HttpResponse  = () -> Any

val BAD_REQUEST = 400
val ERROR_UNAUTHORIZED = 401
val ERROR_FORBIDDEN = 403
val NOT_FOUND = 404
val CONFLICT = 409
val INTERNAL_SERVER_ERROR = 500
val LOCKED = 423
val PRECONDITION_FAILED = 412

interface HttpApi {
  fun url(): String
  fun urlAndQuery(): String
  fun formValue(key: String): String?
  fun header(name: String): String?

  fun hasSession(): Boolean
  fun sessionId(): String?
  fun session(name: String): String?
  fun session(name: String, value: String?)
  fun clearSession()

  fun cookie(name: String): String?

  fun mediaType(mediaType: MediaType): HttpResponse
  fun header(name: String, value: String): HttpResponse
  fun render(template: String, model: Any): HttpResponse
  fun json(model: Any): HttpResponse
  fun <T: Any> json(model: Any, view: KClass<T>): HttpResponse
  fun binaryBase64(bytes: ByteArray): HttpResponse
  fun binaryRaw(bytes: ByteArray): HttpResponse

  fun redirect(location: String): HttpResponse
  fun error(status: Int, message: String? = null, cause: Throwable? = null): HttpResponse
  fun ok(): HttpResponse

  fun attribute(name: String, value: String)
  fun chain(body: HttpApi.() -> Unit) : HttpResponse
}

class ChainedHttpApi(val delegate: HttpApi) : HttpApi {
  override fun url(): String {
    return delegate.url()
  }

  override fun urlAndQuery(): String {
    return delegate.urlAndQuery()
  }

  override fun header(name: String): String? {
    return delegate.header(name)
  }

  override fun hasSession(): Boolean {
    return delegate.hasSession()
  }

  override fun sessionId(): String? {
    return delegate.sessionId()
  }

  override fun session(name: String): String? {
    return delegate.session(name)
  }

  override fun formValue(key: String): String? {
    return delegate.formValue(key)
  }

  val chained = mutableListOf<HttpResponse>()

  override fun clearSession() {
    delegate.clearSession()
  }

  override fun cookie(name: String): String? = delegate.cookie(name)

  override fun mediaType(mediaType: MediaType): HttpResponse {
    return delegate.mediaType(mediaType).also { chained.add(it) }
  }

  override fun header(name: String, value: String): HttpResponse {
    return delegate.header(name, value).also { chained.add(it) }
  }

  override fun json(model: Any): HttpResponse {
    return delegate.json(model).also { chained.add(it) }
  }

  override fun <T: Any> json(model: Any, view: KClass<T>): HttpResponse {
    return delegate.json(model, view).also { chained.add(it) }
  }

  override fun binaryBase64(bytes: ByteArray): HttpResponse {
    return delegate.binaryBase64(bytes).also { chained.add(it) }
  }

  override fun binaryRaw(bytes: ByteArray): HttpResponse {
    return delegate.binaryRaw(bytes).also { chained.add(it) }
  }

  override fun render(template: String, model: Any): HttpResponse {
    return delegate.render(template, model).also { chained.add(it) }
  }

  override fun redirect(location: String): HttpResponse {
    return delegate.redirect(location).also { chained.add(it) }
  }

  override fun error(status: Int, message: String?, cause: Throwable?): HttpResponse {
    return delegate.error(status, message, cause).also { chained.add(it) }
  }

  override fun ok(): HttpResponse {
    return delegate.ok().also { chained.add(it) }
  }

  override fun session(name: String, value: String?) {
    return delegate.session(name,  value)
  }

  override fun attribute(name: String, value: String) {
    delegate.attribute(name, value)
  }

  override fun chain(body: HttpApi.() -> Unit): HttpResponse {
    throw NotImplementedError("Do not chain inside chain")
  }

  val lastResult: Any
    get() = chained.map { it -> it() }.last()
}


val mapper = jacksonObjectMapper()

inline fun <reified T: Any> parseDto(requestBody: String): T {
  val map = HashMap<String, String>()
  URLEncodedUtils.parse(requestBody, Charset.defaultCharset()).map { it ->  map[it.name] = it.value }
  return mapper.readValue(mapper.writeValueAsString(map))
}

fun <T: Any> toJson(data: Any, view: KClass<T>?): String {
  return if (view == null) {
    mapper.writeValueAsString(data)
  } else {
    mapper.writerWithView(view.java).writeValueAsString(data)
  }
}

class HttpException: Exception {
  val code: Int

  constructor(code: Int): super("HTTP error $code") {
    this.code = code
  }

  constructor(code: Int, message: String): super(message) {
    this.code = code
  }

  constructor(message: String?) : super(message) {
    this.code = INTERNAL_SERVER_ERROR
  }
  constructor(message: String?, cause: Throwable?) : super(message, cause) {
    this.code = INTERNAL_SERVER_ERROR
  }
  constructor(cause: Throwable?) : super(cause) {
    this.code = INTERNAL_SERVER_ERROR
  }
}
