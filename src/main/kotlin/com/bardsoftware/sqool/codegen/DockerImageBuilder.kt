package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.docker.testDynamicCode
import com.bardsoftware.sqool.codegen.docker.testStaticCode
import com.bardsoftware.sqool.contest.Flags
import com.spotify.docker.client.DefaultDockerClient
import java.io.File
import java.io.OutputStream
import java.io.PrintWriter

fun buildDockerImage(imageName: String, contest: String, variants: List<Variant>) {
    val root = createTempDir()
    val contestDir = File(root, "workspace/$contest")
    contestDir.mkdirs()

    val schemas = variants.map { it.schemas }.flatten().toSet()
    val schemaDir = File(contestDir, "schema")
    schemaDir.mkdir()
    schemas.forEach { File(schemaDir, "${it.name}.sql").writeText(it.body) }

    for (variant in variants) {
        val variantDir = File(contestDir, variant.name)
        variantDir.mkdir()
        File(variantDir, "static.sql").writeText(variant.generateStaticCode("workspace/$contest/schema"))
        variant.tasks.forEach {
            val dynamicCode = it.generateDynamicCode(variant.name)
            File(variantDir, "${it.name}-dynamic.sql").writeText(dynamicCode)
        }
    }

    val initCode = variants.joinToString("\n") { "\\i '/workspace/$contest/${it.name}/static.sql'" }
    File(contestDir, "init.sql").writeText(initCode)

    createDockerfile(root, ".", "/")
    val docker = DefaultDockerClient.fromEnv().build()
    docker.build(root.toPath(), imageName)

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

enum class ImageCheckResult {
    PASSED, FAILED, ERROR
}

fun checkImage(
        imageName: String, contest: String, variants: List<Variant>,
        flags: Flags, errorStream: OutputStream
): ImageCheckResult {
    val writer = PrintWriter(errorStream)
    writer.println("Static code testing:")
    val staticCodeResult = testStaticCode(imageName, contest, flags, writer)
    if (staticCodeResult != ImageCheckResult.PASSED) {
        return staticCodeResult
    }
    writer.println("Dynamic code testing:")
    return testDynamicCode(imageName, contest, variants, flags, writer)
}