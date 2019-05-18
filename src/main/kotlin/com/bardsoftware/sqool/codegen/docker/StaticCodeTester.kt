package com.bardsoftware.sqool.codegen.docker

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.ContainerExit
import com.spotify.docker.client.messages.HostConfig
import java.io.File
import java.io.PrintWriter

fun testStaticCode(imageName: String, errorStream: PrintWriter): ImageCheckResult {
    val composeFile = createComposeFileInTempDir(imageName)

    val (result, output) = runDockerCompose(composeFile)
    return if (result.statusCode() == 0L) {
        // Extracting output produces by run-sql container to check if there are any error messages.
        val errors = output.lines()
                // docker-compose prepends container name with pipe to original lines.
                .filter { it.matches(".*run-sql.*\\|.*".toRegex()) }
                // We need only original parts.
                .map { it.split("| ")[1] }
        if (errors.any { it.matches(".*ERROR.*".toRegex()) }) {
            errorStream.println("Invalid sql:")
            errorStream.println(errors.joinToString("\n"))
            ImageCheckResult.INVALID_SQL
        } else {
            ImageCheckResult.OK
        }
    } else {
        errorStream.println("Unable to test image:")
        errorStream.println(output)
        ImageCheckResult.COMPOSE_ERROR
    }.also {
        composeFile.parentFile.deleteRecursively()
        errorStream.flush()
    }
}

private fun createComposeFileInTempDir(imageName: String): File {
    val composeYml = """
        |version: '2.1'
        |
        |services:
        |  contest-sql:
        |    image: $imageName
        |    volumes:
        |      - /workspace
        |    # We need to stop docker-compose (and in particular PostgreSQL server)
        |    # when test queries are completed. The easiest way is to set --abort-on-container-exit flag.
        |    # However, we don't want to abort too early, so here we just wait forever.
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
                    HostConfig.Bind.from("/var/run/docker.sock")
                            .to("/var/run/docker.sock")
                            .build()
            )
            .build()
    val composeCommand = listOf(
            "-f", "/var/run/sqool/${composeFile.name}", "up",
            "--force-recreate", "--abort-on-container-exit",
            "--renew-anon-volumes", "--no-color"
    )
    val containerConfig = ContainerConfig.builder()
            .hostConfig(hostConfig)
            .volumes("/var/run/sqool")
            .image("docker/compose:1.23.2")
            .cmd(composeCommand)
            .build()

    val container = docker.createContainer(containerConfig)
    docker.copyToContainer(composeFile.parentFile.toPath(), container.id(), "/var/run/sqool/")
    docker.startContainer(container.id())
    val result = docker.waitContainer(container.id())
    val output = docker.logs(container.id(), DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr()).readFully()
    docker.removeContainer(container.id())
    docker.close()

    return Pair(result, output)
}