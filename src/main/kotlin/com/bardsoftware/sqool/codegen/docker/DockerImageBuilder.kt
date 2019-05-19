package com.bardsoftware.sqool.codegen.docker

import com.bardsoftware.sqool.codegen.CodeGenerator
import com.bardsoftware.sqool.codegen.task.Task
import com.bardsoftware.sqool.contest.Flags
import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.filesystem.AbsoluteUnixPath
import java.io.File
import java.io.OutputStream
import java.io.PrintWriter
import java.nio.file.Paths

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

    //TODO: check it out with scratch image in next release
    Jib.from("busybox")
            .addLayer(listOf(Paths.get(root.toString(), "workspace")), AbsoluteUnixPath.get("/"))
            .containerize(
                    Containerizer.to(DockerDaemonImage.named(imageName))
            )

    root.deleteRecursively()
}

enum class ImageCheckResult {
    PASSED, FAILED, ERROR
}

fun checkImage(imageName: String, imageTasks: List<Task>, flags: Flags, errorStream: OutputStream): ImageCheckResult {
    val writer = PrintWriter(errorStream)
    writer.println("Static code testing:")
    val staticCodeResult = testStaticCode(imageName, flags, writer)
    writer.println()
    writer.println("Dynamic code testing:")
    val dynamicCodeResult = testDynamicCode(imageName, imageTasks, flags, writer)

    return when {
        staticCodeResult == ImageCheckResult.PASSED && dynamicCodeResult == ImageCheckResult.PASSED -> ImageCheckResult.PASSED
        staticCodeResult == ImageCheckResult.FAILED || dynamicCodeResult == ImageCheckResult.FAILED -> ImageCheckResult.FAILED
        else -> ImageCheckResult.ERROR
    }
}