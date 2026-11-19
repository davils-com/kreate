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
package com.davils.kreate.standard

/**
 * The names of the tasks that check the standards a project implements.
 *
 * @since 5.3.0
 */
public object StandardsTaskNames {
    /**
     * Compares the reviewed state of every declared standard with what its body publishes today.
     *
     * @since 5.3.0
     */
    public const val AUDIT: String = "kreateStandardsAudit"

    /**
     * Checks that every declared standard was reviewed, that no review is due and that the code
     * names no standard it does not declare.
     *
     * @since 5.3.0
     */
    public const val INVENTORY: String = "kreateStandardsInventory"

    /**
     * Records the review of standards in the lock file.
     *
     * @since 5.3.0
     */
    public const val REVIEW: String = "kreateStandardsReview"
}
