package com.davils.kreate.module.platform.multiplatform

import com.davils.kreate.module.platform.multiplatform.cinterop.CInteropExtension
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import javax.inject.Inject

public abstract class MultiplatformExtension @Inject constructor(factory: ObjectFactory) {
    @get:Nested
    public abstract val cInterop: CInteropExtension

    public fun cInterop(action: Action<CInteropExtension>) {
        action.execute(cInterop)
    }
}
