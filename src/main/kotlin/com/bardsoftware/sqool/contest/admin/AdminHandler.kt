package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.*
import com.bardsoftware.sqool.contest.storage.User
import com.bardsoftware.sqool.contest.storage.UserStorage

abstract class AdminHandler<T : RequestArgs> : RequestHandler<T>()  {
  protected fun withAdminUser(http: HttpApi, handle: (User) -> HttpResponse): HttpResponse {
    val userName = http.session("name") ?: return redirectToLogin(http)
    return UserStorage.exec {
      val user = findUser(userName) ?: return@exec redirectToLogin(http)
      if (!user.isAdmin) {
        return@exec http.error(403)
      }
      handle(user)
    }
  }
}