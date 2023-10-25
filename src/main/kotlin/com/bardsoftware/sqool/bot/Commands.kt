package com.bardsoftware.sqool.bot

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

fun ObjectNode.getAction(): Result<Int, String> =
  this["p"]?.asInt()?.let { Ok(it) } ?: Err("Действие не указано")

fun ObjectNode.setAction(action: Int) = put("p", action)

fun ObjectNode.getUniversity(): Result<Int, String> =
  this["u"]?.asInt()?.let { Ok(it) } ?: Err("Ошибка состояния: не найден университет")

fun ObjectNode.getStudent() = this["m"]?.asInt()

fun ObjectNode.setStudent(studentId: Int?) = put("m", studentId)

fun ObjectNode.getSprint() = this["s"]?.asInt()
fun ObjectNode.setSprint(sprintNum: Int) = put("s", sprintNum)
