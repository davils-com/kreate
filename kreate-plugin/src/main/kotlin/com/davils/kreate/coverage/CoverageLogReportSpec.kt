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
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * The coverage summary printed to the build log.
 *
 * This is the report a CI system reads. GitLab extracts the coverage percentage for its badge and
 * merge request widget by matching a regular expression against the job log, so the shape of
 * [format] is part of the pipeline's contract rather than a cosmetic choice.
 *
 * @param factory The object factory used for creating properties.
 * @since 2.2.0
 */
public abstract class CoverageLogReportSpec @Inject constructor(
    factory: ObjectFactory
) : CoverageReportSpec(factory) {
    /**
     * The line printed for every measured entity.
     *
     * `<entity>` and `<value>` are substituted. Defaults to
     * `<entity> line coverage: <value>%`, which the GitLab regular expression documented under
     * CI integration matches — changing it means changing that regular expression too.
     *
     * @since 2.2.0
     */
    public val format: Property<String> =
        factory.property(String::class.java).convention("<entity> line coverage: <value>%")

    /**
     * A line printed once before the measurements.
     *
     * Unset by default.
     *
     * @since 2.2.0
     */
    public val header: Property<String> = factory.property(String::class.java)

    /**
     * The entity one line is printed for.
     *
     * Defaults to [Grouping.APPLICATION], which prints a single line — the one a CI regular
     * expression is meant to match.
     *
     * @since 2.2.0
     */
    public val groupBy: Property<Grouping> =
        factory.property(Grouping::class.java).convention(Grouping.APPLICATION)

    /**
     * The unit the printed value is measured in.
     *
     * Defaults to [CoverageUnit.LINE].
     *
     * @since 2.2.0
     */
    public val coverageUnit: Property<CoverageUnit> =
        factory.property(CoverageUnit::class.java).convention(CoverageUnit.LINE)

    /**
     * How the measurements of a group are aggregated into the printed value.
     *
     * Defaults to [Aggregation.COVERED_PERCENTAGE].
     *
     * @since 2.2.0
     */
    public val aggregation: Property<Aggregation> =
        factory.property(Aggregation::class.java).convention(Aggregation.COVERED_PERCENTAGE)
}
