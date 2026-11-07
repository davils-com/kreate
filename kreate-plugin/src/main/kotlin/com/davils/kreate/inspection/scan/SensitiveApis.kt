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
package com.davils.kreate.inspection.scan

private const val PROCESS_EXECUTION: String = "PROCESS_EXECUTION"

private const val NATIVE_LIBRARY_LOAD: String = "NATIVE_LIBRARY_LOAD"

private const val DYNAMIC_CLASS_DEFINITION: String = "DYNAMIC_CLASS_DEFINITION"

private const val JNDI_LOOKUP: String = "JNDI_LOOKUP"

private const val RAW_SOCKET: String = "RAW_SOCKET"

private const val CONSTRUCTOR: String = "<init>"

private val SENSITIVE_CALLS: Map<String, Map<String, String>> = mapOf(
    "java/lang/ProcessBuilder" to mapOf("start" to PROCESS_EXECUTION),
    "java/lang/Runtime" to mapOf(
        "exec" to PROCESS_EXECUTION,
        "load" to NATIVE_LIBRARY_LOAD,
        "loadLibrary" to NATIVE_LIBRARY_LOAD
    ),
    "java/lang/System" to mapOf("load" to NATIVE_LIBRARY_LOAD, "loadLibrary" to NATIVE_LIBRARY_LOAD),
    "java/lang/ClassLoader" to mapOf("defineClass" to DYNAMIC_CLASS_DEFINITION),
    "java/lang/invoke/MethodHandles\$Lookup" to mapOf("defineClass" to DYNAMIC_CLASS_DEFINITION),
    "java/net/URLClassLoader" to mapOf(CONSTRUCTOR to DYNAMIC_CLASS_DEFINITION),
    "javax/script/ScriptEngine" to mapOf("eval" to "SCRIPT_EVALUATION"),
    "javax/naming/Context" to mapOf("lookup" to JNDI_LOOKUP),
    "javax/naming/InitialContext" to mapOf("lookup" to JNDI_LOOKUP),
    "java/io/ObjectInputStream" to mapOf("readObject" to "UNSAFE_DESERIALIZATION"),
    "java/net/Socket" to mapOf(CONSTRUCTOR to RAW_SOCKET, "connect" to RAW_SOCKET),
    "java/net/ServerSocket" to mapOf(CONSTRUCTOR to RAW_SOCKET)
)

internal object SensitiveApis {
    fun ruleFor(owner: String, method: String): String? {
        val calls = SENSITIVE_CALLS[owner] ?: return null
        return calls[method]
    }
}
