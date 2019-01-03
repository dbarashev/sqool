package com.bardsoftware.sqool.contest

/**
 * @author dbarashev@bardsoftware.com
 */
typealias HttpResponse  = () -> Any

interface HttpApi {
  fun formValue(key: String): String?
  fun session(name: String): String?

  fun render(template: String, model: Any): HttpResponse
  fun json(model: Any): HttpResponse
  fun redirect(location: String) : HttpResponse
  fun error(status: Int) : HttpResponse

  fun session(name: String, value: String): HttpResponse
  fun clearSession(): HttpResponse
  fun chain(body: HttpApi.() -> Unit) : HttpResponse
}

class ChainedHttpApi(val delegate: HttpApi) : HttpApi {
  override fun session(name: String): String? {
    return delegate.session(name)
  }

  override fun formValue(key: String): String? {
    return delegate.formValue(key)
  }

  val chained = mutableListOf<HttpResponse>()

  override fun clearSession(): HttpResponse {
    return delegate.clearSession()
  }

  override fun json(model: Any): HttpResponse {
    return delegate.json(model)
  }

  override fun render(template: String, model: Any): HttpResponse {
    return delegate.render(template, model).also { chained.add(it) }
  }

  override fun redirect(location: String): HttpResponse {
    return delegate.redirect(location).also { chained.add(it) }
  }

  override fun error(status: Int): HttpResponse {
    return delegate.error(status).also { chained.add(it) }
  }

  override fun session(name: String, value: String): HttpResponse {
    return delegate.session(name,  value).also { chained.add(it) }
  }

  override fun chain(body: HttpApi.() -> Unit): HttpResponse {
    throw NotImplementedError("Do not chain inside chain")
  }

  val lastResult: Any
    get() = chained.map { it -> it() }.last()
}
