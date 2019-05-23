package com.bardsoftware.sqool.contest

import org.jetbrains.exposed.sql.transactions.transaction

data class AuthDevArgs(var user_id: String) :RequestArgs();

class AuthDevHandler: RequestHandler<AuthDevArgs>()  {
  override fun handle(http: HttpApi, argValues: AuthDevArgs): HttpResponse {
    return transaction {
      http.session("user_id", argValues.user_id );
      http.ok();
    }
  }

  override fun args(): AuthDevArgs = AuthDevArgs("");

}

