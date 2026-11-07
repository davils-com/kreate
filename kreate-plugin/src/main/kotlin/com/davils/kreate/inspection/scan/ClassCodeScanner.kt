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

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal object ClassCodeScanner {
    fun scan(bytes: ByteArray, path: String, hits: MutableCollection<Hit>) {
        try {
            ClassReader(bytes).accept(Visitor(path, hits), ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        } catch (_: IllegalArgumentException) {
            hits += Hit("MALFORMED_CLASS", path)
        } catch (_: ArrayIndexOutOfBoundsException) {
            hits += Hit("MALFORMED_CLASS", path)
        }
    }

    private class Visitor(
        private val path: String,
        private val hits: MutableCollection<Hit>
    ) : ClassVisitor(Opcodes.ASM9) {
        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor = Calls(path, hits)
    }

    private class Calls(
        private val path: String,
        private val hits: MutableCollection<Hit>
    ) : MethodVisitor(Opcodes.ASM9) {
        override fun visitMethodInsn(
            opcode: Int,
            owner: String,
            name: String,
            descriptor: String?,
            isInterface: Boolean
        ) {
            val rule = SensitiveApis.ruleFor(owner, name) ?: return
            hits += Hit(rule, path)
        }

        override fun visitLdcInsn(value: Any?) {
            val text = value as? String ?: return
            TextPatterns.scan(text, path, hits)
            if (StringEntropy.isSuspicious(text)) hits += Hit("HIGH_ENTROPY_STRING", path)
        }
    }
}
