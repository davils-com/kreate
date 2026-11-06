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

package com.davils.kreate.net

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

private const val PASSWORD: String = "loopback"

private const val ALIAS: String = "loopback"

private const val KEY_STORE_TYPE: String = "PKCS12"

object LoopbackCertificate {
    private val directory: Path by lazy { generate() }

    val pemFile: Path
        get() = directory.resolve("loopback.pem")

    val serverContext: SSLContext by lazy { loadServerContext() }

    private fun generate(): Path {
        val target = Files.createTempDirectory("kreate-loopback-tls")
        val keyStore = target.resolve("loopback.p12").toString()
        keytool(
            "-genkeypair", "-keyalg", "EC", "-groupname", "secp256r1", "-alias", ALIAS, "-dname", "CN=localhost",
            "-ext", "SAN=ip:127.0.0.1,dns:localhost", "-validity", "2", "-storetype", KEY_STORE_TYPE,
            "-keystore", keyStore, "-storepass", PASSWORD, "-keypass", PASSWORD
        )
        keytool(
            "-exportcert", "-rfc", "-alias", ALIAS, "-keystore", keyStore, "-storepass", PASSWORD,
            "-file", target.resolve("loopback.pem").toString()
        )
        return target
    }

    private fun keytool(vararg arguments: String) {
        val executable = File(System.getProperty("java.home"), "bin/keytool").path
        val builder = ProcessBuilder(listOf(executable) + arguments)
        builder.redirectErrorStream(true)
        val process = builder.start()
        val output = process.inputStream.readAllBytes()
        check(process.waitFor() == 0) { "keytool failed: ${output.toString(Charsets.UTF_8)}" }
    }

    private fun loadServerContext(): SSLContext {
        val password = PASSWORD.toCharArray()
        val keyStore = KeyStore.getInstance(KEY_STORE_TYPE)
        Files.newInputStream(directory.resolve("loopback.p12")).use { stream -> keyStore.load(stream, password) }

        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagers.init(keyStore, password)

        val context = SSLContext.getInstance("TLS")
        context.init(keyManagers.keyManagers, null, null)
        return context
    }
}
