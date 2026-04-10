package com.davils.kreate.module.platform.multiplatform

import com.davils.kreate.system.Architecture
import com.davils.kreate.system.OsTarget
import com.davils.kreate.system.getArchitecture
import com.davils.kreate.system.getOs
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

@JvmOverloads
public fun KotlinMultiplatformExtension.currentOs(configure: KotlinNativeTarget.() -> Unit = {}) {
    val arch by getArchitecture()
    val os by getOs()
    when (os) {
        OsTarget.WINDOWS -> mingwX64 {
            configure()
        }

        OsTarget.MACOS -> macosArm64 {
            configure()
        }

        OsTarget.LINUX -> {
            when (arch) {
                Architecture.X64 -> linuxX64 {
                    configure()
                }

                else -> linuxArm64 {
                    configure()
                }
            }
        }

        else -> {}
    }
}
