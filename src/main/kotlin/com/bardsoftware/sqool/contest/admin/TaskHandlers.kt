package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskAllHandler {
  fun handle(http: HttpApi): HttpResponse {
    return http.redirect("/")
  }
}
