/*
 * Copyright (c) BarD Software s.r.o 2019
 *
 * This file is a part of SQooL, a service for running SQL contests.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    val pgAddress = if (flags.postgresQaContainer.isNullOrEmpty()) {
      flags.postgresAddress
    } else {
      "postgres"
    }
    val postgresUri = with(flags) {
      "postgres://$postgresUser:$postgresPassword@$pgAddress:$postgresPort"
    }
    val command = listOf("bash", "-c", "psql $postgresUri -f '/workspace/$contest/init.sql'")
    // docker.pull("postgres:10")
    val containerConfig = ContainerConfig.builder()
        .image("postgres")
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
