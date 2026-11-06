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

package com.davils.kreate.archive

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ArchivePathInspectorTest : FunSpec({
    listOf(
        "a.txt",
        "a/b/c.class",
        "META-INF/MANIFEST.MF",
        "./a",
        "a/./b",
        "a..b/c",
        "...",
        "dir/",
        ""
    ).forEach { path ->
        test("accepts '$path'") {
            ArchivePathInspector.inspect(path).shouldBeNull()
        }
    }

    mapOf(
        ".." to ArchivePathIssue.TRAVERSAL,
        "../a" to ArchivePathIssue.TRAVERSAL,
        "a/.." to ArchivePathIssue.TRAVERSAL,
        "a/b/../../../c" to ArchivePathIssue.TRAVERSAL,
        "..\\a" to ArchivePathIssue.TRAVERSAL,
        "a\\..\\b" to ArchivePathIssue.TRAVERSAL,
        "/a" to ArchivePathIssue.ABSOLUTE,
        "\\a" to ArchivePathIssue.ABSOLUTE,
        "\\\\host\\share" to ArchivePathIssue.ABSOLUTE,
        "C:/a" to ArchivePathIssue.ABSOLUTE,
        "c:\\a" to ArchivePathIssue.ABSOLUTE,
        "C:a" to ArchivePathIssue.ABSOLUTE,
        "a\u0000b" to ArchivePathIssue.CONTROL_CHARACTER,
        "a\nb" to ArchivePathIssue.CONTROL_CHARACTER
    ).forEach { (path, issue) ->
        test("reports $issue for an entry path") {
            ArchivePathInspector.inspect(path) shouldBe issue
        }
    }
})
