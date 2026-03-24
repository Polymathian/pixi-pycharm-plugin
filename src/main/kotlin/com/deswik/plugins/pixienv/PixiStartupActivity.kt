package com.deswik.plugins.pixienv

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.flavors.VirtualEnvSdkFlavor
import java.io.File

class PixiStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        thisLogger().info("PixiStartupActivity.execute called for project: ${project.name}")

        val projectDir = project.basePath?.let { File(it) } ?: run {
            thisLogger().warn("Project basePath is null. Skipping Pixi setup.")
            return
        }
        val pixiDir = File(projectDir, ".pixi")
        if (!pixiDir.exists()) {
            thisLogger().info("No .pixi directory found in project ${project.name}. Skipping Pixi SDK setup.")
            return
        }

        // Get all pixi environments with python installed and their version
        val envs = getPixiEnvironments(pixiDir)
        if (envs.isEmpty()) {
            thisLogger().info("No Pixi environments with Python found in project ${project.name}. Skipping Pixi SDK setup.")
            return
        }

        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                excludePixiDirectory(project, pixiDir)

                thisLogger().info("Loading Pixi SDKs for project: ${project.name}")
                // Add all found environments as SDKs
                val addedSdks = envs.map { (envName, pythonPath, versionString) ->
                    addPythonSdk(project, envName, pythonPath, versionString)
                }
                thisLogger().info("Added Pixi SDKs for project: ${project.name}: ${addedSdks.joinToString { it.name }}")

                val currentSdk = ProjectRootManager.getInstance(project).projectSdk
                if (currentSdk?.homePath?.startsWith(FileUtil.toSystemIndependentName(pixiDir.absolutePath)) == true) {
                    thisLogger().info("Project '${project.name}' is already using a Pixi SDK: ${currentSdk.name}")
                    // Already has current .pixi env registered
                    return@runWriteAction
                }

                // Set the 'default' environment as project SDK if it exists, otherwise the first one.
                if (addedSdks.isNotEmpty()) {
                    val defaultSdk = addedSdks.find { it.name.endsWith("-pixi-default") } ?: addedSdks.first()
                    ProjectRootManager.getInstance(project).projectSdk = defaultSdk
                    thisLogger().info("SUCCESS: Project '${project.name}' is now using SDK: ${defaultSdk.name}")
                }
            }
        }
    }

    /**
     * Exclude the .pixi directory from the main module using a single write action.
     */
    private fun excludePixiDirectory(project: Project, pixiDir: File) {
        val moduleManager = ModuleManager.getInstance(project)
        val modules = moduleManager.modules
        if (modules.isEmpty()) {
            thisLogger().warn("No modules found in project ${project.name}. Cannot exclude .pixi directory.")
            return
        }

        val mainModule = modules[0]
        val moduleRootManager = ModuleRootManager.getInstance(mainModule)
        val modifiableModel = moduleRootManager.modifiableModel
        val contentEntries = modifiableModel.contentEntries
        val pixiVFile = LocalFileSystem.getInstance().findFileByIoFile(pixiDir)
        if (pixiVFile != null) {
            contentEntries.forEach { it.addExcludeFolder(pixiVFile.url) }
            modifiableModel.commit()
            thisLogger().info("Marked .pixi directory as excluded in main module.")
        } else {
            modifiableModel.dispose()
            thisLogger().warn(".pixi directory not found in VFS. Could not exclude.")
        }
    }

    /**
     * List all Pixi environments in the .pixi/envs folder and return (envName, pythonPath) pairs for those with Python installed.
     */
    private fun getPixiEnvironments(pixiDir: File): List<Triple<String, String, String>> {
        val allJdks = ProjectJdkTable.getInstance().allJdks

        val envsDir = File(pixiDir, "envs")
        if (!envsDir.exists() || !envsDir.isDirectory) return emptyList()

        val pythonExeName = if (System.getProperty("os.name").lowercase().contains("win")) "python.exe" else "python"
        val pySdkType = PythonSdkType.getInstance()
        val result = mutableListOf<Triple<String, String, String>>()

        envsDir.listFiles()?.forEach { envDir ->
            if (envDir.isDirectory) {
                val binPython = File(envDir, "bin/$pythonExeName")
                val rootPython = File(envDir, pythonExeName)
                val python = when {
                    binPython.exists() -> binPython
                    rootPython.exists() -> rootPython
                    else -> null
                }
                if (python != null && python.exists()) {
                    val pythonPath = FileUtil.toSystemIndependentName(python.absolutePath)

                    if (allJdks.any { it.sdkType == pySdkType && it.homePath != null && FileUtil.pathsEqual(it.homePath ?: "", pythonPath) }) {
                        thisLogger().info("SDK for ${envDir.name} already exists, skipping: $pythonPath")
                        return@forEach
                    }

                    val version = try { pySdkType.getVersionString(pythonPath) } catch (e: Exception) { null }
                    if (!version.isNullOrBlank()) {
                        result.add(Triple(envDir.name, pythonPath, version))
                    }
                }
            }
        }
        return result
    }

    /**
     * Add a Python SDK for the given environment if not already present. Returns the SDK instance or null.
     */
    private fun addPythonSdk(project: Project, envName: String, pythonPath: String, version: String): Sdk {
        val projectName = project.name
        val pySdkType = PythonSdkType.getInstance()
        val jdkTable = ProjectJdkTable.getInstance()

        val baseSdkName = "$projectName-pixi-$envName"
        var sdkName = baseSdkName
        var counter = 1
        while (jdkTable.findJdk(sdkName) != null) {
            sdkName = "$projectName-$counter-pixi-$envName"
            counter++
        }

        val sdk = jdkTable.createSdk(sdkName, pySdkType)
        sdk.sdkModificator.let { modificator ->
            modificator.homePath = pythonPath
            modificator.versionString = version
            associateModulePath(modificator, project.basePath)
            modificator.commitChanges()
        }
        jdkTable.addJdk(sdk)

        thisLogger().info("SUCCESS: Created Python SDK: ${sdk.name}")
        return sdk
    }

    /**
     * Associate SDK with project using internal API
     */
    @Suppress("UnstableApiUsage")
    private fun associateModulePath(modificator: SdkModificator, modulePath: String?) {
        try {
            var additionalData = modificator.sdkAdditionalData as? PythonSdkAdditionalData
            if (additionalData == null) {
                additionalData = PythonSdkAdditionalData(VirtualEnvSdkFlavor.getInstance())
                additionalData.associatedModulePath = modulePath
                modificator.sdkAdditionalData = additionalData
            }
        } catch (e: Throwable) {
            thisLogger().warn("associateModulePath skipped due to missing or changed PyCharm internals: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
