package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.buildDockerImage
import com.bardsoftware.sqool.codegen.task.TaskDeserializationException
import com.bardsoftware.sqool.codegen.task.resultRowToTask
import com.bardsoftware.sqool.contest.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

data class VariantNewArgs(var course: String, var module: String,
                          var variant: String, var schema: String,
                          var tasks: String
) : RequestArgs()

class VariantNewHandler(flags: Flags) : DbHandler<VariantNewArgs>(flags) {
    override fun args(): VariantNewArgs = VariantNewArgs("", "", "", "", "")

    override fun handle(http: HttpApi, argValues: VariantNewArgs): HttpResponse =
            try {
                val tasksId = ObjectMapper().readValue(argValues.tasks, IntArray::class.java)
                val tasks = transaction {
                    tasksId.map {
                        Tasks.select { Tasks.id eq it }
                                .map { resultRowToTask(it) }
                    }.flatten()
                }
                buildDockerImage(
                        "contest-image",
                        argValues.course, argValues.module,
                        argValues.variant, argValues.schema,
                        tasks)
                http.ok()
            } catch (exception: TaskDeserializationException) {
                exception.printStackTrace()
                http.error(400, exception.message, exception)
            }
}