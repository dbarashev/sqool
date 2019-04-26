package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.task.Task
import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.filesystem.AbsoluteUnixPath
import java.io.File
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

    checkImage()
}

private fun checkImage() {
    val composeFile = Task::class.java.classLoader.getResource("docker/contest-compose.yml").path
    val composeCommand = """
        |docker run --rm -v /var/run/docker.sock:/var/run/docker.sock
        |    -v $composeFile:/etc/contest-compose.yml
        |    docker/compose:1.23.2 -f /etc/contest-compose.yml up
        |    --force-recreate
        |    --abort-on-container-exit
        """.trimMargin().replace('\n', ' ')

    val composeProcess = Runtime.getRuntime().exec(composeCommand)
    val exitVal = composeProcess.waitFor()

    if (exitVal == 0) {
        val output = composeProcess.inputStream.bufferedReader()
                .use { it.readText() }
                .lines()
                .filter { it.matches(".*run-sql.*\\|.*".toRegex()) }
        if (output.any { it.matches(".*ERROR.*".toRegex()) }) {
            println("Contest image testing: Invalid sql:")
            print(output.joinToString("\n"))
        } else {
            println("Contest image testing: OK")
        }
    } else {
        val errors = composeProcess.errorStream.bufferedReader()
                .use { it.readText() }
        if (errors.isNotEmpty()) {
            println("Contest image testing: unable to test image:")
            print(errors)
        }
    }
}