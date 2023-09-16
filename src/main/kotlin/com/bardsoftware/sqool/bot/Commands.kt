package com.bardsoftware.sqool.bot

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

fun ObjectNode.getUniversity(): Result<Int, String> =
  this["u"]?.asInt()?.let { Ok(it) } ?: Err("Ошибка состояния: не найден университет")
