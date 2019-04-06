package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.buildDockerImage
import com.bardsoftware.sqool.codegen.task.Task
import com.bardsoftware.sqool.codegen.task.TaskJsonDeserializer
import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler
import com.google.gson.*
import com.google.gson.GsonBuilder
import java.lang.IllegalArgumentException

data class VariantNewArgs(var course: String, var module: String,
                          var variant: String, var schema: String,
                          var tasks: String
) : RequestArgs()

class VariantNewHandler : RequestHandler<VariantNewArgs>() {
    override fun args(): VariantNewArgs = VariantNewArgs("", "", "", "", "")

    override fun handle(http: HttpApi, argValues: VariantNewArgs): HttpResponse {
        val builder = GsonBuilder()
        builder.registerTypeAdapter(Task::class.java, TaskJsonDeserializer())
        val gson = builder.create()

        return try {
            val tasks = JsonParser()
                    .parse(argValues.tasks)
                    .asJsonArray
                    .map { gson.fromJson(it, Task::class.java) }
            buildDockerImage(
                    "image_name",
                    argValues.course, argValues.module,
                    argValues.variant, argValues.schema,
                    tasks)
            http.ok()
        } catch (exception: IllegalArgumentException) {
            http.error(422, exception.message, exception)
        } catch (exception: NullPointerException) {
            http.error(422, exception.message, exception)
        } catch (exception: IllegalStateException) {
            http.error(422, exception.message, exception)
        } catch (exception: JsonParseException) {
            http.error(422, exception.message, exception)
        }
    }

}