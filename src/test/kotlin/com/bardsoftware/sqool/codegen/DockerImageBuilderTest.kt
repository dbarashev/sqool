package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.task.SingleColumnTask
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class DockerImageBuilderTest {
    @Test
    fun testBuildDockerImage() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECT 11;", spec)
        buildDockerImage(
                imageName = "contest-image", course = "hse2019", module =  "cw2",
                variant = "variant3", schemaPath = "/workspace/hse2019/cw2/schema3.sql", tasks = listOf(task))

        val process = Runtime.getRuntime().exec("docker run --rm test-image find /workspace")
        val folders = process.inputStream.bufferedReader()
                .use { it.readText() }
                .lines()
                .dropLastWhile { it.isEmpty() }
        val expectedFolders = listOf(
                "/workspace", "/workspace/hse2019", "/workspace/hse2019/cw2",
                "/workspace/hse2019/cw2/Task3-dynamic.sql", "/workspace/hse2019/cw2/variant3-static.sql"
        )
        assertEquals(expectedFolders.sorted(), folders.sorted())
    }
}
