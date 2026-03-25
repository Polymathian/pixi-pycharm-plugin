package com.deswik.plugins.pixienv

import com.intellij.execution.target.TargetedCommandLineBuilder
import com.intellij.openapi.diagnostic.thisLogger
import java.io.File

data class PyPixiEnv(
    val envName: String,
    val pythonExecutable: File,
    val pixiManifestLocation: File?
) {
    /**
     * Modifies the given TargetedCommandLineBuilder to run the command inside the Pixi environment.
     */
    fun addPixiToTargetBuilder(targetedCommandLineBuilder: TargetedCommandLineBuilder) {
        val manifestPath = pixiManifestLocation?.toString()

        val pixiPath = getSystemPixiExecutable()
        if (pixiPath != null) {
            targetedCommandLineBuilder.setExePath(pixiPath.toString())
            targetedCommandLineBuilder.addParameter("run")
            if (manifestPath != null) {
                targetedCommandLineBuilder.addParameter("--manifest-path")
                targetedCommandLineBuilder.addParameter(manifestPath)
            }
            targetedCommandLineBuilder.addParameter("--environment")
            targetedCommandLineBuilder.addParameter(envName)
            targetedCommandLineBuilder.addParameter("--")
            targetedCommandLineBuilder.addParameter(pythonExecutable.name)
        } else {
            // Log an error and fallback to python.
            thisLogger().error("Pixi executable not found. Falling back to using the python executable directly.")
            targetedCommandLineBuilder.setExePath(pythonExecutable.absolutePath)
        }
    }
}