package com.bardsoftware.sqool.codegen.docker

import com.bardsoftware.sqool.codegen.ImageCheckResult
import com.bardsoftware.sqool.contest.Flags
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.ContainerExit
import com.spotify.docker.client.messages.HostConfig
import java.io.PrintWriter

fun testStaticCode(imageName: String, flags: Flags, errorStream: PrintWriter): ImageCheckResult {
    val (result, output) = runPsql(imageName, flags)
    return if (result.statusCode() == 0L) {
        val errors = output.lines()
        if (errors.any { it.matches(".*ERROR.*".toRegex()) }) {
            errorStream.println("Invalid sql:")
            errorStream.println(errors.joinToString("\n"))
            ImageCheckResult.FAILED
        } else {
            errorStream.println("OK")
            ImageCheckResult.PASSED
        }
    } else {
        errorStream.println("Unable to test image:")
        errorStream.println(output)
        ImageCheckResult.ERROR
    }.also {
        errorStream.flush()
    }
}

private fun runPsql(imageName: String, flags: Flags): Pair<ContainerExit, String> {
    val docker = DefaultDockerClient.fromEnv().build()
    val sqlContainerConfig = ContainerConfig.builder()
            .image(imageName)
            .volumes("/workspace")
            .build()
    val sqlContainer = docker.createContainer(sqlContainerConfig)

    val hostConfig = HostConfig.builder()
            .volumesFrom(sqlContainer.id())
            .build()
    val postgresUri = with(flags) { "postgres://$postgresUser:$postgresPassword@$postgresAddress:$postgresPort" }
    val command = listOf("bash", "-c", "find /workspace -type f -name \"*-static.sql\" -exec cat {} + | psql $postgresUri")
    docker.pull("postgres:10")
    val containerConfig = ContainerConfig.builder()
            .image("postgres:10")
            .hostConfig(hostConfig)
            .cmd(command)
            .build()
    val container = docker.createContainer(containerConfig)

    docker.startContainer(container.id())
    val result = docker.waitContainer(container.id())
    val output = docker.logs(container.id(), DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr()).readFully()
    docker.removeContainer(sqlContainer.id())
    docker.removeContainer(container.id())
    docker.close()

    throw Exception(output)
    return Pair(result, output)
}
