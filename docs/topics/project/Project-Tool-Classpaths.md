# Tool classpaths

<link-summary>Raising vulnerable dependencies of build tools.</link-summary>

<card-summary>Version floors for Dokka, Detekt, Kover and the Kotlin Gradle plugin.</card-summary>

The tools a build runs bring dependencies of their own, which Kreate locks and scans like every other
configuration. When a tool still asks for a vulnerable release, a version floor raises it:

```kotlin
kreate {
    project {
        toolClasspaths {
            floor("com.fasterxml.jackson", "2.22.3") {
                exclude("jackson-annotations")
            }
            floor("org.bouncycastle", "1.85")
        }
    }
}
```

A floor raises every module of its group and subgroups that a tool resolves below the floor, and
never lowers a version. `exclude` keeps modules that follow a numbering of their own. A version that
is no plain number, such as a range, is left alone.

Floors apply to the configurations whose name starts with `dokka`, `detekt`, `kover`,
`kotlinBouncyCastleConfiguration`, `kotlinCompilerClasspath`, `kotlinCompilerPluginClasspath`,
`kotlinBuildToolsApiClasspath`, `kotlinKlibCommonizerClasspath` or
`kotlinNativeCompilerPluginClasspath`. The compile and runtime classpaths of the project are never
touched. Refresh the locks after adding a floor.

<seealso>
    <category ref="project">
        <a href="Dependency-Locking.md">Dependency locking</a>
    </category>
</seealso>
