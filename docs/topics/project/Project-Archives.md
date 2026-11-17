# Main jar

<link-summary>Legal files and the manifest of the main jar.</link-summary>

<card-summary>Ship LICENSE and NOTICE and name the implementation.</card-summary>

The main jar is `jar` of a JVM project and `jvmJar` of a multiplatform project. Gradle builds every
archive reproducibly on its own.

```kotlin
kreate {
    project {
        archives {
            legalFiles.from(isolated.rootProject.projectDirectory.file("LICENSE"))
            legalFiles.from(isolated.rootProject.projectDirectory.file("NOTICE"))
            implementationManifest = true
            vendor = "Example"
        }
    }
}
```

| Property                 | Default | Meaning                                                          |
|--------------------------|---------|------------------------------------------------------------------|
| `legalFiles`             | empty   | Files the main jar carries in `META-INF`                         |
| `implementationManifest` | `false` | Adds `Implementation-Title`, `-Version` and `-Vendor`            |
| `vendor`                 | unset   | The `Implementation-Vendor`; left out when unset                 |

`Implementation-Title` is the [Java module name](Platform-Java-Modules.md) or else the project name,
`Implementation-Version` the project version.

<seealso>
    <category ref="project">
        <a href="Publishing-Overview.md">Publishing</a>
    </category>
</seealso>
