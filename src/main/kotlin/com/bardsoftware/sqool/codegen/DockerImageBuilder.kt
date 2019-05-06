package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.task.Task
import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.filesystem.AbsoluteUnixPath
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient.LogsParam.stderr
import com.spotify.docker.client.DockerClient.LogsParam.stdout
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.ContainerExit
import com.spotify.docker.client.messages.HostConfig
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
    checkImage(imageName)
}

private fun checkImage(imageName: String) {
    val composeFile = createComposeFile(imageName)

    val (result, output) = runDockerCompose(composeFile)
    if (result.statusCode() == 0L) {
        val errors = output.lines()
                .filter { it.matches(".*run-sql.*\\|.*".toRegex()) }
                .map { it.split("| ")[1] }
        if (errors.any { it.matches(".*ERROR.*".toRegex()) }) {
            println("Contest image testing: Invalid sql:")
            print(errors.joinToString("\n"))
        } else {
            println("Contest image testing: OK")
        }
    } else {
        println("Contest image testing: unable to test image:")
        println(output)
    }

    composeFile.deleteRecursively()
}

private fun createComposeFile(imageName: String): File {
    val composeYml = """
        |version: '2.1'
        |
        |services:
        |  contest-sql:
        |    image: $imageName
        |    volumes:
        |      - /workspace
        |    #waiting for run-sql container to finish queries and stop compose command
        |    command: tail -f /dev/null
        |
        |  db:
        |    image: postgres
        |    healthcheck:
        |      test: ["CMD-SHELL", "pg_isready -U postgres"]
        |      interval: 5s
        |      timeout: 5s
        |      retries: 5
        |
        |  run-sql:
        |    image: postgres
        |    depends_on:
        |      db:
        |        condition: service_healthy
        |    volumes_from:
        |      - contest-sql:ro
        |    command: bash -c 'find /workspace -type f -name "*-static.sql" -exec cat {} + | psql -h db -U postgres'
        """.trimMargin()

    val composeDir = createTempDir()
    val composeFile = File(composeDir, "contest-compose.yml")
    composeFile.writeText(composeYml)
    return composeFile
}

private fun runDockerCompose(composeFile: File): Pair<ContainerExit, String> {
    val docker = DefaultDockerClient.fromEnv().build()
    docker.pull("docker/compose:1.23.2")

    val hostConfig = HostConfig.builder()
            .appendBinds(
                    HostConfig.Bind.from(composeFile.parentFile.canonicalPath)
                            .to("/etc/contest-compose/")
                            .build(),
                    HostConfig.Bind.from("/var/run/docker.sock")
                            .to("/var/run/docker.sock")
                            .build()
            )
            .build()
    val composeCommand = listOf(
            "-f", "/etc/contest-compose/${composeFile.name}", "up",
            "--force-recreate", "--abort-on-container-exit",
            "--renew-anon-volumes", "--no-color"
    )
    val containerConfig = ContainerConfig.builder()
            .hostConfig(hostConfig)
            .image("docker/compose:1.23.2")
            //.volumes("/etc/contest-compose")
            .cmd(composeCommand)
            .build()

    val container = docker.createContainer(containerConfig)
    //docker.copyToContainer(composeFile.parentFile.toPath(), container.id(), "/etc/contest-compose/")
    docker.startContainer(container.id())
    val result = docker.waitContainer(container.id())
    val output = docker.logs(container.id(), stdout(), stderr()).readFully()
    docker.removeContainer(container.id())
    docker.close()

    return Pair(result, output)
}

fun main() {
    checkImage("contest-image")
}