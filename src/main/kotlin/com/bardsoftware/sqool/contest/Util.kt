package com.bardsoftware.sqool.contest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.http.client.utils.URLEncodedUtils
import java.nio.charset.Charset

/**
 * @author dbarashev@bardsoftware.com
 */
val mapper = jacksonObjectMapper()

inline fun <reified T: Any> parseDto(requestBody: String): T {
  val map = HashMap<String, String>()
  URLEncodedUtils.parse(requestBody, Charset.defaultCharset()).map { it ->  map[it.name] = it.value }
  return mapper.readValue(mapper.writeValueAsString(map))
}

fun toJson(data: Any): String {
  return mapper.writeValueAsString(data)
}

