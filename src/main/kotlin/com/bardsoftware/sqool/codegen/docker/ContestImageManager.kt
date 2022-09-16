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

import com.bardsoftware.sqool.codegen.Contest
import com.bardsoftware.sqool.contest.Flags
import com.spotify.docker.client.DefaultDockerClient
import java.io.File
import java.io.OutputStream
import java.io.PrintWriter

const val CONTEST_DIRECTORY = "/workspace"

enum class ImageCheckResult {
  PASSED, FAILED, ERROR
}

class ContestImageManager(private val contest: Contest, flags: Flags) {
  private val dynamicCodeTester = DynamicCodeTester(contest.code, contest.code, contest.variants, flags)
  private val staticCodeTester = StaticCodeTester(contest.code, contest.code, flags)

  fun createImage() {
    val root = createTempDir()
    val contestDir = File(root, "$CONTEST_DIRECTORY/${contest.code}")
    contestDir.mkdirs()

    val schemas = contest.variants.map { it.schemas }.flatten().toSet()
    val schemaDir = File(contestDir, "schema")
    schemaDir.mkdir()
    schemas.forEach { File(schemaDir, "${it.description}.sql").writeText(it.body) }

    for (variant in contest.variants) {
      val variantDir = File(contestDir, variant.name)
      variantDir.mkdir()
      File(variantDir, "static.sql").writeText(
          variant.generateStaticCode("$CONTEST_DIRECTORY/${contest.code}/schema")
      )
      variant.tasks.forEach {
        val dynamicCode = it.generateDynamicCode(variant.name)
        File(variantDir, "${it.name}-dynamic.sql").writeText(dynamicCode)
      }
    }

    val initCode = contest.variants.joinToString("\n") { "\\i '$CONTEST_DIRECTORY/${contest.code}/${it.name}/static.sql'" }
    File(contestDir, "init.sql").writeText(initCode)

    createDockerfile(root, ".", "/")
    val docker = DefaultDockerClient.fromEnv().build()
    docker.build(root.toPath(), contest.code)

    docker.close()
    root.deleteRecursively()
  }

  private fun createDockerfile(dockerfileDir: File, localPath: String, imagePath: String) {
    val dockerfileContent = """
        FROM busybox:latest
        COPY $localPath $imagePath
        """.trimIndent()
    File(dockerfileDir, "dockerfile").writeText(dockerfileContent)
  }

  fun checkImage(errorStream: OutputStream): ImageCheckResult {
    val writer = PrintWriter(errorStream)
    writer.println("Static code testing:")
    val staticCodeResult = staticCodeTester.test(writer)
    if (staticCodeResult != ImageCheckResult.PASSED) {
      return staticCodeResult
    }
    writer.println("Dynamic code testing:")
    return dynamicCodeTester.test(writer)
  }
}
