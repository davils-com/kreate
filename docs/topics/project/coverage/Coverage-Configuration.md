# Coverage configuration

<link-summary>Every property in the coverage block: engine, sources, instrumentation, filters.</link-summary>

<card-summary>What is measured, what is instrumented, and what is left out of the number.</card-summary>

The `coverage { }` block has four ways to change what ends up in the number, and they act at
different stages. Picking the wrong one produces a report that is subtly wrong rather than one
that fails loudly.

| Stage             | Block                 | Effect                                               |
|-------------------|-----------------------|------------------------------------------------------|
| Compilation       | `sources { }`         | Whole source sets are never considered               |
| Test execution    | `instrumentation { }` | No data is collected for these classes or test tasks |
| Report generation | `filters { }`         | Data exists but the class is left out of the report  |
| Verification      | `verify { }`          | See [Verification](Coverage-Verification.md)         |

## Top-level properties

### `enabled`
- **Type**: `Property<Boolean>`
- **Default**: `false`
- **Description**: Master switch. When `true`, %product% applies the
  `org.jetbrains.kotlinx.kover` plugin and configures its project settings, reports and
  verification rules. See [](Plugin-Management.md).

### `useJacoco`
- **Type**: `Property<Boolean>`
- **Default**: `false`
- **Description**: Measures with JaCoCo instead of Kover's own engine. Not feature-equivalent —
  filtering by annotation does not work under JaCoCo.

### `jacocoVersion`
- **Type**: `Property<String>`
- **Default**: unset, which uses Kover's bundled version
- **Description**: The JaCoCo version used when `useJacoco` is `true`.

---

## `sources { }`

Selects which source sets contribute code to the measurement. Code in an excluded source set is
not measured at all — the difference between "this code is untested" and "this code is not part of
the number".

### `includedSourceSets`
- **Type**: `ListProperty<String>`
- **Default**: empty, meaning every source set except those excluded

### `excludedSourceSets`
- **Type**: `ListProperty<String>`
- **Default**: empty
- **Description**: Test source sets are already excluded by the coverage engine. This is for what
  it cannot know about: generated sources, fixtures, or a source set that only supports another
  module's build.

<note>
The coverage engine recognises a test compilation by the name <code>test</code> and nothing else,
so a <a href="Testing-Suites.md">test suite</a> called <code>unitTest</code> would land in the
denominator as if it were code you ship. %product% appends every enabled suite's compilation name
to this list for you, controlled by
<code>tests { excludeSuitesFromCoverage }</code> (default <code>true</code>).

It does not do so when <code>includedSourceSets</code> is non-empty: an explicit include list
already decides what counts, and adding exclusions to it would only be a second opinion.
</note>

### `excludeJava`
- **Type**: `Property<Boolean>`
- **Default**: `false`
- **Description**: Leaves Java sources out. In a mixed project this produces a number describing
  only part of the artifact you ship, so make it a deliberate choice.

```kotlin
coverage {
    sources {
        excludedSourceSets = listOf("integrationTest", "fixtures")
    }
}
```

---

## `instrumentation { }`

Instrumentation is what makes coverage measurable, and it is not free: every instrumented class
carries counter bookkeeping at runtime. Irrelevant for a unit test, very relevant for a benchmark
or a load test where the instrumented timings are not the timings being measured.

### `disabledForAll`
- **Type**: `Property<Boolean>`
- **Default**: `false`
- **Description**: Switches instrumentation off for every test task. The reporting tasks remain
  but have nothing to report.

### `disabledForTestTasks`
- **Type**: `ListProperty<String>`
- **Default**: empty
- **Description**: Names of test tasks that run uninstrumented.

### `excludedClasses` / `includedClasses`
- **Type**: `ListProperty<String>`
- **Default**: empty
- **Description**: Fully qualified class names, `*` and `?` wildcards allowed.

```kotlin
coverage {
    instrumentation {
        // The measured timings have to be the real ones.
        disabledForTestTasks = listOf("benchmark", "loadTest")
    }
}
```

---

## `filters { }`

Decides which classes appear in the reports. This is what keeps the number honest: generated
constants, DSL marker types and data holders are counted as untested code by default, and a build
that leaves them in reports a figure describing its code generator rather than its tests.

Both `excludes { }` and `includes { }` accept the same four criteria:

| Criterion       | Matches                                                    |
|-----------------|------------------------------------------------------------|
| `classes`       | Fully qualified class names, wildcards allowed              |
| `packages`      | Package names — the package and its subpackages             |
| `annotatedBy`   | Classes or members carrying one of these annotations        |
| `inheritedFrom` | Classes extending or implementing one of these supertypes   |

Exclusion wins: a class matched by both is excluded.

```kotlin
coverage {
    filters {
        excludes {
            classes = listOf("com.example.*.BuildConstants")
            packages = listOf("com.example.generated")
            annotatedBy = listOf("com.example.Generated")
        }
    }
}
```

<tip>
Prefer <code>annotatedBy</code> over a name pattern for generated code. An annotation travels with
the generated source wherever it lands; a name pattern has to be kept in step with whatever the
generator emits next. This criterion requires the Kover engine.
</tip>

<warning>
<b>Kotlin compiles top-level declarations into a class of their own.</b> A file
<code>Main.kt</code> containing a <code>main()</code> function produces a class
<code>MainKt</code>, counted separately from any class declared in the same file. Excluding
<code>com.example.Main</code> does not exclude <code>com.example.MainKt</code>.
</warning>

## Instrumentation or filter?

Both can make a class stop lowering your coverage, by different means and with different results.

<deflist type="medium">
    <def title="Exclude from the report">
        The class leaves the numerator and the denominator. Use this for code that cannot
        meaningfully be tested here — a JNI wrapper whose constructor loads a native library,
        or generated code.
    </def>
    <def title="Exclude from instrumentation">
        No data is collected, but the class is still discovered from the classpath and still
        appears in the report at zero. Use this for the runtime cost, not to change a number.
    </def>
</deflist>

<seealso>
    <category ref="project">
        <a href="Coverage-Overview.md">Overview</a>
        <a href="Coverage-Reports.md">Reports</a>
        <a href="Coverage-Verification.md">Verification</a>
    </category>
</seealso>
