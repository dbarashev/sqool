package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.ImageCheckResult
import com.bardsoftware.sqool.codegen.buildDockerImage
import com.bardsoftware.sqool.codegen.checkImage
import com.bardsoftware.sqool.codegen.task.TaskDeserializationException
import com.bardsoftware.sqool.codegen.task.resultRowToTask
import com.bardsoftware.sqool.contest.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.ByteArrayOutputStream

data class VariantNewArgs(var course: String, var module: String,
                          var variant: String, var schema: String,
                          var tasks: String, var imageName: String
) : RequestArgs()

class VariantNewHandler(private val flags: Flags) : DbHandler<VariantNewArgs>(flags) {
    override fun args(): VariantNewArgs = VariantNewArgs("", "", "", "", "", "contest-image")

    override fun handle(http: HttpApi, argValues: VariantNewArgs): HttpResponse =
            try {
                val taskIdList = ObjectMapper().readValue(argValues.tasks, IntArray::class.java)
                val tasks = transaction {
                    Tasks.select { Tasks.id inList taskIdList.toList() }
                            .map { resultRowToTask(it) }
                }
                buildDockerImage(
                        argValues.imageName,
                        argValues.course, argValues.module,
                        argValues.variant, argValues.schema,
                        tasks)

                val errorStream = ByteArrayOutputStream()
                when (checkImage(argValues.imageName, tasks, flags, errorStream)) {
                    ImageCheckResult.PASSED -> http.ok()
                    ImageCheckResult.ERROR -> http.error(500, errorStream.toString())
                    ImageCheckResult.FAILED -> http.error(400, errorStream.toString())
                }.also { errorStream.close() }
            } catch (exception: TaskDeserializationException) {
                exception.printStackTrace()
                http.error(400, exception.message, exception)
            }
}
