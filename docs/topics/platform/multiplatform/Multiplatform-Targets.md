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
the one thing it still had to program in `kotlin { }`. Kreate now declares them itself, once the build
script is evaluated and before any other feature looks at them, so the
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

Targets declared in `kotlin { }` stay as they are. Declaring the same target in both places fails in
the Kotlin plugin. A project with the Kotlin JVM plugin that asks for targets fails the build; a project
without a Kotlin plugin ignores them.

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

<seealso>
    <category ref="platform">
        <a href="Platform-Overview.md">Platform overview</a>
        <a href="Platform-Java-Modules.md">Java modules</a>
        <a href="Testing-Multiplatform.md">Multiplatform testing</a>
    </category>
</seealso>
