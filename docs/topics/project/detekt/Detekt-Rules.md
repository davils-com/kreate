# Kreate rule set

<link-summary>The comment and KDoc rules Kreate puts on Detekt's classpath.</link-summary>

<card-summary>Six rules that enforce the Kreate comment and KDoc standard. They report; they never rewrite.</card-summary>

<tldr>
<p><b>Artifact</b>: <code>com.davils:kreate-detekt-rules</code>, published with Kreate and pinned to its version</p>
<p><b>Rule set id</b>: <code>kreate</code></p>
<p><b>Switch off</b>: <code>project { detekt { kreateRules = false } }</code></p>
</tldr>

Kreate ships a Detekt rule set of its own. When [Detekt is enabled](Detekt-Overview.md), the rule set
is added to the project's `detektPlugins` configuration and every rule in it is active — no entry in
your `detekt.yml` is required, because the artifact carries its own default configuration and Kreate
runs with `buildUponDefaultConfig = true`.

The rules encode the part of the Kreate Kotlin standard that is about what a file says rather than
what it does: **no `//` comments, KDoc on the published surface only, and a `@since` tag on
everything that is documented.**

<note>
Every rule <b>reports</b>. None of them rewrites a file, and none of them is auto-correctable. A
comment is deleted by the person who knows whether the sentence it holds belongs in a name, in a
test, or nowhere — that judgement is the work, and a tool that made it silently would be removing
the only record of a decision nobody wrote down.
</note>

## The rules

### ForbiddenLineComment

Reports every `//` comment, whether it stands alone or trails code.

A comment is not compiled, not tested and not renamed with the thing it describes, so it is the one
part of a file that can be wrong without anything failing. A name is checked by the compiler on
every build.

```kotlin
// Retry once, because the first attempt warms the connection pool.  // <- reported
send()

val warmsConnectionPool = send()                                     // <- the fix
```

Block comments and KDoc are untouched, so a copyright header written as `/* … */` is unaffected.

| Option           | Type     | Default | Meaning                                                                                |
|------------------|----------|---------|----------------------------------------------------------------------------------------|
| `allowedPattern` | `String` | `''`    | Regular expression a comment may match to be allowed. Empty allows none, which is the standard. |

A project that keeps directives in its sources names them:

```yaml
kreate:
  ForbiddenLineComment:
    allowedPattern: '^(region|endregion)\b'
```

### KDocOnNonPublicDeclaration

Reports a KDoc block on a declaration no consumer of the artifact can see.

The visibility considered is the effective one, which is what makes this different from Detekt's
`DocumentationOverPrivateFunction`: a `public` member of an `internal` class is reported, a class
declared inside a function body is reported, and `internal` — where a multi-module codebase keeps
most of its implementation — is covered at all.

A property declared in a constructor is judged by the visibility of its type, not of the
constructor. In `public class Key private constructor(public val id: String)` the property is as
public as the class, and its KDoc belongs where it is.

| Option           | Type      | Default | Meaning                                              |
|------------------|-----------|---------|------------------------------------------------------|
| `allowProtected` | `Boolean` | `false` | Whether a `protected` declaration may carry KDoc.    |

### KDocWithoutSinceTag

Reports a documented **public** declaration that does not say which version introduced it.

`@since` is the one tag a reader cannot reconstruct from the code. It answers the question every
consumer asks before calling a declaration — whether the version they are pinned to has it.

A non-public declaration is not reported here; `KDocOnNonPublicDeclaration` owns that case, and two
findings on one comment would leave it ambiguous what to do about it.

### SingleLineKDocWithBlockTag

Reports a one-line KDoc block that holds a description and a block tag at once.

```kotlin
/** A key exchanger. @since 1.0.0 */
```

This does not say what it looks like it says. The KDoc lexer only recognises a tag at the start of a
line, so the `@since` above is part of the description and no tooling — Dokka included — ever sees a
version. The same text over several lines carries a tag:

```kotlin
/**
 * A key exchanger.
 *
 * @since 1.0.0
 */
```

A one-line block holding only a description, or only tags, is unambiguous and is left alone.

### KDocClosingMarkerOnSharedLine

Reports a multi-line KDoc block whose closing marker shares a line with content. The marker on a
line of its own makes the block's extent obvious, and the next tag added to the block has to move it
anyway.

### KDocSeparatedFromDeclaration

Reports a blank line between a KDoc block and the declaration it documents.

Kotlin binds the comment across the blank line, so nothing is rendered as undocumented. What the
blank line costs is legibility: in a diff the comment reads as a floating note, and the annotation
below it reads as the start of something new rather than as part of the declaration the comment
belongs to.

## Test sources

Detekt analyses every source set, test suites included, and a test class is public like any other
declaration — so a documented test without `@since` is reported. Whether that is wanted is a decision
per project rather than per rule, and it is made the ordinary Detekt way:

```yaml
kreate:
  KDocWithoutSinceTag:
    excludes: [ '**/test/**', '**/unitTest/**', '**/integrationTest/**' ]
```

Kreate does not exclude them for you. A test suite is code that is read more often than most, and the
comment rules are worth at least as much there as in `main`.

## Switching rules off

Ordinary Detekt configuration. Per rule:

```yaml
kreate:
  KDocWithoutSinceTag:
    active: false
```

Or the whole set, in `detekt.yml`:

```yaml
kreate:
  active: false
```

To keep the artifact off the analysis classpath altogether — which is the only way to be sure it is
never resolved, for instance in an air-gapped build:

```kotlin
kreate {
    project {
        detekt {
            enabled = true
            kreateRules = false
        }
    }
}
```

<warning>
A project that sets <code>buildUponDefaultConfig = false</code> owns its entire Detekt
configuration, including this rule set's. The rules are then inactive until its
<code>detekt.yml</code> lists them.
</warning>

## Adopting the rules on an existing codebase

Findings in the thousands are the normal first result on a codebase that was never held to this
standard, and fixing them in one change is neither reviewable nor safe. Use Detekt's baseline:

```kotlin
plugins {
    id("dev.detekt")
}

detekt {
    baseline = file("config/detekt/baseline.xml")
}
```

`./gradlew detektBaseline` records what exists today, so the rules apply to everything written from
then on. Work the baseline down per module rather than per rule: the comments in one file are usually
one person's explanation of one design, and they are worth reading together.

## Using the rule set without Kreate

The artifact is a plain Detekt rule set, so any Gradle build can consume it:

```kotlin
dependencies {
    detektPlugins("com.davils:kreate-detekt-rules:<version>")
}
```

The version is a Kreate version. The rule set is published from the same repository, at the same
version, in the same release — which is what lets Kreate pin it and be sure of what it gets.

<seealso>
    <category ref="project">
        <a href="Detekt-Overview.md">Overview</a>
        <a href="Detekt-Configuration.md">Configuration</a>
        <a href="Detekt-Reports.md">Reports</a>
    </category>
</seealso>
