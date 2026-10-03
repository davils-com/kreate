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

package com.davils.kreate.coverage

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

/**
 * One side of a report filter: a set of criteria that select classes by name, package,
 * annotation, or supertype.
 *
 * The same specification type is used for both the include and the exclude side. Every criterion
 * accepts the `*` and `?` wildcards in fully qualified names.
 *
 * @param factory The object factory used for creating properties.
 * @since 2.2.0
 */
public abstract class CoverageFilterSpec @Inject constructor(
    /**
     * The object factory instance.
     * @since 2.2.0
     */
    factory: ObjectFactory
) {
    /**
     * Fully qualified class names, wildcards allowed.
     *
     * @since 2.2.0
     */
    public val classes: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * Package names, wildcards allowed. Selects every class in the package and its subpackages.
     *
     * @since 2.2.0
     */
    public val packages: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * Fully qualified names of annotations. Selects every class or member carrying one of them.
     *
     * This is the criterion that handles generated code properly: an annotation travels with the
     * generated source wherever it lands, whereas a name pattern has to be kept in step with the
     * generator's output. Note that it requires the Kover engine — JaCoCo cannot filter by
     * annotation.
     *
     * @since 2.2.0
     */
    public val annotatedBy: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())

    /**
     * Fully qualified names of supertypes. Selects every class that extends or implements one
     * of them.
     *
     * @since 2.2.0
     */
    public val inheritedFrom: ListProperty<String> =
        factory.listProperty(String::class.java).convention(emptyList())
}
