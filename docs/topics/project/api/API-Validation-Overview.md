# Binary compatibility validation

<link-summary>Recording the public binary interface so that a breaking change cannot merge unnoticed.</link-summary>

<card-summary>A checked-in `.api` dump that fails the build when your public API changes.</card-summary>

<tldr>
<p><b>Enable</b>: <code>project { apiValidation { enabled = true } }</code></p>
<p><b>Requires</b>: nothing — no extra plugin</p>
<p><b>Tasks</b>: <code>kreateApiDump</code>, <code>kreateApiCheck</code></p>
</tldr>

A library's public API is a promise. Breaking it is easy — deleting an overload, widening a
return type, making a class `final` — and none of those changes look dangerous in a source
diff. They look like ordinary edits.

Binary compatibility validation makes them visible. Kreate reads the compiled classes,
writes every public and protected declaration to a file you commit, and fails the build
when the compiled reality and that file disagree. The change is then something a reviewer
approves deliberately, in a diff that shows exactly what a consumer will notice.

## Quick start

Validation is **disabled by default**. Enable it, then record the current interface:

```kotlin
kreate {
    project {
        apiValidation {
            enabled = true
        }
    }
}
```

```bash
./gradlew kreateApiDump
```

That writes `api/<project-name>.api`. Commit it. From then on `kreateApiCheck` runs as part
of `check`, and any change to the public interface fails the build until you re-run
`kreateApiDump` and commit the result.

## What the dump contains

The dump lists every declaration a consumer can link against, in JVM terms:

```
public final class com/example/Greeter {
	public fun <init> ()V
	public final fun greet (Ljava/lang/String;)Ljava/lang/String;
}
```

Names are internal names and types are JVM descriptors, because that is the form the JVM
resolves against. A change that is invisible in Kotlin source but visible to the linker —
a nullability-driven signature change, an inline class being boxed differently — shows up
here and nowhere else.

Left out are declarations no consumer can reach:

* `private` and package-private members.
* Kotlin `internal` declarations, including the bridges the compiler generates for their
  default arguments. These are `public` in bytecode with a mangled name; recording them
  would turn every rename inside your module into an apparent API change.
* Anything nested inside a hidden class.
* Compiler plumbing: `access$…` accessors, `…$annotations` holders, and the marker-only
  constructor emitted for a class whose real constructor is not public.
* Anything you exclude yourself — see [](API-Validation-Configuration.md).

## No extra plugin

Kreate reads the bytecode itself, with ASM. There is nothing to apply and no version to
keep in step with your Kotlin release.

> The file format is the one the Kotlin `binary-compatibility-validator` plugin writes. If
> you already have an `api/*.api` file from that plugin, remove the plugin, enable this
> feature and keep the file — Kreate reproduces it byte for byte. Kreate's own test suite
> asserts exactly that against a dump the plugin wrote.
>
{style="note"}

## Scope

Validation reads JVM class files. In a Kotlin Multiplatform project the `jvm` target's main
compilation is validated; Kotlin/Native, JavaScript and Wasm targets produce no class files and
are not covered by default. `klib = true` covers every target, klibs and an Android library target
included, through the Kotlin plugin's own ABI validation - see
[API validation configuration](API-Validation-Configuration.md).
