package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.Contest
import com.bardsoftware.sqool.codegen.Schema
import com.bardsoftware.sqool.codegen.Variant
import com.bardsoftware.sqool.codegen.task.*
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
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.IllegalArgumentException


class NoSuchContestException : Exception()

class MalformedDataException : Exception {
  constructor(message: String?, cause: Throwable) : super(message, cause)

  constructor(message: String?) : super(message)
}

class DbQueryManager {
  private val jsonMapper = ObjectMapper()

  fun findContest(code: String): Contest = transaction {
    val contest = Contests.select {
      Contests.code eq code
    }.map { resultRowToContest(it) }
    if (contest.isEmpty()) throw NoSuchContestException()
    contest.first()
  }

  fun findVariants(idList: List<Int>) = transaction {
    Variants.select {
      Variants.id inList idList.toList()
    }.map(::resultRowToVariant)
  }

  fun findTasks(idList: List<Int>) = transaction {
    Tasks.select {
      Tasks.id inList idList.toList()
    }.map(::resultRowToTask)
  }

  fun resultRowToContest(contest: ResultRow): Contest {
    val variantsIdList = jsonMapper.readValue(contest[Contests.variants_id_json_array], IntArray::class.java)
    val variants = findVariants(variantsIdList.toList())
    return Contest(contest[Contests.code], contest[Contests.name], variants)
  }

  fun resultRowToVariant(variant: ResultRow): Variant {
    val schemas = jsonMapper.readValue(variant[Variants.scripts_id_json_array], IntArray::class.java).map(::Schema)
    val tasksIdList = jsonMapper.readValue(variant[Variants.tasks_id_json_array], IntArray::class.java)
    val tasks = findTasks(tasksIdList.toList())
    return Variant(variant[Variants.name], tasks, schemas)
  }

  fun resultRowToTask(row: ResultRow) = try {
    val attributesJson = row[Tasks.result_json]
    val keyAttributes = jsonMapper.readValue(attributesJson, object : TypeReference<List<AttributeDto>>() {})
    TaskDto(row[Tasks.name], row[Tasks.solution], keyAttributes).toTask()
  } catch (exception: Exception) {
    when (exception) {
      is UnrecognizedPropertyException, is InvalidDefinitionException, is JsonMappingException,
      is TypeCastException, is IllegalArgumentException -> throw MalformedDataException(exception.message, exception)
      else -> throw exception
    }
  }
}

private class TaskDto(
    private val name: String,
    private val solution: String,
    private val keyAttributes: List<AttributeDto>,
    private val nonKeyAttributes: List<AttributeDto> = emptyList()
) {
  fun toTask(): Task {
    if (!isValid()) {
      throw MalformedDataException("Invalid task json")
    }

    if (keyAttributes.size == 1 && keyAttributes[0].name.isEmpty()) {
      return ScalarValueTask(name, solution, SqlDataType.getEnum(keyAttributes[0].type))
    }

    if (keyAttributes.size == 1 && nonKeyAttributes.isEmpty()) {
      val type = SqlDataType.getEnum(keyAttributes[0].type)
      val column = TaskResultColumn(keyAttributes[0].name, type)
      return SingleColumnTask(name, solution, column)
    }

    return buildMultiColumnTask()
  }

  private fun isValid() = name.isNotEmpty() && solution.isNotEmpty()

  private fun buildMultiColumnTask(): Task {
    val keyAttributes = keyAttributes.map { it.toTaskResultColumn() }
    val nonKeyAttributes = nonKeyAttributes.map { it.toTaskResultColumn() }
    val relationSpec = RelationSpec(keyAttributes, nonKeyAttributes)
    val matcherSpec = MatcherSpec(relationSpec)
    return MultiColumnTask(name, solution, matcherSpec)
  }
}

private class AttributeDto {
  val name: String = ""
  val type: String = ""

  fun toTaskResultColumn(): TaskResultColumn = TaskResultColumn(name, SqlDataType.getEnum(type))
}