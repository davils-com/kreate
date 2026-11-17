# Benchmark configuration

<link-summary>Every property in the benchmark block, and how profiles relate to the kotlinx-benchmark DSL.</link-summary>

<card-summary>Profiles, the source set, and what Kreate deliberately does not manage.</card-summary>

## Properties

| Property         | Type                | Default        | Purpose                                                  |
|------------------|---------------------|----------------|----------------------------------------------------------|
| `enabled`        | `Property<Boolean>` | `false`        | Activates the feature                                     |
| `sourceSetName`  | `Property<String>`  | `benchmarks`   | Name of the source set holding the benchmarks             |
| `createSourceSet`| `Property<Boolean>` | `true`         | Whether Kreate creates and wires that source set          |
| `runtimeVersion` | `Property<String>`  | `0.5.0`        | Version of `kotlinx-benchmark-runtime`                    |
| `jmhVersion`     | `Property<String>`  | `1.37`         | JMH version used for JVM targets                          |
| `applyAllOpen`   | `Property<Boolean>` | `true`         | Whether Kreate applies `allopen` for `@State`             |

Keep `runtimeVersion` in step with the plugin version you applied — the runtime and the
plugin are released together.

## Profiles

A profile is one set of measurement settings. Each produces its own tasks and its own report
directory, which is how a short profile for continuous integration lives beside a long one
you run deliberately. `main` always exists.

```kotlin
kreate {
    project {
        benchmark {
            enabled = true

            profiles {
                named("main") {
                    warmups = 5
                    iterations = 5
                    iterationTime = 1
                    iterationTimeUnit = "s"
                    mode = "thrpt"
                    advanced("jvmForks", "1")
                }

                register("smoke") {
                    include("com\\.example\\.ParserBenchmark.*")
                    warmups = 0
                    iterations = 1
                    iterationTime = 200
                    iterationTimeUnit = "ms"
                }
            }
        }
    }
}
```

| Property            | Type                             | Default   |
|---------------------|----------------------------------|-----------|
| `warmups`           | `Property<Int>`                  | `5`       |
| `iterations`        | `Property<Int>`                  | `5`       |
| `iterationTime`     | `Property<Long>`                 | `1`       |
| `iterationTimeUnit` | `Property<String>`               | `s`       |
| `mode`              | `Property<String>`               | `thrpt`   |
| `outputTimeUnit`    | `Property<String>`               | unset     |
| `reportFormat`      | `Property<String>`               | `json`    |
| `includes`          | `ListProperty<String>`           | empty     |
| `excludes`          | `ListProperty<String>`           | empty     |
| `params`            | `MapProperty<String, List<String>>` | empty  |
| `advanced`          | `MapProperty<String, String>`    | `jvmForks=1` |

`mode` is `thrpt` (operations per unit of time, higher is better) or `avgt` (time per
operation, lower is better). The regression gate reads it to know which direction counts as
a regression.

> Changing `mode` or `outputTimeUnit` invalidates an existing baseline. The gate refuses to
> compare scores recorded in different units rather than producing a number that means
> nothing — re-record the baseline after such a change.
>
{style="warning"}

## What Kreate does not manage

The `profiles { }` block covers the settings that affect reproducibility and the gate. It is
deliberately not a copy of the whole kotlinx-benchmark surface: a copy would have to be
chased through every 0.4.x release, and it would create a second place to look.

Everything else stays available in the `benchmark { }` block the plugin installs, and Kreate
writes only the values listed above:

```kotlin
// kotlinx-benchmark's own block, untouched by Kreate
benchmark {
    targets {
        register("jvmOther")
    }
}
```

## The source set

By default Kreate creates `src/benchmarks/kotlin` and associates it with `main`. Set
`createSourceSet = false` to keep a hand-written setup and let Kreate configure only the
profiles and the gate — for instance when benchmarks live in a separate Gradle module, or
when you need a layout the association cannot express.
