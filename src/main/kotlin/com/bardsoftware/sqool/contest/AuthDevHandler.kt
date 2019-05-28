package com.bardsoftware.sqool.contest

data class AuthDevArgs(var user_id: String) : RequestArgs()

class AuthDevHandler : RequestHandler<AuthDevArgs>() {
  override fun handle(http: HttpApi, argValues: AuthDevArgs): HttpResponse {
    http.session("user_id", argValues.user_id)
    return http.ok()
  }

  override fun args(): AuthDevArgs = AuthDevArgs("")
}

