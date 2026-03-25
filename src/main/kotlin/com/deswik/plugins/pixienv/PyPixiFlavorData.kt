package com.deswik.plugins.pixienv

import com.intellij.execution.target.TargetedCommandLineBuilder
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.sdk.flavors.PyFlavorData

data class PyPixiFlavorData(val env: PyPixiEnv) : PyFlavorData {
    override fun prepareTargetCommandLine(sdk: Sdk, targetCommandLineBuilder: TargetedCommandLineBuilder) {
        env.addPixiToTargetBuilder(targetCommandLineBuilder)
    }
}
