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
package com.davils.kreate.provenance

/**
 * The names of the tasks that judge where the dependencies come from.
 *
 * @since 4.0.0
 */
public object ProvenanceTaskNames {
    /**
     * Assesses how old the dependencies are and how their sources are maintained, using deps.dev.
     *
     * @since 4.0.0
     */
    public const val TRUST: String = "kreateDependencyTrust"

    /**
     * Looks for typosquatting, inflated versions, internal modules in the public repository and
     * plugin markers that lead elsewhere.
     *
     * @since 4.0.0
     */
    public const val CONFUSION: String = "kreateDependencyConfusion"
}
