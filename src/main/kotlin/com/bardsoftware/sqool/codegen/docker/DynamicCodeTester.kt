package com.bardsoftware.sqool.codegen.docker

import com.bardsoftware.sqool.codegen.task.Task
import com.bardsoftware.sqool.contest.Flags
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.messages.ContainerConfig
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.postgresql.ds.PGSimpleDataSource
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

    val testResults = tasksToTest.map {
        val submissionResult = tester.runTest(it.name, it.mockSolution)
        when {
            submissionResult.message == it.mockSolutionError -> {
                writer.println("OK")
                ImageCheckResult.PASSED
            }
            submissionResult.status == SubmissionResultStatus.ERROR -> {
                writer.println("Invalid ${it.name} sql:")
                writer.println(submissionResult.message)
                ImageCheckResult.FAILED
            }
            else -> {
                writer.println("Unexpected ${it.name}_Matcher result:")
                writer.println(submissionResult.message)
                ImageCheckResult.FAILED
            }
        }
    }

    writer.flush()
    return if (testResults.all { it == ImageCheckResult.PASSED }) ImageCheckResult.PASSED else ImageCheckResult.FAILED
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
            val file = File(destinationFolder, entry.name)
            if (!entry.isFile) {
                file.mkdir()
            } else {
                FileOutputStream(file).use { tarStream.copyTo(it) }
            }
            entry = tarStream.nextTarEntry
        }
    }
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
    private val dataSource = PGSimpleDataSource()

    init {
        with(flags) {
            dataSource.serverName = postgresAddress
            dataSource.portNumber = postgresPort.toInt()
            dataSource.user = postgresUser
            //dataSource.password = postgresPassword
        }
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