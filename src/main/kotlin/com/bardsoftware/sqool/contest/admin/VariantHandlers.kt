package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.ImageCheckResult
import com.bardsoftware.sqool.codegen.buildDockerImage
import com.bardsoftware.sqool.codegen.checkImage
import com.bardsoftware.sqool.codegen.task.TaskDeserializationException
import com.bardsoftware.sqool.codegen.task.deserializeJsonTasks
import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler
import java.io.ByteArrayOutputStream

data class VariantNewArgs(var course: String, var module: String,
                          var variant: String, var schema: String,
                          var tasks: String
) : RequestArgs()

class VariantNewHandler : RequestHandler<VariantNewArgs>() {
    override fun args(): VariantNewArgs = VariantNewArgs("", "", "", "", "")

    override fun handle(http: HttpApi, argValues: VariantNewArgs): HttpResponse =
            try {
                val tasks = deserializeJsonTasks(argValues.tasks)
                buildDockerImage(
                        "contest-image",
                        argValues.course, argValues.module,
                        argValues.variant, argValues.schema,
                        tasks)

                val errorStream = ByteArrayOutputStream()
                when(checkImage("contest-image", errorStream)) {
                    ImageCheckResult.OK -> http.ok()
                    ImageCheckResult.COMPOSE_ERROR -> http.error(500, errorStream.toString())
                    ImageCheckResult.INVALID_SQL -> http.error(409, errorStream.toString())
                }.also { errorStream.close() }

            } catch (exception: TaskDeserializationException) {
                exception.printStackTrace()
                http.error(400, exception.message, exception)
            }
}
