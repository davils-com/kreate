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
package com.davils.kreate.assurance

/**
 * Accepts one finding, with the reason it is accepted.
 *
 * @since 4.0.0
 */
public class AssuranceIgnoreSpec {
    /**
     * Why the finding is accepted. Required: a suppression nobody can explain is a finding.
     *
     * @since 4.0.0
     */
    public var reason: String? = null

    /**
     * The `group:artifact` the rule is limited to. When absent, it applies to every module.
     *
     * @since 4.0.0
     */
    public var module: String? = null

    /**
     * The last day the rule applies, as `YYYY-MM-DD`. When absent, the rule never expires.
     *
     * @since 4.0.0
     */
    public var expires: String? = null
}
