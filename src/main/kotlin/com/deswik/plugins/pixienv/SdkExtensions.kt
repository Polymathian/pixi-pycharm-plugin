package com.deswik.plugins.pixienv

import com.intellij.openapi.projectRoots.Sdk

/**
 * Extension property to check if this SDK is a Pixi environment.
 * Returns true if the SDK's home path contains ".pixi" directory.
 */
fun String.isPixiEnvPath() = contains("/.pixi/envs/") || contains("\\.pixi\\envs\\")

val Sdk.isPixi: Boolean
    get() = homePath?.contains(".pixi") == true

val Sdk.pixiEnvironmentName: String
    get() {
        // Extract environment name from SDK path which contains .pixi/envs/{env_name}/
        // SDK paths are always system-independent (use forward slashes)
        val homePath = homePath ?: return "default"

        val envNameRegex = """\\.pixi/envs/([^/]+)""".toRegex()
        val matchResult = envNameRegex.find(homePath)

        return matchResult?.groupValues?.get(1) ?: "default"
    }