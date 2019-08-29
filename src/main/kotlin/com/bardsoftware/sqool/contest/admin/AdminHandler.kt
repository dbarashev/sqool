package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.*
import com.bardsoftware.sqool.contest.storage.User
import com.bardsoftware.sqool.contest.storage.UserStorage

typealias CodeExecutor = (code: UserStorage.() -> HttpResponse) -> HttpResponse
private val PROD_CODE_EXECUTOR: CodeExecutor = UserStorage.Companion::exec

abstract class AdminHandler<T : RequestArgs>(private val codeExecutor: CodeExecutor = PROD_CODE_EXECUTOR) : RequestHandler<T>()  {
  protected fun withAdminUser(http: HttpApi, handle: (User) -> HttpResponse): HttpResponse {
    val userName = http.session("name") ?: return redirectToLogin(http)
    return codeExecutor {
      val user = findUser(userName) ?: return@codeExecutor redirectToLogin(http)
      if (!user.isAdmin) {
        return@codeExecutor http.error(403)
      }
      handle(user)
    }
  }
}