package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.buildDockerImage
import com.bardsoftware.sqool.codegen.task.TaskDeserializationException
import com.bardsoftware.sqool.codegen.task.deserializeJsonTasks
import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler

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
                        "image_name",
                        argValues.course, argValues.module,
                        argValues.variant, argValues.schema,
                        tasks)
                http.ok()
            } catch (exception: TaskDeserializationException) {
                http.error(400, exception.message, exception)
            }
}
