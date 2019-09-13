package com.bardsoftware.sqool.codegen.docker

import com.bardsoftware.sqool.contest.Flags
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.ContainerExit
import com.spotify.docker.client.messages.HostConfig
import java.io.PrintWriter

class StaticCodeTester(
    private val image: String,
    private val contest: String,
    private val flags: Flags
) {
  fun test(errorStream: PrintWriter): ImageCheckResult {
    println("Testing $image $contest ")
    val (result, output) = runPsql()
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

  private fun runPsql(): Pair<ContainerExit, String> {
    val docker = DefaultDockerClient.fromEnv().build()
    val sqlContainerConfig = ContainerConfig.builder()
        .image(image)
        .volumes("/workspace")
        .build()
    val sqlContainer = docker.createContainer(sqlContainerConfig)

    var hostConfigBuilder = HostConfig.builder().volumesFrom(sqlContainer.id())
    hostConfigBuilder = if (flags.postgresQaContainer.isNullOrEmpty()) {
      hostConfigBuilder.networkMode("host")
    } else {
      hostConfigBuilder.links("${flags.postgresQaContainer}:postgres")
    }
    val hostConfig = hostConfigBuilder.build()
    val postgresUri = with(flags) {
      if (postgresQaContainer.isNullOrEmpty()) {
        "postgres://$postgresUser:$postgresPassword@$postgresAddress:$postgresPort"
      } else {
        "postgres://$postgresUser:$postgresPassword@postgres:$postgresPort"
      }
    }
    val command = listOf("bash", "-c", "psql $postgresUri -f '/workspace/$contest/init.sql'")
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

    return Pair(result, output)
  }
}
