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

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

private const val TEST_CLASS_VERSION: Int = Opcodes.V17

internal fun classCalling(owner: String, method: String, constant: String? = null): ByteArray {
    val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
    writer.visit(TEST_CLASS_VERSION, Opcodes.ACC_PUBLIC, "Sample", null, "java/lang/Object", null)
    val code = writer.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "run", "()V", null, null)
    code.visitCode()
    constant?.let { value ->
        code.visitLdcInsn(value)
        code.visitInsn(Opcodes.POP)
    }
    code.visitInsn(Opcodes.ACONST_NULL)
    code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, method, "()V", false)
    code.visitInsn(Opcodes.RETURN)
    code.visitMaxs(0, 0)
    code.visitEnd()
    writer.visitEnd()
    return writer.toByteArray()
}
