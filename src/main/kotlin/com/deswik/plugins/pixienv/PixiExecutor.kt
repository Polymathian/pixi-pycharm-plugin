package com.deswik.plugins.pixienv

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.errorProcessing.PyResult
import com.jetbrains.python.packaging.conda.CondaPackage
import java.io.File

data class PixiCommandResult(
    val exitCode: Int,
    val output: String,
    val isSuccess: Boolean = exitCode == 0
)

@Suppress("UnstableApiUsage")
class PixiExecutor {

    private val projectDir: File
    private val sdk: Sdk?

    constructor(projectDir: String?, sdk: Sdk? = null) {
        this.projectDir = File(projectDir ?: error("projectDir is not set"))
        this.sdk = sdk
    }

    fun install(): PyResult<Unit> {
        val result = runPixiCommand("install")
        return if (result.isSuccess) {
            PyResult.success(Unit)
        } else {
            PyResult.localizedError("Pixi install failed with exit code ${result.exitCode}: ${result.output}")
        }
    }

    @Suppress("UnstableApiUsage")
    fun listPackages(): PyResult<List<CondaPackage>> {
        val environment = sdk?.pixiEnvironmentName ?: error("sdk is not set")
        val result = runPixiCommand(
            "list",
            "--environment", environment
        )
        return if (result.isSuccess) {
            val packages = parsePixiListOutput(result.output)
            PyResult.success(packages)
        } else {
            PyResult.localizedError("Pixi list failed with exit code ${result.exitCode}: ${result.output}")
        }
    }

    fun installPackage(name: String, version: String?, feature: String?, isPypi: Boolean = false): PyResult<Unit> {
        val commands = buildList {
            add("add")
            add(name)
            if (version != null) add("==$version")
            if (isPypi) add("--pypi")
            add("--feature")
            add(feature ?: "default")
        }
        val result = runPixiCommand(*commands.toTypedArray())
        return if (result.isSuccess) {
            PyResult.success(Unit)
        } else {
            PyResult.localizedError("Failed to install package $name: ${result.output}")
        }
    }

    /**
     * Gets the Pixi workspace name via `pixi workspace name get`.
     * Returns null if pixi isn't available, the manifest has no name, or the command fails.
     */
    fun workspaceName(): String? {
        return try {
            val result = runPixiCommand("workspace", "name", "get")
            result.output.trim().takeIf { result.isSuccess && it.isNotBlank() }
        } catch (e: Exception) {
            thisLogger().warn("Failed to get Pixi workspace name for ${projectDir.absolutePath}: ${e.message}")
            null
        }
    }

    private fun runPixiCommand(vararg commands: String?): PixiCommandResult {
        val pixiFile = getSystemPixiExecutable()?.toFile() ?: error("Pixi executable not found")
        val processBuilder = ProcessBuilder(
            pixiFile.absolutePath,
            *commands.filterNotNull().toTypedArray()
        )
        processBuilder.directory(projectDir)
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return PixiCommandResult(exitCode, output)
    }

    private fun parsePixiListOutput(output: String): List<CondaPackage> {
        val lines = output.lines()
        if (lines.isEmpty()) return emptyList()

        // Skip header line (Name Version Build Size Kind Source)
        return lines.drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                // Split by whitespace and extract columns
                val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (parts.size < 2) return@mapNotNull null

                val name = parts[0]
                val version = parts[1]
                // Find the "Kind" column which indicates pypi or conda
                val kindIndex = parts.indexOfFirst { it.equals("pypi", ignoreCase = true) || it.equals("conda", ignoreCase = true) }
                val installedWithPip = if (kindIndex >= 0) parts[kindIndex].equals("pypi", ignoreCase = true) else false

                CondaPackage(
                    name = name,
                    version = version,
                    editableMode = false,
                    installedWithPip = installedWithPip
                )
            }
    }
}

