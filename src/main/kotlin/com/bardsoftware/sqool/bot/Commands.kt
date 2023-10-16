package com.bardsoftware.sqool.bot

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

fun ObjectNode.getAction(): Result<Int, String> =
  this["p"]?.asInt()?.let { Ok(it) } ?: Err("Действие не указано")

fun ObjectNode.getUniversity(): Result<Int, String> =
  this["u"]?.asInt()?.let { Ok(it) } ?: Err("Ошибка состояния: не найден университет")

fun ObjectNode.getStudent(): Result<Int, String> =
  this["m"]?.asInt()?.let { Ok(it) } ?: Err("ID студента не найден")
