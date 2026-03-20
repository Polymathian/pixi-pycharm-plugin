package com.deswik.plugins.pixienv

import com.deswik.plugins.pixienv.PixiStartupActivity
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.io.FileUtil
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import java.io.File

class PixiStartupActivityTest : BasePlatformTestCase() {

    private lateinit var tempDir: File

    @Before
    override fun setUp() {
        super.setUp()
        tempDir = File(FileUtil.createTempDirectory("pixi-test", null, true), "project")
        tempDir.mkdirs()
        // Simulate project base path
        project.basePath = tempDir.absolutePath
    }

    @After
    override fun tearDown() {
        try {
            FileUtil.delete(tempDir)
        } finally {
            super.tearDown()
        }
    }

    fun `test no sdk created if no .pixi dir`() = runBlocking {
        val activity = PixiStartupActivity()
        activity.execute(project)
        val jdkTable = ProjectJdkTable.getInstance()
        val sdks = jdkTable.allJdks.filter { it.name.contains("pixi") }
        assertTrue("No SDKs should be created if .pixi is missing", sdks.isEmpty())
    }

    fun `test no sdk created if no python in envs`() = runBlocking {
        val pixiDir = File(tempDir, ".pixi/envs/testenv")
        pixiDir.mkdirs()
        val activity = PixiStartupActivity()
        activity.execute(project)
        val jdkTable = ProjectJdkTable.getInstance()
        val sdks = jdkTable.allJdks.filter { it.name.contains("pixi") }
        assertTrue("No SDKs should be created if no python executables", sdks.isEmpty())
    }

    fun `test sdk created for python env`() = runBlocking {
        val envName = "default"
        val envDir = File(tempDir, ".pixi/envs/$envName")
        envDir.mkdirs()
        val pythonExe = if (System.getProperty("os.name").lowercase().contains("win")) "python.exe" else "python"
        val pythonPath = File(envDir, pythonExe)
        pythonPath.writeText("") // create dummy python executable

        val activity = PixiStartupActivity()
        activity.execute(project)
        val jdkTable = ProjectJdkTable.getInstance()
        val sdks = jdkTable.allJdks.filter { it.name.contains("pixi") }
        assertTrue("SDK should be created for python env", sdks.any { it.homePath == FileUtil.toSystemIndependentName(pythonPath.absolutePath) })
    }

    fun `test default env set as project sdk`() = runBlocking {
        val envName = "default"
        val envDir = File(tempDir, ".pixi/envs/$envName")
        envDir.mkdirs()
        val pythonExe = if (System.getProperty("os.name").lowercase().contains("win")) "python.exe" else "python"
        val pythonPath = File(envDir, pythonExe)
        pythonPath.writeText("")

        val activity = PixiStartupActivity()
        activity.execute(project)
        val sdk = ProjectJdkTable.getInstance().allJdks.find { it.name.endsWith("-pixi-default") }
        assertNotNull("Default pixi env should be set as project SDK", sdk)
        assertEquals(sdk, com.intellij.openapi.roots.ProjectRootManager.getInstance(project).projectSdk)
    }
}
