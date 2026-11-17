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

package com.davils.kreate.packaging

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration of the main jar of the project: the legal files it carries and the attributes of its
 * manifest. Gradle builds every archive reproducibly on its own.
 *
 * The main jar is `jar` of a JVM project and `jvmJar` of a multiplatform project.
 *
 * @param factory The object factory used to create the properties.
 * @since 4.3.0
 */
public abstract class ArchivesExtension @Inject constructor(factory: ObjectFactory) {
    /**
     * The files the main jar carries in `META-INF`, such as `LICENSE` and `NOTICE`. Empty by default.
     *
     * @since 4.3.0
     */
    public val legalFiles: ConfigurableFileCollection = factory.fileCollection()

    /**
     * Whether the manifest of the main jar names the implementation: `Implementation-Title`, the Java
     * module name or else the project name, `Implementation-Version`, the project version, and
     * `Implementation-Vendor`, the [vendor] when it is set. Defaults to `false`.
     *
     * @since 4.3.0
     */
    public val implementationManifest: Property<Boolean> = factory.property(Boolean::class.java).convention(false)

    /**
     * The vendor the manifest names as `Implementation-Vendor`. Unset by default.
     *
     * @since 4.3.0
     */
    public val vendor: Property<String> = factory.property(String::class.java)
}
