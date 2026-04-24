# JNI Support

Kreate provides comprehensive support for the Java Native Interface (JNI) in JVM modules. This enables seamless integration of C++ code into your JVM projects using CMake.

## Configuration

JNI configuration is done via the `jni` block within the `jvm` platform settings.

```kotlin
kreate {
    platform {
        jvm {
            jni {
                enabled = true
                nameOverride = "native-lib" // Optional name for the native project
                projectDirectory = file("jni") // Base directory for JNI sources
            }
        }
    }
}
```

## How it works

Once JNI is enabled, Kreate automates the entire build process:

1.  **Project Initialization**: The `initializeJniProject` task creates a minimal C++ project including `CMakeLists.txt` and example sources if they don't already exist.
2.  **Automated Build**: The `buildNative` task runs CMake to compile the shared library (`.so`, `.dylib`, or `.dll`). This task is automatically hooked into the Kotlin compilation process.
3.  **Runtime Configuration**: Kreate automatically configures the `java.library.path` for all `Test` and `JavaExec` tasks. This allows the native library to be found at runtime without manual path specifications.

## Directory Structure

Kreate follows the same convention pattern for JNI as for C-Interop:

```text
.
├── build.gradle.kts
├── src/main/kotlin/            # Kotlin sources
└── jni/                        # Root directory for JNI (configurable)
    └── native_lib/             # The native C++ project
        ├── CMakeLists.txt      # CMake configuration
        └── src/                # C++ source files
            └── native_lib.cpp
```

## Example Usage

### Kotlin Definition

Define your native methods in a Kotlin class:

```kotlin
package com.example

class NativeBridge {
    external fun helloFromCpp(): String

    companion object {
        init {
            System.loadLibrary("native_lib")
        }
    }
}
```

### C++ Implementation

Implement the corresponding function in C++:

```cpp
#include <jni.h>
#include <string>

extern "C" {
    JNIEXPORT jstring JNICALL
    Java_com_example_NativeBridge_helloFromCpp(JNIEnv* env, jobject thiz) {
        return env->NewStringUTF("Hello from C++!");
    }
}
```

## CMake Integration

Kreate generates a standard `CMakeLists.txt` that is already pre-configured for JNI. It automatically finds the required JNI headers from your JDK and links the corresponding libraries.

```cmake
cmake_minimum_required(VERSION 3.20)
project(native_lib CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(JNI REQUIRED)

file(GLOB NATIVE_LIB_SOURCES "src/*.cpp" "src/*.cc")

add_library(native_lib SHARED ${NATIVE_LIB_SOURCES})
target_include_directories(native_lib PRIVATE ${JNI_INCLUDE_DIRS} include)
target_link_libraries(native_lib PRIVATE ${JNI_LIBRARIES})
```

> **Note**: Ensure that CMake is installed on your system. On macOS, Kreate automatically searches in common paths (like Homebrew) if CMake is not found directly in the `PATH`.
