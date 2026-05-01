# Detekt Configuration Reference

The `detekt { }` block provides several properties to customize how static analysis is performed. These settings are designed to provide a balance between strictness and flexibility.

## Configuration Properties

The following properties are available directly within the `detekt { }` block:

### `enabled`
- **Type**: `Property<Boolean>`
- **Default**: `false`
- **Description**: Master switch for Detekt integration. When set to `true`, Kreate automatically applies the `io.gitlab.arturbosch.detekt` plugin and configures all Detekt tasks.

### `allRules`
- **Type**: `Property<Boolean>`
- **Default**: `true`
- **Description**: If enabled, Detekt will use all available rules, including experimental ones and those that are disabled by default in the standard Detekt configuration.

### `buildUponDefaultConfig`
- **Type**: `Property<Boolean>`
- **Default**: `true`
- **Description**: When `true`, your custom configuration file (if provided) will be merged with Detekt's default rule set. This is recommended to ensure you benefit from standard Kotlin best practices while still being able to override specific rules.

### `config`
- **Type**: `RegularFileProperty`
- **Default**: `detekt.yaml` at the root project level.
- **Description**: Specifies the path to the YAML configuration file containing rule definitions and suppressions.

---

## Task Management

Kreate automatically configures the following Gradle tasks when Detekt is enabled:

- `detekt`: Runs analysis on all source sets.
- `detektMain`: Runs analysis on the main source set.
- `detektTest`: Runs analysis on the test source set.

### Customizing Task Behavior
Since Kreate uses the official Detekt plugin, you can still access and modify the tasks directly if you need advanced customization not covered by the Kreate DSL:

```kotlin
tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    exclude("**/generated/**")
    parallel = true
}
```

## Integration with CI/CD
Detekt is an essential part of a modern CI/CD pipeline. By enabling `sarif` reports, you can integrate Detekt results directly into GitHub Actions or GitLab CI code quality views.

See [](Detekt-Reports.md) for more details on configuring output formats.
