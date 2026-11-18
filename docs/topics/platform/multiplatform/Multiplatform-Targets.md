# Multiplatform targets

<link-summary>Declare the JVM, JavaScript and Wasm targets from the kreate block.</link-summary>

<card-summary>jvm, js and wasmJs from conventions, and Node.js from the settings.</card-summary>

```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.davils.kreate")
}

kreate {
    platform {
        multiplatform {
            toolsFromSettingsRepositories = true

            targets {
                jvm = true
                js = setOf(WebEnvironment.NODE)
                wasmJs = setOf(WebEnvironment.NODE)
            }
        }
    }
}
```

## Why declare targets here

A convention plugin of a library holds the `kreate { }` block and nothing else. The targets used to be
the one thing it still had to program in `kotlin { }`. Kreate now declares them itself as soon as the
`targets { }` block has run, while the build script is still evaluated, so the Kotlin plugin applies
compiler plugins such as KSP or kotlinx-serialization to them, and the
[Java module](Platform-Java-Modules.md), the [test suites](Testing-Multiplatform.md), API validation
and publishing see them like targets declared in `kotlin { }`.

| Property | Default | Declares |
|----------|---------|----------|
| `jvm`    | `false` | `jvm()` |
| `js`     | empty   | `js { }` with the IR compiler, one call per environment |
| `wasmJs` | empty   | `wasmJs { }`, one call per environment |

`WebEnvironment.NODE` calls `nodejs()`, `WebEnvironment.BROWSER` calls `browser()`. The klib of a
library is the same for both, so a library that only tests on Node.js still works in the browser.
Browser tests run through Karma and need a headless browser on the machine.

Targets declared in `kotlin { }` stay as they are; declaring the same target in both places configures
the one target. A value set outside the `targets { }` block, such as `targets.jvm = true`, is taken
up only after the build script is evaluated, too late for compiler plugins, so keep the block. A
project with the Kotlin JVM plugin that asks for targets fails the build; a project without a Kotlin
plugin ignores them.

## Node.js and Yarn from the settings

The Kotlin plugin downloads Node.js and Yarn for the JavaScript and Wasm targets and adds a repository
for each of them to the project. A build that sets `RepositoriesMode.FAIL_ON_PROJECT_REPOS` refuses
that:

```
Build was configured to prefer settings repositories over project repositories but repository
'Distributions at https://nodejs.org/dist' was added by unknown code
```

With `toolsFromSettingsRepositories = true`, Kreate keeps the Kotlin plugin from adding them, and the
settings declare where the tools come from:

```kotlin
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        mavenCentral()

        exclusiveContent {
            forRepository {
                ivy("https://nodejs.org/dist") {
                    name = "Node.js"
                    patternLayout {
                        artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                    }
                    metadataSources {
                        artifact()
                    }
                }
            }
            filter {
                includeModule("org.nodejs", "node")
            }
        }

        exclusiveContent {
            forRepository {
                ivy("https://github.com/yarnpkg/yarn/releases/download") {
                    name = "Yarn"
                    patternLayout {
                        artifact("v[revision]/[artifact](-v[revision]).[ext]")
                    }
                    metadataSources {
                        artifact()
                    }
                }
            }
            filter {
                includeModule("com.yarnpkg", "yarn")
            }
        }
    }
}
```

The Kotlin plugin installs the npm packages of every project in the root project, so the root project
of a multi-project build sets `toolsFromSettingsRepositories = true` as well.

## Pinning npm packages

The Kotlin plugin runs the tests of JavaScript and Wasm through Mocha, which it installs with Yarn
and records in `kotlin-js-store/yarn.lock` and `kotlin-js-store/wasm/yarn.lock`. A Trivy scan of
the repository reads those lock files. When it finds a vulnerable package there, pin its fixed
version:

```kotlin
kreate {
    platform {
        multiplatform {
            npmResolutions = mapOf(
                "serialize-javascript" to "7.1.3",
                "glob" to "14.0.2",
            )
        }
    }
}
```

Each entry becomes a Yarn resolution of both toolchains and wins over the range the depending
package asks for, so run the JavaScript and Wasm tests after pinning a new major version. Set the
same entries on the root project of a multi-project build, where the Kotlin plugin installs the
packages, and refresh the lock files with `kotlinUpgradeYarnLock` and `kotlinWasmUpgradeYarnLock`.

## One Yarn process at a time

`kotlinNpmInstall`, `kotlinWasmNpmInstall` and `kotlinWasmToolingSetup` each start Yarn, and with the
configuration cache Gradle runs them side by side. Yarn guards its shared cache with a network mutex
that does not hold on every machine, and on Windows a Yarn process could end successfully without
writing `yarn.lock`, so `kotlinStoreYarnLock` or `kotlinWasmStoreYarnLock` failed. Kreate lets these
tasks of the project it is applied to run one at a time; apply Kreate to the root project of a
multi-project build, where the Kotlin plugin registers them.

<seealso>
    <category ref="platform">
        <a href="Platform-Overview.md">Platform overview</a>
        <a href="Platform-Java-Modules.md">Java modules</a>
        <a href="Testing-Multiplatform.md">Multiplatform testing</a>
    </category>
</seealso>
