# Benchmarks

<link-summary>Measuring performance with kotlinx-benchmark, and holding the numbers against a committed baseline.</link-summary>

<card-summary>JMH benchmarks with a source set Kreate builds for you and a regression gate that fails the build.</card-summary>

<tldr>
<p><b>Enable</b>: <code>project { benchmark { enabled = true } }</code></p>
<p><b>Applies</b>: the <code>org.jetbrains.kotlinx.benchmark</code> plugin</p>
<p><b>Tasks</b>: <code>kreateBenchmarkBaseline</code>, <code>kreateBenchmarkCheck</code>, <code>kreateBenchmarkReport</code></p>
</tldr>

[kotlinx-benchmark](https://github.com/Kotlin/kotlinx-benchmark) measures. What it does not
do is tell you whether today's number is worse than last month's — and a benchmark nobody
compares against an earlier run is a number in a build log.

Kreate adds the parts around the measurement: the source set, the compiler plugin JMH
needs, a report at a path other tasks can depend on, and a baseline you commit and review.

## Quick start

Enable the integration; %product% applies the plugin:

```kotlin
kreate {
    project {
        benchmark {
            enabled = true
        }
    }
}
```

Write a benchmark in `src/benchmarks/kotlin`:

```kotlin
package com.example

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State

@State(Scope.Benchmark)
class ParserBenchmark {
    private val input = "…"

    @Benchmark
    fun parse(): Document = Parser.parse(input)
}
```

Record the baseline and commit it:

```bash
./gradlew kreateBenchmarkBaseline
```

From then on `kreateBenchmarkCheck` runs the benchmarks and fails when one got measurably
slower. It is **not** wired into `check`: a benchmark run takes minutes, and attaching it to
the ordinary build is the surest way to get it switched off.

## What Kreate sets up

Enabling the feature replaces the setup the kotlinx-benchmark documentation asks you to
write by hand:

* **A `benchmarks` source set**, kept apart from `main` so that JMH's generated code and the
  `allopen` transformation never reach the artifact you publish.
* **An association with `main`**, so the benchmarks see its `internal` declarations and
  inherit its dependencies. Without it you would be benchmarking a facade.
* **The `kotlinx-benchmark-runtime` dependency** on that source set.
* **The `allopen` compiler plugin**, configured for `org.openjdk.jmh.annotations.State`.
  JMH subclasses every `@State` class, so a benchmark class that is final fails during
  generation with an error that never mentions `allopen`.
* **The measurement profiles**, with defaults chosen for reproducibility rather than speed —
  including a fixed fork count, which is the difference between a measurement and a
  snapshot of whatever state the JIT happened to be in.

## The plugin is applied for you

Enabling `benchmark { }` applies kotlinx-benchmark; your `plugins { }` block does not need to
mention it. Note that the plugin is published to the Gradle Plugin Portal only, which a build
resolving its buildscript classpath through an internal mirror has to account for.

It is a 0.4.x release with an API that is not marked stable, so pin the version yourself if you
need a particular one:

```kotlin
plugins {
    id("org.jetbrains.kotlinx.benchmark") version "<version>"
}
```

Kreate's own application is then a no-op.

## Scope

The source set scaffolding covers Kotlin/JVM and the JVM target of a Kotlin Multiplatform
project. kotlinx-benchmark also supports Kotlin/Native, JS and Wasm; register those targets
yourself in the `benchmark { }` block it installs. The regression gate reads the JSON report
and does not care which platform produced it.
