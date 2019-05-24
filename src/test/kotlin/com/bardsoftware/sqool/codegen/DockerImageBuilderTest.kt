package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.docker.testStaticCode
import com.bardsoftware.sqool.codegen.task.SingleColumnTask
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.bardsoftware.sqool.contest.Flags
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.ContainerCreation
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class DockerImageBuilderTest {
    private val outputStream = ByteArrayOutputStream()

    companion object {
        private val dockerClient = DefaultDockerClient.fromEnv().build()
        private val postgresServer: ContainerCreation
        private lateinit var flags: Flags

        init {
            dockerClient.pull("postgres:10")
            val hostConfig = HostConfig.builder()
                    .portBindings(mapOf(Pair("5432/tcp", listOf(PortBinding.of("", "5432")))))
                    .build()
            val containerConfig = ContainerConfig.builder()
                    .image("postgres:10")
                    .exposedPorts("5432/tcp")
                    .hostConfig(hostConfig)
                    .build()
            postgresServer = dockerClient.createContainer(containerConfig)
        }

        @BeforeAll
        @JvmStatic
        fun runPostgresServer() {
            dockerClient.startContainer(postgresServer.id())
            val postgresServerIp = dockerClient.inspectContainer(postgresServer.id()).networkSettings().ipAddress()
            flags = mock {
                on { postgresAddress } doReturn postgresServerIp
                on { postgresPort } doReturn "5432"
                on { postgresUser } doReturn "postgres"
                on { postgresPassword } doReturn ""
            }
        }

        @AfterAll
        @JvmStatic
        fun stopPostgresServer() {
            dockerClient.killContainer(postgresServer.id())
            dockerClient.removeContainer(postgresServer.id())
            dockerClient.close()
        }
    }

    @AfterEach
    fun cleanOutputStream() {
        outputStream.reset()
    }

    @Test
    fun testFileStructure() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECT 11;", spec)
        buildDockerImage(
                imageName = "contest-image", course = "hse2019", module = "cw1",
                variant = "variant3", schemaPath = "/workspace/hse2019/cw1/schema3.sql", tasks = listOf(task))

        val process = Runtime.getRuntime().exec("docker run --rm contest-image find /workspace")
        val folders = process.inputStream.bufferedReader()
                .use { it.readText() }
                .lines()
                .dropLastWhile { it.isEmpty() }
        val expectedFolders = listOf(
                "/workspace", "/workspace/hse2019", "/workspace/hse2019/cw1",
                "/workspace/hse2019/cw1/Task3-dynamic.sql", "/workspace/hse2019/cw1/variant3-static.sql"
        )
        assertEquals(expectedFolders.sorted(), folders.sorted())
    }

    @Test
    fun testValidStaticSql() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECT 11 LIMIT 0;", spec)
        buildDockerImage(
                imageName = "contest-image", course = "hse2019", module = "cw3",
                variant = "variant3", schemaPath = "/workspace/hse2019/cw3/schema3.sql", tasks = listOf(task))
        val result = testStaticCode("contest-image", flags, PrintWriter(outputStream))

        assertEquals(ImageCheckResult.PASSED, result)
        assertEquals("OK\n", outputStream.toString())
    }

    @Test
    fun testInvalidStaticSql() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECTY 11", spec)
        buildDockerImage(
                imageName = "contest-image", course = "hse2019", module = "cw2",
                variant = "variant3", schemaPath = "/workspace/hse2019/cw2/schema3.sql", tasks = listOf(task))
        val result = testStaticCode("contest-image", flags, PrintWriter(outputStream))

        assertEquals(ImageCheckResult.FAILED, result)
        val expectedOutput = """
            |Invalid sql:
            |(NOTICE:  schema "cw2" does not exist, skipping
            |DROP SCHEMA|DROP SCHEMA
            |NOTICE:  schema "cw2" does not exist, skipping)
            |CREATE SCHEMA
            |SET
            |/workspace/hse2019/cw2/schema3.sql: No such file or directory
            |ERROR:  syntax error at or near "SELECTY"
            |LINE 3: SELECTY 11
            |        \^
            |CREATE FUNCTION
            |ERROR:  function task3_robot\(\) does not exist
            |LINE 2:    SELECT 0 AS query_id, \* FROM Task3_Robot\(\)
            |                                        \^
            |HINT:  No function matches the given name and argument types. You might need to add explicit type casts.
            |CREATE FUNCTION
            |DROP FUNCTION
            |
            |
            """.trimMargin().toRegex()
        assertTrue(outputStream.toString().matches(expectedOutput))
    }
}
