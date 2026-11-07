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
package com.davils.kreate.githook.hook

import com.davils.kreate.freshDirectory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class HookInstallerTest : FunSpec({
    val workspace = tempdir()

    context("the installer") {
        test("writes an executable hook that runs the hook task") {
            val hook = workspace.freshDirectory().resolve("hooks/pre-commit")

            HookInstaller.install(hook) shouldBe HookInstallation.CREATED

            hook.readText() shouldContain "exec ./gradlew --quiet kreateGitHookRun"
            hook.canExecute() shouldBe true
        }

        test("updates its own hook") {
            val hook = workspace.freshDirectory().resolve("pre-commit")
            HookInstaller.install(hook)

            HookInstaller.install(hook) shouldBe HookInstallation.UPDATED
        }

        test("never replaces a foreign hook") {
            val hook = workspace.freshDirectory().resolve("pre-commit")
            hook.writeText("#!/bin/sh\nmake lint\n")

            shouldThrow<IllegalStateException> { HookInstaller.install(hook) }

            hook.readText() shouldBe "#!/bin/sh\nmake lint\n"
        }

        test("removes only its own hook") {
            val directory = workspace.freshDirectory()
            val ours = directory.resolve("ours")
            val foreign = directory.resolve("foreign")
            HookInstaller.install(ours)
            foreign.writeText("#!/bin/sh\n")

            HookInstaller.uninstall(ours) shouldBe true
            HookInstaller.uninstall(foreign) shouldBe false
            ours.exists() shouldBe false
            foreign.exists() shouldBe true
        }
    }
})
