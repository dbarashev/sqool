package com.bardsoftware.sqool.codegen.docker

import com.bardsoftware.sqool.codegen.task.Task
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.messages.ContainerConfig
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

fun testDynamicCode(imageName: String, imageTasks: List<Task>, writer: PrintWriter): ImageCheckResult {
    val docker = DefaultDockerClient.fromEnv().build()
    val containerConfig = ContainerConfig.builder()
            .image(imageName)
            .build()
    val container = docker.createContainer(containerConfig)

    val tarStream = TarArchiveInputStream(docker.archiveContainer(container.id(), "/workspace"))
    var entry = tarStream.nextTarEntry
    while (entry != null) {
        val file = File("/", entry.name)
        if (!entry.isFile) {
            file.mkdir()
        } else {
            IOUtils.copy(tarStream, FileOutputStream(file))
        }
        entry = tarStream.nextTarEntry
    }
    tarStream.close()

    return ImageCheckResult.OK
}

fun main() {
    testDynamicCode("contest-image", emptyList(), PrintWriter(System.out))
}