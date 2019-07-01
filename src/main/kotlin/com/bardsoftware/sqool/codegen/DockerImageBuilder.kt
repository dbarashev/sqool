package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.docker.testDynamicCode
import com.bardsoftware.sqool.codegen.docker.testStaticCode
import com.bardsoftware.sqool.codegen.task.Task
import com.bardsoftware.sqool.contest.Flags
import com.spotify.docker.client.DefaultDockerClient
import java.io.File
import java.io.OutputStream
import java.io.PrintWriter

fun buildDockerImage(
        imageName: String,
        course: String, module: String,
        variant: String, schemaPath: String,
        tasks: List<Task>
) {
    val root = createTempDir()
    val moduleFolder = File(root, "workspace/$course/$module")
    moduleFolder.mkdirs()

    val codeGenerator = CodeGenerator(module, schemaPath)
    val staticCode = codeGenerator.generateStaticCodeHeader() + "\n\n" +
            tasks.joinToString("\n\n") { it.generateStaticCode() }
    File(moduleFolder, "$variant-static.sql").writeText(staticCode)
    tasks.forEach {
        val perSubmissionCode = it.generateDynamicCode(codeGenerator)
        File(moduleFolder, "${it.name}-dynamic.sql").writeText(perSubmissionCode)
    }

    createDockerfile(root, ".", "/")
    val docker = DefaultDockerClient.fromEnv().build()
    docker.build(root.toPath(), imageName)

    root.deleteRecursively()
}

private fun createDockerfile(dockerfileDir: File, localPath: String, imagePath: String) {
    val dockerfileContent = """
        FROM scratch
        ADD $localPath $imagePath
        """.trimIndent()
    File(dockerfileDir, "dockerfile").writeText(dockerfileContent)
}

enum class ImageCheckResult {
    PASSED, FAILED, ERROR
}

fun checkImage(imageName: String, imageTasks: List<Task>, flags: Flags, errorStream: OutputStream): ImageCheckResult {
    val writer = PrintWriter(errorStream)
    writer.println("Static code testing:")
    val staticCodeResult = testStaticCode(imageName, flags, writer)
    if (staticCodeResult != ImageCheckResult.PASSED) {
        return staticCodeResult
    }
    writer.println("Dynamic code testing:")
    return testDynamicCode(imageName, imageTasks, flags, writer)
}
