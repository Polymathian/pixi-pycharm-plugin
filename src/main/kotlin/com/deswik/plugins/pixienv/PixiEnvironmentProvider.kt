
package com.deswik.plugins.pixienv
import com.intellij.openapi.diagnostic.Logger

import com.intellij.openapi.project.Project
import com.jetbrains.python.run.AbstractPythonRunConfiguration
import com.jetbrains.python.run.PythonExecution
import com.jetbrains.python.run.PythonRunParams
import com.jetbrains.python.run.target.HelpersAwareTargetEnvironmentRequest
import com.jetbrains.python.run.target.PythonCommandLineTargetEnvironmentProvider

@Suppress("UnstableApiUsage")
class PixiEnvironmentProvider : PythonCommandLineTargetEnvironmentProvider {
    private val logger = Logger.getInstance(PixiEnvironmentProvider::class.java)

    override fun extendTargetEnvironment(
        project: Project,
        helpersAwareTargetRequest: HelpersAwareTargetEnvironmentRequest,
        pythonExecution: PythonExecution,
        runParams: PythonRunParams
    ) {
        if (runParams !is AbstractPythonRunConfiguration<*>) {
            logger.info("PixiEnvironmentProvider: Not an AbstractPythonRunConfiguration, skipping.")
            return
        }

        // Try to get the SDK from the run configuration
        val sdk = try {
            val sdkField = runParams.javaClass.getMethod("getSdk")
            sdkField.invoke(runParams) as? com.intellij.openapi.projectRoots.Sdk
        } catch (e: Exception) {
            logger.warn("PixiEnvironmentProvider: Could not get SDK from run configuration, falling back to project SDK.", e)
            com.intellij.openapi.roots.ProjectRootManager.getInstance(project).projectSdk
        }

        var homePath = sdk?.homePath
        if (homePath == null) {
            logger.info("PixiEnvironmentProvider: No SDK homePath found, skipping.")
            return
        }
        homePath = com.intellij.openapi.util.io.FileUtil.toSystemIndependentName(homePath)

        // Use regex with group to extract .pixi/envs/<envName> (cross-platform)
        val regex = Regex("(^.*/\\.pixi/envs/[^/]+)", RegexOption.IGNORE_CASE)
        val match = regex.find(homePath)
        if (match == null) {
            logger.info("PixiEnvironmentProvider: No .pixi env found in SDK path, skipping.")
            return
        }

        val pixiEnvDir = java.io.File(match.groupValues[1].trimEnd('/', '\\'))
        if (!pixiEnvDir.exists() || !pixiEnvDir.isDirectory) {
            logger.info("PixiEnvironmentProvider: Pixi env dir does not exist or is not a directory, skipping.")
            return
        }

        val osName = System.getProperty("os.name").lowercase()
        val envName = pixiEnvDir.name

        val processBuilder = if (osName.contains("win")) {
            ProcessBuilder("pixi", "run", "--environment", envName, "--", "cmd", "/c", "echo", "%PATH%")
        } else {
            ProcessBuilder("pixi", "run", "--environment", envName, "--", "printenv", "PATH")
        }
        project.basePath?.let { processBuilder.directory(java.io.File(it)) }
        processBuilder.redirectErrorStream(true)
        try {
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            if (exitCode == 0 && output.isNotBlank()) {
                pythonExecution.addEnvironmentVariable("PATH", output)
                logger.info("PixiEnvironmentProvider: Injected PATH for run configuration from pixi: $output")
            } else {
                logger.warn("PixiEnvironmentProvider: Failed to get PATH from pixi for env $envName (exit code: $exitCode, output: '$output')")
            }
        } catch (e: Exception) {
            logger.warn("PixiEnvironmentProvider: Could not update PATH for run configuration", e)
        }
    }
}
