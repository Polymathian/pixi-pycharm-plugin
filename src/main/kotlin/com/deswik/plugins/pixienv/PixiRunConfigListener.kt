package com.deswik.plugins.pixienv

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.messages.MessageBusConnection
import java.io.File

@Service(Service.Level.PROJECT)
class PixiRunConfigListener(project: Project) {
    private val logger = Logger.getInstance(PixiRunConfigListener::class.java)
    private val connection: MessageBusConnection = project.messageBus.connect()

    init {
        connection.subscribe(
            ExecutionManager.EXECUTION_TOPIC,
            object : ExecutionListener {
                override fun processStartScheduled(executorId: String, env: ExecutionEnvironment) {
                    val runConfig = env.runProfile as? RunConfiguration ?: return
                    try {
                        // Only mutate in-memory config for this run
                        val projectBasePath = env.project.basePath ?: return
                        val pixiEnvDir = getPixiEnvDirForRun(runConfig, env.project) ?: return

                        // Not a .pixi environment, skip
                        if (!pixiEnvDir.exists() || !pixiEnvDir.isDirectory) return

                        val osName = System.getProperty("os.name").lowercase()
                        val envField = runConfig.javaClass.getMethod("getEnvs")

                        @Suppress("UNCHECKED_CAST")
                        val envs = envField.invoke(runConfig) as? MutableMap<String, String>
                        if (envs != null) {

                            val envName = pixiEnvDir.name

                            val processBuilder = if (osName.contains("win")) {
                                ProcessBuilder("pixi", "run", "--environment", envName, "--", "cmd", "/c", "echo", "%PATH%")
                            } else {
                                ProcessBuilder("pixi", "run", "--environment", envName, "--", "printenv", "PATH")
                            }
                            processBuilder.directory(File(projectBasePath))
                            processBuilder.redirectErrorStream(true)
                            val process = processBuilder.start()
                            val output = process.inputStream.bufferedReader().readText().trim()
                            val exitCode = process.waitFor()
                            if (exitCode == 0 && output.isNotBlank()) {
                                envs["PATH"] = output
                                logger.info("PixiRunConfigListener: Updated PATH for run configuration from pixi: $output")
                            } else {
                                logger.warn("PixiRunConfigListener: Failed to get PATH from pixi for env $envName (exit code: $exitCode, output: '$output')")
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("PixiRunConfigListener: Could not update PATH for run configuration", e)
                    }
                }
            }
        )
    }
    
    private fun getPixiEnvDirForRun(runConfig: RunConfiguration, project: Project): File? {
        // Try to get the SDK from the run configuration
        val sdk = try {
            val sdkField = runConfig.javaClass.getMethod("getSdk")
            sdkField.invoke(runConfig) as? com.intellij.openapi.projectRoots.Sdk
        } catch (_: Exception) {
            // Fallback: try to get project SDK
            com.intellij.openapi.roots.ProjectRootManager.getInstance(project).projectSdk
        }

        var homePath = sdk?.homePath ?: return null
        homePath = FileUtil.toSystemIndependentName(homePath)

        // Use regex with group to extract .pixi/envs/<envName> (cross-platform)
        val regex = Regex("(^.*/\\.pixi/envs/[^/]+)", RegexOption.IGNORE_CASE)
        val match = regex.find(homePath) ?: return null

        val envDir = match.groupValues[1].trimEnd('/', '\\')
            return File(envDir)
    }
}
