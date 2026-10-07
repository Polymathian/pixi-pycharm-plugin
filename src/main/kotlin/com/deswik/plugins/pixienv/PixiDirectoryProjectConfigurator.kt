package com.deswik.plugins.pixienv

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectConfigurator

/**
 * Runs once, only on the very first open of a directory with no `.idea` yet - strictly before any
 * postStartupActivity. Without this, on a fresh clone PixiStartupActivity (a postStartupActivity) races
 * PyCharm's own "no interpreter configured yet" fallback (PythonSdkConfigurator.findSystemWideSdk), which
 * can win and assign a system-wide interpreter instead of the Pixi one. Running the same detection here
 * first gives Pixi's SDK a head start, so that fallback sees an interpreter is already configured and skips.
 */
class PixiDirectoryProjectConfigurator : DirectoryProjectConfigurator.AsyncDirectoryProjectConfigurator() {
    override suspend fun configure(project: Project, baseDir: VirtualFile, moduleRef: Ref<Module>, isProjectCreatedWithWizard: Boolean) {
        PixiStartupActivity().execute(project)
    }
}
