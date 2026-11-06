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

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private const val CERTIFICATE_TYPE: String = "X.509"

private const val TLS_PROTOCOL: String = "TLS"

internal object ExtraCertificateAuthority {
    fun sslContext(pemFile: Path): SSLContext =
        try {
            buildContext(pemFile)
        } catch (cause: IOException) {
            throw unusable(pemFile, cause)
        } catch (cause: GeneralSecurityException) {
            throw unusable(pemFile, cause)
        }

    private fun buildContext(pemFile: Path): SSLContext {
        val extraAuthorities = readCertificates(pemFile)
        if (extraAuthorities.isEmpty()) {
            throw NetworkException.Misconfigured("The CA certificate file $pemFile holds no certificate.")
        }

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        val authorities = defaultAuthorities() + extraAuthorities
        authorities.forEachIndexed { index, certificate ->
            keyStore.setCertificateEntry("authority-$index", certificate)
        }

        val trustManagers = trustManagerFactory(keyStore)
        val context = SSLContext.getInstance(TLS_PROTOCOL)
        context.init(null, trustManagers.trustManagers, null)
        return context
    }

    private fun readCertificates(pemFile: Path): List<Certificate> {
        val factory = CertificateFactory.getInstance(CERTIFICATE_TYPE)
        return Files.newInputStream(pemFile).use { stream -> factory.generateCertificates(stream).toList() }
    }

    private fun defaultAuthorities(): List<Certificate> {
        val factory = trustManagerFactory(keyStore = null)
        val trustManagers = factory.trustManagers.filterIsInstance<X509TrustManager>()
        return trustManagers.flatMap { manager -> manager.acceptedIssuers.asList() }
    }

    private fun trustManagerFactory(keyStore: KeyStore?): TrustManagerFactory {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(keyStore)
        return factory
    }

    private fun unusable(pemFile: Path, cause: Exception): NetworkException.Misconfigured =
        NetworkException.Misconfigured("The CA certificate file $pemFile could not be loaded.", cause)
}
