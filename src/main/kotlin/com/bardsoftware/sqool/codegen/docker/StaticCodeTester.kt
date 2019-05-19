package com.bardsoftware.sqool.codegen.docker

import com.bardsoftware.sqool.contest.Flags
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.ContainerExit
import com.spotify.docker.client.messages.HostConfig
import java.io.File
import java.io.PrintWriter

fun testStaticCode(imageName: String, flags: Flags, errorStream: PrintWriter): ImageCheckResult {
    val composeFile = createComposeFileInTempDir(imageName, flags)

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
            ImageCheckResult.FAILED
        } else {
            ImageCheckResult.PASSED
        }
    } else {
        errorStream.println("Unable to test image:")
        errorStream.println(output)
        ImageCheckResult.ERROR
    }.also {
        composeFile.parentFile.deleteRecursively()
        errorStream.flush()
    }
}

private fun createComposeFileInTempDir(imageName: String, flags: Flags): File {
    //val connectionUri = with(flags) { "postgresql://$postgresUser:$postgresPassword@$postgresAddress:$postgresPort" }
    val connectionUri = with(flags) { "postgresql://$postgresUser@$postgresAddress:$postgresPort" }
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
        |  run-sql:
        |    image: postgres
        |    volumes_from:
        |      - contest-sql:ro
        |    command: bash -c 'find /workspace -type f -name "*-static.sql" -exec cat {} + | psql $connectionUri'
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