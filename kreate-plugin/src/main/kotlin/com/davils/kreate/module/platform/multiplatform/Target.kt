/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
