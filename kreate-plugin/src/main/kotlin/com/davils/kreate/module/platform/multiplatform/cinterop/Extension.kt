package com.davils.kreate.module.platform.multiplatform.cinterop

import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import javax.inject.Inject

/**
 * Configuration for C-interop in Kotlin Multiplatform projects.
 *
 * This class provides properties to enable C-interop, specify the project directory,
 * set the package name, and configure definition files.
 *
 * @param objects The Gradle [ObjectFactory] used to create property instances.
 * @since 1.0.4
 * @author Nils Jaekel
 */
public abstract class CInteropExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * Configuration action for MinGW targets.
     *
     * @since 1.0.5
     */
    internal var mingwConfiguration: KotlinNativeTarget.() -> Unit = {}

    /**
     * Configuration action for Linux targets.
     *
     * @since 1.0.5
     */
    internal var linuxConfiguration: KotlinNativeTarget.() -> Unit = {}

    /**
     * Configuration action for macOS targets.
     *
     * @since 1.0.5
     */
    internal var macosConfiguration: KotlinNativeTarget.() -> Unit = {}

    /**
     * Whether C-interop should be enabled.
     *
     * Defaults to `false`.
     *
     * @since 1.0.4
     * @author Nils Jaekel
     */
    public val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    /**
     * Optional override for the interop name.
     *
     * @since 1.0.4
     * @author Nils Jaekel
     */
    public val nameOverride: Property<String> = objects.property(String::class.java)

    /**
     * The directory where the C project is located.
     *
     * @since 1.0.4
     * @author Nils Jaekel
     */
    public val projectDirectory: DirectoryProperty = objects.directoryProperty()

    /**
     * The Kotlin package name for the generated interop bindings.
     *
     * @since 1.0.4
     * @author Nils Jaekel
     */
    public val packageName: Property<String> = objects.property(String::class.java)

    /**
     * Configuration for definition files used in C-interop.
     *
     * @since 1.0.4
     * @author Nils Jaekel
     */
    @get:Nested
    public abstract val defFiles: DefFilesConfiguration

    /**
     * Configures the [DefFilesConfiguration] using the provided [action].
     *
     * @param action The configuration action to apply to [defFiles].
     * @since 1.0.4
     * @author Nils Jaekel
     */
    public fun defFile(action: Action<DefFilesConfiguration>) {
        action.execute(defFiles)
    }

    /**
     * Configures C-interop settings specifically for MinGW targets.
     *
     * This allows platform-specific adjustments to the [KotlinNativeTarget]
     * during the C-interop initialization process on Windows hosts.
     *
     * @param action The configuration action to apply to the [KotlinNativeTarget].
     * @since 1.0.8
     * @author Nils Jaekel
     */
    public fun mingw(action: KotlinNativeTarget.() -> Unit) {
        mingwConfiguration = action
    }

    /**
     * Configures C-interop settings specifically for Linux targets.
     *
     * This allows platform-specific adjustments to the [KotlinNativeTarget]
     * during the C-interop initialization process on Linux hosts.
     *
     * @param action The configuration action to apply to the [KotlinNativeTarget].
     * @since 1.0.8
     * @author Nils Jaekel
     */
    public fun linux(action: KotlinNativeTarget.() -> Unit) {
        linuxConfiguration = action
    }

    /**
     * Configures C-interop settings specifically for macOS targets.
     *
     * This allows platform-specific adjustments to the [KotlinNativeTarget]
     * during the C-interop initialization process on macOS hosts.
     *
     * @param action The configuration action to apply to the [KotlinNativeTarget].
     * @since 1.0.8
     * @author Nils Jaekel
     */
    public fun macos(action: KotlinNativeTarget.() -> Unit) {
        macosConfiguration = action
    }
}

/**
 * Configuration for definition files used in C-interop.
 *
 * @param objects The Gradle [ObjectFactory] used to create property instances.
 * @since 1.0.4
 * @author Nils Jaekel
 */
public abstract class DefFilesConfiguration @Inject constructor(objects: ObjectFactory) {
    /**
     * The name of the definition file.
     *
     * Defaults to `cinterop.def`.
     *
     * @since 1.0.4
     * @author Nils Jaekel
     */
    public val fileName: Property<String> = objects.property(String::class.java).convention("cinterop.def")

    /**
     * The directory name where definition files are stored.
     *
     * Defaults to `defs`.
     *
     * @since 1.0.4
     * @author Nils Jaekel
     */
    public val dirName: Property<String> = objects.property(String::class.java).convention("defs")
}