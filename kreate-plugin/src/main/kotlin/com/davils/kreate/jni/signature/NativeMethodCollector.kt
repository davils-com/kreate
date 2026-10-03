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

package com.davils.kreate.jni.signature

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

private const val SKIPPED_CLASS_PARTS: Int = ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES

internal class NativeMethodCollector : ClassVisitor(Opcodes.ASM9) {
    private var ownerInternalName: String = ""

    private val collected: MutableList<NativeMethod> = mutableListOf()

    val methods: List<NativeMethod>
        get() = collected.toList()

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        ownerInternalName = name
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        val isNative = access and Opcodes.ACC_NATIVE != 0
        if (!isNative) return null

        collected += NativeMethod(
            ownerInternalName = ownerInternalName,
            name = name,
            descriptor = descriptor,
            isStatic = access and Opcodes.ACC_STATIC != 0
        )
        return null
    }
}

internal fun readNativeMethods(bytecode: ByteArray): List<NativeMethod> {
    val collector = NativeMethodCollector()
    ClassReader(bytecode).accept(collector, SKIPPED_CLASS_PARTS)
    return collector.methods
}
