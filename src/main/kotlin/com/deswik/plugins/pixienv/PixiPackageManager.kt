package com.deswik.plugins.pixienv

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.errorProcessing.PyResult
import com.jetbrains.python.packaging.common.PythonOutdatedPackage
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.packaging.management.PythonPackageManagerProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import java.lang.reflect.InvocationHandler

@Suppress("UnstableApiUsage")
class PixiPackageManagerProvider : PythonPackageManagerProvider {
    // Suspend version for current PyCharm versions (2025.x) - required for compilation
    override suspend fun createPackageManagerForSdk(project: Project, sdk: Sdk): PythonPackageManager? {
        return createPackageManagerImpl(project, sdk)
    }

    // Non-suspend signature for newer PyCharm versions (2026.1+)
    @JvmName("createPackageManagerForSdk")
    @Suppress("unused")
    fun createPackageManagerForSdkNonSuspend(project: Project, sdk: Sdk): PythonPackageManager? {
        return runBlocking {
            createPackageManagerImpl(project, sdk)
        }
    }

    private suspend fun createPackageManagerImpl(project: Project, sdk: Sdk): PythonPackageManager? {
        if (!sdk.isPixi) {
            return null
        }

        return try {
            // Run creation on IO dispatcher to avoid blocking EDT
            withContext(Dispatchers.IO) {
                createPixiPackageManager(project, sdk)
            }
        } catch (e: Exception) {
            thisLogger().error("Failed to create PixiPackageManager for SDK: ${sdk.name}", e)
            null
        }
    }

    private fun createPixiPackageManager(project: Project, sdk: Sdk): PythonPackageManager {
        // Create the implementation objects
        val pixiExec = PixiExecutor(project.basePath, sdk)
        val repositoryManager = PixiRepositoryManager(project, sdk)

        // InvocationHandler for abstract methods
        val handler = InvocationHandler { _, method, _ ->
            when (method.name) {
                "getRepositoryManager" -> repositoryManager
                "syncCommand" -> pixiExec.install()
                "loadPackagesCommand" -> pixiExec.listPackages()
                "loadOutdatedPackagesCommand" -> PyResult.success(emptyList<PythonOutdatedPackage>())

                else -> throw UnsupportedOperationException("Method ${method.name} not implemented")
            }
        }

        // Create dynamic subclass using ByteBuddy - only intercept abstract methods
        val dynamicType = ByteBuddy()
            .subclass(PythonPackageManager::class.java)
            .method(ElementMatchers.isAbstract())
            .intercept(InvocationHandlerAdapter.of(handler))
            .make()
            .load(javaClass.classLoader)
            .loaded

        // Detect and use the appropriate constructor
        val constructors = PythonPackageManager::class.java.declaredConstructors
        thisLogger().debug("Available PythonPackageManager constructors: ${constructors.map { 
            it.parameterTypes.joinToString { p -> p.simpleName } 
        }}")

        return try {
            // Try 2-arg constructor (PyCharm 2025.x)
            val constructor2 = dynamicType.getConstructor(Project::class.java, Sdk::class.java)
            constructor2.newInstance(project, sdk) as PythonPackageManager
        } catch (@Suppress("SwallowedException") e: NoSuchMethodException) {
            // Try 3-arg constructor (PyCharm 2026.1+)
            val constructor3 = dynamicType.declaredConstructors.find {
                it.parameterCount == 3 &&
                it.parameterTypes[0] == Project::class.java &&
                it.parameterTypes[1] == Sdk::class.java
            } ?: throw NoSuchMethodException("No suitable PythonPackageManager constructor found")

            constructor3.isAccessible = true
            constructor3.newInstance(project, sdk, false) as PythonPackageManager
        }
    }
}