package com.deswik.plugins.pixienv

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.SystemProperties
import java.nio.file.Files
import java.nio.file.Path

private const val PIXI_EXE_NAME = "pixi.exe"
private const val PIXI_BINARY_NAME = "pixi"

private val LOG = Logger.getInstance("#com.deswik.plugins.pixienv.PixiExecutablesLocator")

fun getSystemPixiExecutable(): Path? {
    val pixiName = if (SystemInfo.isWindows) PIXI_EXE_NAME else PIXI_BINARY_NAME
    val userHome = Path.of(SystemProperties.getUserHome())
    val pixiPath = userHome.resolve(".pixi").resolve("bin").resolve(pixiName)
    if (Files.exists(pixiPath)) {
        LOG.info("Using $pixiPath as a pixi executable (found in user home .pixi/bin)")
        return pixiPath
    }
    val pixiInPath = PathEnvironmentVariableUtil.findInPath(pixiName)
    if (pixiInPath != null) {
        LOG.info("Using $pixiInPath as a pixi executable (found in PATH)")
        return pixiInPath.toPath()
    }
    LOG.info("System pixi executable is not found")
    return null
}





