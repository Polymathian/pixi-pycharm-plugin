package com.deswik.plugins.pixienv

import com.jetbrains.python.sdk.flavors.CPythonSdkFlavor
import com.jetbrains.python.sdk.flavors.PythonFlavorProvider
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import java.nio.file.Files
import java.nio.file.Path

object PyPixiEnvSdkFlavor : CPythonSdkFlavor<PyPixiFlavorData>() {
    // override fun getIcon(): IconLoader.getIcon("/icons/pixi.svg", javaClass)
    override fun getFlavorDataClass(): Class<PyPixiFlavorData> = PyPixiFlavorData::class.java
    override fun getUniqueId(): String = "PixiEnvSdkFlavor"

    override fun isPlatformIndependent(): Boolean = true

    /* Pycharm 2025 version */
    override fun isValidSdkPath(pythonBinaryPath: String): Boolean =
        super.isValidSdkPath(pythonBinaryPath) && pythonBinaryPath.isPixiEnvPath()

    /* Pycharm 2026 version */
    fun isValidSdkPath(pythonBinaryPath: Path): Boolean =
        Files.exists(pythonBinaryPath) && Files.isExecutable(pythonBinaryPath) && pythonBinaryPath.toString().isPixiEnvPath()
}

internal class PixiEnvSdkFlavorProvider : PythonFlavorProvider {
    fun getFlavor(p0: Boolean): PythonSdkFlavor<*>  = PyPixiEnvSdkFlavor

    override fun getFlavor(): PythonSdkFlavor<*>  = PyPixiEnvSdkFlavor
}