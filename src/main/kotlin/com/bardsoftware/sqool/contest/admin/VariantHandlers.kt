package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.docker.ImageCheckResult
import com.bardsoftware.sqool.codegen.docker.buildDockerImage
import com.bardsoftware.sqool.codegen.docker.checkImage
import com.bardsoftware.sqool.codegen.task.TaskDeserializationException
import com.bardsoftware.sqool.codegen.task.resultRowToTask
import com.bardsoftware.sqool.contest.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.ByteArrayOutputStream

data class VariantNewArgs(var course: String, var module: String,
                          var variant: String, var schema: String,
                          var tasks: String
) : RequestArgs()

class VariantNewHandler(flags: Flags) : DbHandler<VariantNewArgs>(flags) {
    override fun args(): VariantNewArgs = VariantNewArgs("", "", "", "", "")

    override fun handle(http: HttpApi, argValues: VariantNewArgs): HttpResponse =
            try {
                val taskIdList = ObjectMapper().readValue(argValues.tasks, IntArray::class.java)
                val tasks = transaction {
                    Tasks.select { Tasks.id inList taskIdList.toList() }
                            .map { resultRowToTask(it) }
                }
                buildDockerImage(
                        "contest-image",
                        argValues.course, argValues.module,
                        argValues.variant, argValues.schema,
                        tasks)

                val errorStream = ByteArrayOutputStream()
                when(checkImage("contest-image", tasks, errorStream)) {
                    ImageCheckResult.OK -> http.ok()
                    ImageCheckResult.COMPOSE_ERROR -> http.error(500, errorStream.toString())
                    ImageCheckResult.INVALID_SQL -> http.error(409, errorStream.toString())
                }.also { errorStream.close() }
            } catch (exception: TaskDeserializationException) {
                exception.printStackTrace()
                http.error(400, exception.message, exception)
            }
}