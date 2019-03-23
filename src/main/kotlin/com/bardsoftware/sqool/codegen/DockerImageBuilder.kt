package com.bardsoftware.sqool.codegen

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.filesystem.AbsoluteUnixPath
import java.io.File
import java.nio.file.Paths

fun buildDockerImage(
        course: String, module: String,
        variant: String, schemaPath: String,
        tasks: List<Task>
) {
    val root = createTempDir()
    val moduleFolder = File(root, "/workspace/$course/$module")
    moduleFolder.mkdirs()

    val codeGenerator = ContestCodeGenerator(module, schemaPath)
    val staticCode = codeGenerator.generateStaticCodeHeader() + "\n\n" +
            tasks.joinToString("\n\n") { codeGenerator.generateStaticCode(it) }
    File(moduleFolder, "$variant-static.sql").writeText(staticCode)
    tasks.forEach {
        val perSubmissionCode = codeGenerator.generateDynamicCode(it)
        File(moduleFolder, "${it.name}-dynamic.sql").writeText(perSubmissionCode)
    }

    Jib.from("busybox")
            .addLayer(listOf(Paths.get(root.toString(), "workspace")), AbsoluteUnixPath.get("/"))
            .containerize(
                    Containerizer.to(DockerDaemonImage.named("$course/$module/$variant"))
            )

    root.deleteRecursively()
}
