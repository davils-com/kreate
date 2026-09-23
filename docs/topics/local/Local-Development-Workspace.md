# Workspaces

<link-summary>One command to republish a library and everything downstream of it.</link-summary>

<card-summary>Declare the repositories and their edges; kreateLocalPublishAll does the order.</card-summary>

A change at the root of the dependency graph has to be pushed through every library above it before
it can be tried in the one that matters. Done by hand that means remembering both the set and the
order — and getting the order wrong does not produce an error. It produces a library published
against the *released* version of the thing that just changed, which is a confident wrong answer.

## Declaring a workspace

One repository declares the shape. A small repository that exists for nothing else is the usual
arrangement — it keeps the declaration out of any library that would otherwise have to know about
its siblings.

```kotlin
plugins {
    id("%plugin_id%") version "%version%"
}

kreate {
    local {
        workspace {
            // Defaults to the parent of this repository.
            root = file("../..")

            library("core")  { path = "libraries/core" }
            library("net") { path = "libraries/net"; dependsOn("core") }
            library("http") { path = "libraries/http"; dependsOn("core", "net") }
            library("json") { path = "libraries/json"; dependsOn("core", "net", "http") }
            library("ui") {
                path = "libraries/ui"
                dependsOn("json")
                tasks("kreateLocalPublish", ":ui-gradle:publishToMavenLocal")
            }
        }
    }
}
```

A library's name has to match the root project name of its build, so that the record a publish
writes can be matched back to the entry.

Only direct edges need declaring; the transitive order follows. `kreateLocalPublishAll` is
registered **only** in the repository that declares a workspace — a library cannot orchestrate a
workspace whose shape it does not know.

### Why the edges are declared and not derived

Reading them out of each repository's version catalog is tempting. It is also wrong: the catalog
records what a library was last *released* against, and during a refactor the edge that matters is
usually the one that is not in the catalog yet.

### The `tasks` override

A repository that also builds a Gradle plugin of its own as an included build has two things to
install, and the library's own task cannot reach the second. Consumers resolve that plugin as an
ordinary artifact, so it has to be named here.

## Running it

```bash
# Everything, in dependency order.
./gradlew kreateLocalPublishAll

# core changed — republish it and everything that depends on it.
./gradlew kreateLocalPublishAll --from core

# Exactly these, nothing downstream.
./gradlew kreateLocalPublishAll --only http,json
```

`--from` is the normal case. `--only` is for when you know the downstream libraries do not need
rebuilding and want the time back.

Each repository is built by its own wrapper in its own process — sharing a daemon across a dozen
unrelated builds would mean one JVM holding every plugin classpath in the workspace at once.

## When something fails

The run stops at the first failure and says what did not run:

```
Publishing 'http' failed (exit 1).

    Repository: /workspace/libraries/http
    Tasks:      kreateLocalPublish
    Not run:    json, ui, cli

The libraries published before this one are still installed; the workspace is half
updated until this is fixed and rerun.
```

Half a workspace is a real state to be left in, and the message says so rather than pretending the
run was atomic. Fix the failure and rerun — the libraries already published are simply republished.

A cycle or an edge pointing at an undeclared library fails while the build is *configured*, before
the first repository is touched.

<seealso>
    <category ref="local">
        <a href="Local-Development-Publishing.md">Publishing</a>
        <a href="Local-Development-Troubleshooting.md">Troubleshooting</a>
    </category>
</seealso>
