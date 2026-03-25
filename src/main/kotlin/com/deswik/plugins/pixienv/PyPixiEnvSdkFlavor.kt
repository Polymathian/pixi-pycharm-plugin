package com.deswik.plugins.pixienv

import com.jetbrains.python.sdk.flavors.CPythonSdkFlavor
import com.jetbrains.python.sdk.flavors.PythonFlavorProvider
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor

object PyPixiEnvSdkFlavor : CPythonSdkFlavor<PyPixiFlavorData>() {
    // override fun getIcon(): IconLoader.getIcon("/icons/pixi.svg", javaClass)
    override fun getFlavorDataClass(): Class<PyPixiFlavorData> = PyPixiFlavorData::class.java
    override fun getUniqueId(): String = "PixiEnvSdkFlavor"

    override fun isPlatformIndependent(): Boolean = true
}

internal class PixiEnvSdkFlavorProvider : PythonFlavorProvider {
    override fun getFlavor(p0: Boolean): PythonSdkFlavor<*>  = PyPixiEnvSdkFlavor

    // Needed for latest PyCharm versions, but not for older ones.
    public fun getFlavor(): PythonSdkFlavor<*>  = PyPixiEnvSdkFlavor
}