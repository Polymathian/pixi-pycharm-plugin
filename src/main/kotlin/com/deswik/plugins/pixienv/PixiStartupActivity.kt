package com.deswik.plugins.pixienv

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.flavors.PyFlavorAndData
import java.io.File

class PixiStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        thisLogger().info("PixiStartupActivity.execute called for project: ${project.name}")

        val pixiRoots = findPixiRoots(project)
        if (pixiRoots.isEmpty()) {
            thisLogger().info("No .pixi directories found in project ${project.name}. Skipping Pixi SDK setup.")
            return
        }

        val envs = pixiRoots.flatMap { getPixiEnvironments(it) }
        if (envs.isEmpty()) {
            thisLogger().info("No Pixi environments with Python found in project ${project.name}. Skipping Pixi SDK setup.")
            return
        }

        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                pixiRoots.forEach { excludePixiDirectory(project, File(it, ".pixi")) }

                thisLogger().info("Loading Pixi SDKs for project: ${project.name}")
                val addedSdks = envs.map { (env, versionString) ->
                    addPythonSdk(project, env, versionString)
                }
                thisLogger().info("Added Pixi SDKs for project: ${project.name}: ${addedSdks.joinToString { it.name }}")

                val currentSdk = ProjectRootManager.getInstance(project).projectSdk
                val isAlreadyPixiSdk = currentSdk?.homePath?.let { homePath ->
                    pixiRoots.any { root ->
                        homePath.startsWith(FileUtil.toSystemIndependentName(File(root, ".pixi").absolutePath))
                    }
                } == true
                if (isAlreadyPixiSdk) {
                    thisLogger().info("Project '${project.name}' is already using a Pixi SDK: ${currentSdk.name}")
                    return@runWriteAction
                }

                if (addedSdks.isNotEmpty()) {
                    val defaultSdk = addedSdks.find { it.name.endsWith("-pixi-default") } ?: addedSdks.first()
                    ProjectRootManager.getInstance(project).projectSdk = defaultSdk
                    thisLogger().info("SUCCESS: Project '${project.name}' is now using SDK: ${defaultSdk.name}")
                }
            }
        }
    }

    /**
     * Finds all content root directories (across all modules) that contain a .pixi directory.
     */
    private fun findPixiRoots(project: Project): List<File> {
        val candidates = mutableListOf<File>()

        project.basePath?.let { File(it) }?.also { candidates.add(it) }

        ModuleManager.getInstance(project).modules.flatMapTo(candidates) { module ->
            ModuleRootManager.getInstance(module).contentRoots.mapNotNull { vRoot ->
                vRoot.path.let { File(it) }
            }
        }

        return candidates.distinct().filter { File(it, ".pixi").exists() }
    }

    /**
     * Excludes the given .pixi directory from whichever content entries contain it.
     */
    private fun excludePixiDirectory(project: Project, pixiDir: File) {
        val pixiVFile = LocalFileSystem.getInstance().findFileByIoFile(pixiDir) ?: run {
            thisLogger().warn(".pixi directory not found in VFS: ${pixiDir.absolutePath}. Could not exclude.")
            return
        }

        ModuleManager.getInstance(project).modules.forEach { module ->
            val rootManager = ModuleRootManager.getInstance(module)
            val modifiableModel = rootManager.modifiableModel
            var modified = false
            modifiableModel.contentEntries.forEach { entry ->
                val entryFile = entry.file ?: return@forEach
                if (VfsUtilCore.isAncestor(entryFile, pixiVFile, false)) {
                    entry.addExcludeFolder(pixiVFile.url)
                    modified = true
                }
            }
            if (modified) {
                modifiableModel.commit()
                thisLogger().info("Marked ${pixiDir.absolutePath} as excluded in module '${module.name}'.")
            } else {
                modifiableModel.dispose()
            }
        }
    }

    /**
     * Returns (PyPixiEnv, versionString) pairs for all Pixi environments with Python in the given root dir.
     */
    private fun getPixiEnvironments(projectDir: File): List<Pair<PyPixiEnv, String>> {
        val pySdkType = PythonSdkType.getInstance()
        return PyPixiEnvProvider().getEnvs(projectDir).mapNotNull { env ->
            val pythonPath = FileUtil.toSystemIndependentName(env.pythonExecutable.absolutePath)
            val version = try { pySdkType.getVersionString(pythonPath) } catch (_: Exception) { null }
            if (!version.isNullOrBlank()) Pair(env, version) else null
        }
    }

    /**
     * Add a Python SDK for the given environment if not already present. Returns the SDK instance or null.
     */
    private fun addPythonSdk(project: Project, env: PyPixiEnv, version: String): Sdk {
        val projectName = project.name
        val pySdkType = PythonSdkType.getInstance()
        val jdkTable = ProjectJdkTable.getInstance()

        val pythonPath = FileUtil.toSystemIndependentName(env.pythonExecutable.absolutePath)
        val envName = env.envName

        // Check for existing SDK with the same homePath BEFORE creating a new one
        val existing = jdkTable.allJdks.find { it.homePath != null && FileUtil.pathsEqual(it.homePath!!, pythonPath) && it.sdkType == pySdkType }
        if (existing != null) {
            val additionalData = existing.sdkAdditionalData as? PythonSdkAdditionalData
            val isPixiFlavor = additionalData?.flavor?.javaClass == PyPixiEnvSdkFlavor::class.java
            if (isPixiFlavor) {
                thisLogger().info("SDK already exists for $pythonPath with correct Pixi flavor: ${existing.name}")
                return existing
            } else {
                jdkTable.removeJdk(existing)
                thisLogger().info("Removed SDK for $pythonPath with wrong flavor: ${existing.name}")
            }
        }

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

            val additionalData = PythonSdkAdditionalData(PyFlavorAndData(PyPixiFlavorData(env), PyPixiEnvSdkFlavor))

            @Suppress("UnstableApiUsage")
            try {
                additionalData.associatedModulePath = project.basePath
            }
            catch (e: Throwable) {
                thisLogger().warn("associateModulePath skipped due to missing or changed PyCharm internals: ${e.javaClass.simpleName}: ${e.message}")
            }

            modificator.sdkAdditionalData = additionalData
            modificator.commitChanges()
        }
        jdkTable.addJdk(sdk)

        thisLogger().info("SUCCESS: Created Python SDK: ${sdk.name}")
        return sdk
    }
}