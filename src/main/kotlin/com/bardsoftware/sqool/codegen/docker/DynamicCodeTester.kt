package com.bardsoftware.sqool.codegen.docker

import com.bardsoftware.sqool.codegen.ImageCheckResult
import com.bardsoftware.sqool.codegen.task.Task
import com.bardsoftware.sqool.contest.Flags
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.messages.ContainerConfig
import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.sql.SQLException
import java.text.MessageFormat
import java.util.*

fun testDynamicCode(
        imageName: String, tasksToTest: List<Task>, flags: Flags, writer: PrintWriter
): ImageCheckResult {
    val CONTEST_DIRECTORY = "/workspace"
    File(CONTEST_DIRECTORY).deleteRecursively()
    copyDirectoryFromImage(imageName, CONTEST_DIRECTORY, "/")

    // assume there is only one course, module and variant in the image
    val contestSpec = getContestSpec(CONTEST_DIRECTORY)
    if (contestSpec == null) {
        writer.println("Invalid file structure")
        return ImageCheckResult.ERROR
    }
    val tester = CodeTester(contestSpec, flags)

    val testResults = tasksToTest.map { testTask(tester, it, writer) }

    return if (testResults.all { it == ImageCheckResult.PASSED }) {
        writer.println("OK")
        ImageCheckResult.PASSED
    } else {
        ImageCheckResult.FAILED
    }.also { writer.flush() }
}

private fun copyDirectoryFromImage(imageName: String, imagePath: String, destinationFolder: String) {
    val docker = DefaultDockerClient.fromEnv().build()
    val containerConfig = ContainerConfig.builder()
            .image(imageName)
            .build()
    val container = docker.createContainer(containerConfig)

    val tarStream = TarArchiveInputStream(docker.archiveContainer(container.id(), imagePath))
    tarStream.use {
        var entry = tarStream.nextTarEntry
        while (entry != null) {
            if (entry.isFile) {
                val file = File(destinationFolder, entry.name)
                file.parentFile.mkdirs()
                file.createNewFile()
                FileOutputStream(file).use { tarStream.copyTo(it) }
            }
            entry = tarStream.nextTarEntry
        }
    }

    docker.removeContainer(container.id())
    docker.close()
}

private fun getContestSpec(contestDirectory: String): ContestSpec? {
    val root = File(contestDirectory)

    for (courseDirectory in root.listFiles { file -> file.isDirectory }) {
        for (moduleDirectory in courseDirectory.listFiles { file -> file.isDirectory }) {
            val staticCodeExists = moduleDirectory.listFiles { file -> file.isFile }
                    .any { it.name.matches("(.*)-static.sql".toRegex()) }
            if (staticCodeExists) {
                return ContestSpec(courseDirectory.name, moduleDirectory.name)
            }
        }
    }

    return null
}

private data class ContestSpec(val course: String, val module: String)

private fun testTask(tester: CodeTester, task: Task, writer: PrintWriter): ImageCheckResult {
    val mockSubmissionOutput = tester.runTest(task.name, task.mockSolution)
    val mockSubmissionResult = when {
        mockSubmissionOutput.message?.matches(task.mockSolutionError) ?: false -> ImageCheckResult.PASSED

        mockSubmissionOutput.status == SubmissionResultStatus.ERROR -> {
            writer.println("Invalid ${task.name} sql:")
            writer.println(mockSubmissionOutput.message)
            ImageCheckResult.FAILED
        }

        else -> {
            writer.println("Unexpected ${task.name}_Matcher result for mock solution:")
            writer.println(mockSubmissionOutput.message)
            ImageCheckResult.FAILED
        }
    }

    val correctSubmissionOutput = tester.runTest(task.name, task.solution)
    val correctSubmissionResult = if (correctSubmissionOutput.message.isNullOrEmpty()) {
        ImageCheckResult.PASSED
    } else {
        if (correctSubmissionOutput.status != SubmissionResultStatus.ERROR) {
            writer.println("Unexpected ${task.name}_Matcher result for teacher's solution:")
            writer.println(mockSubmissionOutput.message)
        }
        ImageCheckResult.FAILED
    }

    return if (mockSubmissionResult == ImageCheckResult.PASSED && correctSubmissionResult == ImageCheckResult.PASSED)
        ImageCheckResult.PASSED else ImageCheckResult.FAILED
}

private enum class SubmissionResultStatus {
    SUCCESSFUL, FAILED, ERROR, TIMEOUT
}

private data class SubmissionResult(val status: SubmissionResultStatus, val message: String? = "") {
    companion object {
        val SUCCESSFUL = SubmissionResult(SubmissionResultStatus.SUCCESSFUL, null)
    }
}

private class CodeTester(contestSpec: ContestSpec, flags: Flags) {
    private val course = contestSpec.course
    private val module = contestSpec.module
    private val dataSource = HikariDataSource().apply {
        username = flags.postgresUser
        password = flags.postgresPassword
        jdbcUrl = "jdbc:postgresql://${flags.postgresAddress}:${flags.postgresPort}/${flags.postgresUser}"
        connectionTimeout = Long.MAX_VALUE
        idleTimeout = Long.MAX_VALUE
        validationTimeout = Long.MAX_VALUE
        loginTimeout = Int.MAX_VALUE
        initializationFailTimeout = Long.MAX_VALUE
    }

    fun runTest(task: String, solution: String): SubmissionResult {
        val connection = dataSource.connection
        val schemaName = "A" + UUID.randomUUID().toString().replace("-", "")

        val postgresDbms = connection.createStatement()
        try {
            postgresDbms.execute("create schema $schemaName;")
            postgresDbms.execute("set search_path = $schemaName,$module;")
            postgresDbms.queryTimeout = 120

            val scriptTemplate = File("/workspace/$course/$module/$task-dynamic.sql").readText()
            val formatArgs = listOf(task, solution)

            val userScript = MessageFormat.format(scriptTemplate, *formatArgs.toTypedArray())
            postgresDbms.execute(userScript)

            val result = mutableListOf<String>()
            val rowSet = postgresDbms.executeQuery("SELECT * FROM ${task}_Matcher()")
            while (rowSet.next()) {
                result.add(rowSet.getString(1))
            }
            return if (result.isEmpty()) SubmissionResult.SUCCESSFUL
            else SubmissionResult(SubmissionResultStatus.FAILED, result.joinToString("\n"))
        } catch (ex: SQLException) {
            return SubmissionResult(SubmissionResultStatus.ERROR, ex.message)
        }
    }
}
