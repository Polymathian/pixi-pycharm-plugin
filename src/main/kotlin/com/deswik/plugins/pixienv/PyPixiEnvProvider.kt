// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.deswik.plugins.pixienv

import java.io.File

class PyPixiEnvProvider {
    /**
     * Returns a list of PyPixiEnv (envName + pythonExecutable) for all valid environments with a python executable.
     */
    fun getEnvs(projectDir: File): List<PyPixiEnv> {
        val pixiManifest = File(projectDir, "pixi.toml").takeIf { it.exists() }
            ?: File(projectDir, "pyproject.toml").takeIf { it.exists() }
        val pixiEnvsDir = File(projectDir, ".pixi/envs")
        if (!pixiEnvsDir.exists() || !pixiEnvsDir.isDirectory) return emptyList()
        return pixiEnvsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { envDir ->
                val python = File(envDir, "bin/python")
                val pythonWin = File(envDir, "python.exe")
                when {
                    python.exists() -> PyPixiEnv(envDir.name, python, pixiManifest)
                    pythonWin.exists() -> PyPixiEnv(envDir.name, pythonWin, pixiManifest)
                    else -> null
                }
            } ?: emptyList()
    }
}