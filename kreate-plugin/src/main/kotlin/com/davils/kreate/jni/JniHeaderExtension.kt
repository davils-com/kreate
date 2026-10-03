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

package com.davils.kreate.jni

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configures generation of JNI headers from the project's compiled classes.
 *
 * Kotlin has no equivalent of `javac -h`, so the signatures of `external` functions
 * normally have to be transcribed into C++ by hand. A single typo in a mangled name such
 * as `Java_com_example_Foo_bar` compiles cleanly and only fails at runtime with an
 * `UnsatisfiedLinkError`. Generating the header removes that entire class of defect.
 *
 * @param factory The object factory used for creating properties.
 * @since 2.0.0
 */
public abstract class JniHeaderExtension @Inject constructor(
    /**
     * The object factory instance used to create Gradle properties.
     * @since 2.0.0
     */
    factory: ObjectFactory
) {
    /**
     * Whether headers are generated for every `native` method found in the compiled
     * classes.
     *
     * Defaults to `true`. The generated header is placed on the CMake include path
     * automatically, so a native source file only has to `#include` it.
     *
     * @since 2.0.0
     */
    public val enabled: Property<Boolean> = factory.property(Boolean::class.java).convention(true)

    /**
     * The file name of the generated header, without a directory component.
     *
     * Defaults to `<projectName>_jni.h`.
     *
     * @since 2.0.0
     */
    public val fileName: Property<String> = factory.property(String::class.java)
}
