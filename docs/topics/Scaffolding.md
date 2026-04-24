# Project Scaffolding

The `initializeJniProject` task creates the native C++ project structure on first run.
It is registered automatically when JNI is enabled and runs as a dependency of `buildNative`.

## What Gets Generated

Given a project named `example`, Kreate creates the following structure under the configured `projectDirectory`:

```text
jni/
└── example/
    ├── CMakeLists.txt      # CMake build definition
    └── src/
        └── example.cpp     # Placeholder C++ source file
```

All files are only written if they do **not** already exist. Running the task on an existing project
is safe and idempotent.

## Generated `CMakeLists.txt`

```cmake
cmake_minimum_required(VERSION 3.20)
project(example CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(JNI REQUIRED)

file(GLOB EXAMPLE_SOURCES "src/*.cpp" "src/*.cc")

add_library(example SHARED ${EXAMPLE_SOURCES})
target_include_directories(example PRIVATE ${JNI_INCLUDE_DIRS} include)
target_link_libraries(example PRIVATE ${JNI_LIBRARIES})
```

Key properties of the generated configuration:

- **C++17** standard enforced (`CMAKE_CXX_STANDARD 17`)
- **Position-independent code** enabled (`-fPIC`) for shared library compatibility
- **JNI headers** located and linked automatically via `find_package(JNI REQUIRED)`
- All `.cpp` and `.cc` files under `src/` are included via `file(GLOB ...)`
- An optional `include/` directory is pre-wired for custom headers

## Generated Placeholder Source

```cpp
#include <jni.h>

// Placeholder source for JNI project "example".
// Implement your native methods here.
```

This file ensures CMake has at least one translation unit to compile on the first build invocation.
Replace its content with your actual JNI function implementations.

## Running the Task Manually

```bash
./gradlew initializeJniProject
```

<note>
You do not need to run this task manually in normal development.
It is executed automatically as part of the <code>buildNative</code> dependency chain
whenever you compile your Kotlin sources.
</note>

## Customizing the CMake Configuration

After the initial scaffold, `CMakeLists.txt` is yours to modify freely.
Kreate never overwrites an existing `CMakeLists.txt`, so any changes you make are preserved across builds.

Common customizations include:

- Adding subdirectories with `add_subdirectory()`
- Linking additional system or third-party libraries with `target_link_libraries()`
- Adding compile definitions with `target_compile_definitions()`
- Including additional header search paths with `target_include_directories()`