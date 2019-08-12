package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.storage.UserStorage
import org.apache.commons.codec.digest.DigestUtils

data class LoginReq(val name: String, val password: String, val createIfMissing: Boolean)

/**
 * @author dbarashev@bardsoftware.com
 */
class LoginHandler {
  fun handle(http: HttpApi, req: LoginReq): HttpResponse {
    return UserStorage.exec {
      (findUser(req.name) ?: if (req.createIfMissing) {
        val newUser = createUser(req.name, req.password)
        newUser?.acceptRandomChallenges()
        newUser
      } else null)?.let {
        if (DigestUtils.md5Hex(req.password) == it.password) {
          http.chain {
            session("name", it.name)
            redirect("/me")
          }
        } else {
          http.redirect("/error403")
        }
      } ?: http.render("signup.ftl", mapOf("name" to req.name, "password" to req.password))
    }
  }
}
