# Configuration schema validation

<link-summary>Recording a configuration schema so that a change which breaks a deployed file cannot merge unnoticed.</link-summary>

<card-summary>A checked-in JSON Schema export that fails the build when a schema change would stop a deployed document loading.</card-summary>

<tldr>
<p><b>Enable</b>: <code>project { configurationSchema { enabled = true } }</code></p>
<p><b>Requires</b>: a configuration library on the project's own runtime classpath</p>
<p><b>Tasks</b>: <code>kreateConfigSchemaDump</code>, <code>kreateConfigSchemaCheck</code>, <code>kreateConfigValidate</code></p>
</tldr>

A configuration schema is a promise to every file already written against it, in exactly the way a
published signature is a promise to everything already compiled against one. Making a field required,
narrowing a type or removing an enum constant breaks every deployed document, and none of those
changes looks dangerous in a source diff - they look like ordinary edits, and the first anybody hears
of one is a validation failure at a customer's boot.

This is [binary compatibility validation](API-Validation-Overview.md) for that promise: a checked-in
export, a task that writes it, a task that compares against it, and `check` failing on a change that
nothing will repair.

## Quick start

Disabled by default, like every other Kreate feature. Say where the declaration lives, then record
the current export:

```kotlin
kreate {
    project {
        configurationSchema {
            enabled = true

            schema("server") {
                holder = "com.acme.config.ServerConfigKt"
                accessor = "getServerSchema"
            }
        }
    }
}
```

```bash
./gradlew kreateConfigSchemaDump
```

That writes `config-schema/server.json`. Commit it, and treat the directory the way you treat `api/`:
a change to it is a change a reviewer approves, so it belongs under the same CODEOWNERS rule.

From then on `kreateConfigSchemaCheck` runs as part of `check`. Never run it in the same invocation
as the dump: the two read and write the same files and Gradle refuses the implicit dependency.

## Why a build has to be told where the declaration is

A schema is built by Kotlin code rather than written down as data, so there is nothing for a build to
find by looking. The two names are that address:

- **`holder`** is the JVM class the declaration compiled into. A top level `val` in `ServerConfig.kt`
  lives on `com.acme.config.ServerConfigKt`; a member of an `object` lives on the object's own class.
- **`accessor`** is a method on it taking no arguments. Both spellings of a property are accepted -
  `getServerSchema` and `serverSchema` - because which one a property compiles to is not something a
  build file should have to know. Leave it out and the schema's own name is used.

**Kreate depends on no configuration library.** Everything is read by reflection over the project's
own runtime classpath, in a class loader of its own parented to the platform loader, so a project
that uses none is unaffected and a project that uses one is not pinned to whichever version this
plugin was built against. A facade that is not there is reported by name rather than as a
`ClassNotFoundException` out of the middle of a task.

## What decides that a change is breaking

Kreate does not. It asks the library and reports what it was told, and the rule the library applies
is the one worth knowing: a change that would stop an existing document loading is **breaking** when
the schema version has not moved, because nothing will run to repair it, and merely **a migration**
when it has, because a raised version is exactly the promise that a migration chain exists.

So the fix for a failing check is usually not to re-record the export. It is to raise the version and
write the migration that repairs a stored document - and then to record the export, because the
schema really did change.

Only what the export carries structurally is compared: presence, requiredness, type, enum constants
and defaults. A constraint is exported as prose and is not compared, which is stated here rather than
papered over - a check that quietly missed a category of change would be worse than one that says
which category it covers.

## The dry run over your own files

A repository usually carries the configuration it ships with, and nothing checks that those files
still load:

```kotlin
configurationSchema {
    enabled = true
    validation {
        holder = "com.acme.config.ValidationKt"
        accessor = "configurationReports"
    }
}
```

`kreateConfigValidate` calls that accessor and fails the build on any file that would not load,
reporting every one of them rather than stopping at the first - a run over forty files that names one
of them is a run somebody has to repeat thirty-nine times.

The accessor is a few lines of the project's own code rather than something Kreate assembles, and
that is deliberate. A dry run has to resolve the sources, the environment prefix and the secret
resolvers a real load would resolve, because a required field an environment variable supplies is not
missing and a reference nothing can resolve is a failure rather than a value. A build that assembled
that itself would be a second place the declaration is written, and the two would disagree the first
time either moved.

Nothing is written, no backup is made, no watcher is started and no handle is opened, so the check
does not change the thing it is checking.

## Where to go next

- [Binary compatibility validation](API-Validation-Overview.md), the same shape for a published
  signature.
- [Task reference](Task-Reference.md) for every task Kreate registers.
