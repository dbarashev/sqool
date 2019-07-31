package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.Flags
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler

/**
 * @author dbarashev@bardsoftware.com
 */
abstract class DbHandler<T : RequestArgs>(private val flags: Flags) : RequestHandler<T>() {
}
