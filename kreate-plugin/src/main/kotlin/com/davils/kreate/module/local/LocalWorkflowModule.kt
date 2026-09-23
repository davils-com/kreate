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

package com.davils.kreate.module.local

import com.davils.kreate.KreateExtension
import com.davils.kreate.module.Module
import org.gradle.api.Project

/**
 * Module for the local development workflow.
 *
 * Registers the tasks that install a repository into the local Maven repository and manage what
 * is installed there. Resolving from it is the other half of the feature and lives in the
 * separate `com.davils.kreate.settings` plugin, because substitution has to be in place before
 * anything resolves and has to reach `build-logic`, which does not apply this plugin.
 *
 * @since 3.2.0
 */
internal object LocalWorkflowModule : Module {
    /**
     * Applies the local development module to the project.
     *
     * Applied on the root project only. The tasks describe a repository and a machine rather than
     * a module, and registering them per project would give `./gradlew kreateLocalPublish` one
     * invocation per subproject — each racing the others to write the same record.
     *
     * @param project The Gradle project to configure.
     * @param extension The Kreate configuration extension.
     * @since 3.2.0
     */
    override fun apply(project: Project, extension: KreateExtension) {
        if (project != project.rootProject) return

        // Last of the modules, so that `ProjectModule` has already resolved the version — the
        // record has to carry the suffixed version that was actually published, not the one the
        // project started with.
        project.afterEvaluate {
            initializeLocalWorkflow(extension)
        }
    }
}
