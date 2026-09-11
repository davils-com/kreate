# Coverage aggregation

<link-summary>Merging the coverage of several projects into one report.</link-summary>

<card-summary>One number for the product instead of one per module — without injecting plugins.</card-summary>

<tldr>
<p><b>Enable</b>: <code>coverage { aggregate { enabled = true } }</code> on the aggregating project</p>
<p><b>Requires</b>: the Kover plugin applied in <i>every</i> aggregated project</p>
</tldr>

In a multi-project build each project measures only itself. That produces one number per module
and none for the product — and it hides the case a modular codebase runs into constantly: a class
that is untested in its own module but exercised thoroughly by another module's tests counts as
uncovered in the first report and invisible in the second.

Aggregation merges the measurements so the report describes the whole.

## Enabling it

Enable it on the project that should own the combined report, usually the root:

```kotlin
kreate {
    project {
        coverage {
            enabled = true

            aggregate {
                enabled = true
            }
        }
    }
}
```

With no `projects` listed, every subproject is merged. Then run the reports from the aggregating
project:

```bash
./gradlew :koverXmlReport :koverVerify
```

## Naming the projects explicitly

```kotlin
aggregate {
    enabled = true
    projects = listOf(":core", ":service:api")
}
```

<tip>
Listing paths is the safer default in a build that gains modules over time. With the list left
empty, adding a module silently changes the number your gate is measured against — and a
threshold that moves on its own is not a threshold. With the list written down, a new module has
to be added deliberately.
</tip>

## Every aggregated project needs Kover

An aggregated project has to measure its own coverage before it can contribute any, so the Kover
plugin has to be applied in each of them:

```kotlin
// core/build.gradle.kts
plugins {
}
```

Only the aggregating project needs the `kreate { }` coverage configuration; the others just need
the plugin.

A project that is missing it gets it: %product% applies Kover to every project it aggregates, for
the same reason it applies it to the aggregating one. An aggregated project has to measure its own
coverage before it can contribute any, so listing it and applying the plugin there are the same
decision.

<note>
The projects are wired through Kover's own <code>kover</code> configuration rather than through its
<code>merge { }</code> block, which reaches into other projects to configure them as well as to
apply a plugin. Naming a project in <code>aggregate { }</code> should mean that its coverage is
counted, and nothing more.
</note>

## Verification applies to the merged number

The bounds on the aggregating project are checked against the merged measurement:

```kotlin
coverage {
    enabled = true

    aggregate {
        enabled = true
    }

    verify {
        minLineCoverage = 75
    }
}
```

That is the point of aggregating: the gate answers for the product rather than for whichever
module happens to be best tested. Individual modules can still carry their own bounds — a module
gate and a product gate answer different questions, and a codebase with a well-tested core and a
neglected edge passes the second while failing the first.

## Mixing engines does not work

Every project taking part has to use the same measurement engine. If one uses JaCoCo through
`useJacoco` and another uses Kover's own, the merged report cannot be produced. Set the engine the
same way everywhere, or aggregate only the projects that agree.

<seealso>
    <category ref="project">
        <a href="Coverage-Overview.md">Overview</a>
        <a href="Coverage-Verification.md">Verification</a>
        <a href="Coverage-Rules.md">Verification rules</a>
        <a href="Coverage-Reports.md">Reports</a>
    </category>
</seealso>
