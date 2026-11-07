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
package com.davils.kreate.policy

/**
 * The names of the tasks of the organisation policy.
 *
 * @since 4.0.0
 */
public object PolicyTaskNames {
    /**
     * Checks the build against the policy named by `kreate.assurance.policy`.
     *
     * @since 4.0.0
     */
    public const val CHECK: String = "kreateAssurancePolicyCheck"

    /**
     * Writes a policy that requires the current assurance settings, as a start for an organisation policy.
     *
     * @since 4.0.0
     */
    public const val TEMPLATE: String = "kreateAssurancePolicyTemplate"
}
