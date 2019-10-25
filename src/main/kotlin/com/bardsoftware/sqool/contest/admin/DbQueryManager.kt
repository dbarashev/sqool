package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.Contest
import com.bardsoftware.sqool.codegen.Schema
import com.bardsoftware.sqool.codegen.Variant
import com.bardsoftware.sqool.codegen.task.MultiColumnTask
import com.bardsoftware.sqool.codegen.task.ScalarValueTask
import com.bardsoftware.sqool.codegen.task.SingleColumnTask
import com.bardsoftware.sqool.codegen.task.Task
import com.bardsoftware.sqool.codegen.task.spec.MatcherSpec
import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class NoSuchContestException : Exception()

class MalformedDataException : Exception {
  constructor(message: String?, cause: Throwable) : super(message, cause)

  constructor(message: String?) : super(message)
}

object ContestTasks : Table("Contest.TaskContest") {
  val contestCode = text("contest_code")
  val variantId = integer("variant_id")
  val taskId = integer("task_id")
}

class DbQueryManager {
  fun listContestVariantsId(contestCode: String): List<Int> = transaction {
    Contests.select { Contests.code eq contestCode }
        .map { ObjectMapper().readValue(it[Contests.variants_id_json_array], IntArray::class.java).toList() }
        .firstOrNull()
        ?.toList() ?: throw Exception("Queried contests doesn't exist")
  }

  fun listContestTasksId(contestCode: String): List<Int> = transaction {
    ContestTasks.select { ContestTasks.contestCode eq contestCode }
        .map { it[ContestTasks.taskId] }
        .distinct()
        .toList()
  }

  fun findContest(code: String): Contest = transaction {
    val contest = Contests.select {
      Contests.code eq code
    }.map { resultRowToContest(it) }
    if (contest.isEmpty()) throw NoSuchContestException()
    contest.first()
  }

  private fun findVariants(idList: List<Int>): List<Variant> = transaction {
    Variants.select {
      Variants.id inList idList.toList()
    }.map(::resultRowToVariant)
  }

  private fun findTasks(idList: List<Int>): List<Task> = transaction {
    Tasks.select {
      Tasks.id inList idList.toList()
    }.map(::resultRowToTask)
  }

  private fun resultRowToContest(contest: ResultRow): Contest {
    val variantsIdList = jsonMapper.readValue(contest[Contests.variants_id_json_array], IntArray::class.java)
    val variants = findVariants(variantsIdList.toList())
    return Contest(contest[Contests.code], contest[Contests.name], variants)
  }

  private fun resultRowToVariant(variant: ResultRow): Variant {
    val schemas = jsonMapper.readValue(variant[Variants.scripts_id_json_array], IntArray::class.java).map(::Schema)
    val tasksIdList = jsonMapper.readValue(variant[Variants.tasks_id_json_array], IntArray::class.java)
    val tasks = findTasks(tasksIdList.toList())
    return Variant(variant[Variants.name], tasks, schemas)
  }

  fun resultRowToTask(row: ResultRow): Task = try {
    buildTask(row[Tasks.name], row[Tasks.result_json], row[Tasks.solution])
  } catch (exception: Exception) {
    when (exception) {
      is UnrecognizedPropertyException, is InvalidDefinitionException, is JsonMappingException,
      is TypeCastException, is IllegalArgumentException -> throw MalformedDataException(exception.message, exception)
      else -> throw exception
    }
  }
}

private val TYPE_REFERENCE = object : TypeReference<List<AttributeDto>>() {}
fun buildTask(name: String, resultJson: String, solution: String): Task {
  val keyAttributes = jsonMapper.readValue(resultJson, TYPE_REFERENCE)
  return buildTask(name, solution, keyAttributes)
}

private fun buildTask(
    name: String, solution: String, keyAttributes: List<AttributeDto>, nonKeyAttributes: List<AttributeDto> = emptyList()): Task {
  if (name.isEmpty() || solution.isEmpty()) {
    throw MalformedDataException("Invalid task")
  }

  if (keyAttributes.size == 1 && keyAttributes[0].name.isEmpty()) {
    return ScalarValueTask(name, solution, SqlDataType.getEnum(keyAttributes[0].type))
  }

  if (keyAttributes.size == 1 && nonKeyAttributes.isEmpty()) {
    val type = SqlDataType.getEnum(keyAttributes[0].type)
    val column = TaskResultColumn(keyAttributes[0].name, type)
    return SingleColumnTask(name, solution, column)
  }

  val keyAttributes = keyAttributes.map { it.toTaskResultColumn() }
  val nonKeyAttributes = nonKeyAttributes.map { it.toTaskResultColumn() }
  val relationSpec = RelationSpec(keyAttributes.sortedBy { it.num }, nonKeyAttributes)
  val matcherSpec = MatcherSpec(relationSpec)
  return MultiColumnTask(name, solution, matcherSpec)
}


private class AttributeDto {
  val name: String = ""
  val type: String = ""
  val num: String = "0"

  fun toTaskResultColumn(): TaskResultColumn = TaskResultColumn(name, SqlDataType.getEnum(type), num.toInt())
}

private val jsonMapper = ObjectMapper()
