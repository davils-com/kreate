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
package com.davils.kreate.policy.check

import com.davils.kreate.assurance.task.IgnoreRuleInput
import com.davils.kreate.dependency.ModuleCoordinate
import java.time.LocalDate

internal data class PolicyInput(
    val values: Map<String, String>,
    val ignoreRules: List<IgnoreRuleInput>,
    val modules: List<ModuleCoordinate>,
    val today: LocalDate
)
