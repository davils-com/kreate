/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kreate.module.platform.multiplatform.cinterop

/**
 * The native source language used for C-interop.
 *
 * Kreate supports bridging Kotlin/Native with libraries written in different
 * native languages. Each value selects a dedicated scaffolding and build
 * pipeline:
 *
 * - [RUST] builds a Cargo project and generates C headers with `cbindgen`.
 * - [C] and [CPP] build a CMake project that produces a static library and
 *   expose a hand-written C header consumed directly by Kotlin/Native.
 *
 * @since 1.3.0
 */
public enum class NativeLanguage {
    /**
     * Interoperability with a Rust library built through Cargo and `cbindgen`.
     *
     * This is the default language and preserves the historical Kreate
     * behavior.
     *
     * @since 1.3.0
     */
    RUST,

    /**
     * Interoperability with a C library built through CMake.
     *
     * The native project is compiled into a static library and bridged via a
     * hand-written C header located in the conventional `include` directory.
     *
     * @since 1.3.0
     */
    C,

    /**
     * Interoperability with a C++ library built through CMake.
     *
     * The native project is compiled into a static library and bridged via a
     * hand-written C header (using an `extern "C"` boundary) so that
     * Kotlin/Native can resolve the exported symbols.
     *
     * @since 1.3.0
     */
    CPP
}
