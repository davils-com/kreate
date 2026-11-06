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
package com.davils.kreate.trivy.task

import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Trivy scans depend on external vulnerability databases and tools")
internal abstract class TrivyFilesystemScan @Inject constructor(exec: ExecOperations) : TrivyDirectoryScan(
    "Scans the whole project directory for vulnerabilities, secrets and insecure configuration.",
    "vuln,secret,misconfig",
    "Trivy found problems in the project directory!",
    exec
)
