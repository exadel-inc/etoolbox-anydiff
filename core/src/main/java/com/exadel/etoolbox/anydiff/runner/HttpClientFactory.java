/*
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
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
package com.exadel.etoolbox.anydiff.runner;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * Contains utility methods for creating {@link HttpClient} objects
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Slf4j
class HttpClientFactory {

    private static final int HTTP_TIMEOUT = 30000;

    private static final Registry<ConnectionSocketFactory> SOCKET_FACTORIES;

    static {
        Registry<ConnectionSocketFactory> socketFactoryRegistry = null;
        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        try {
            SSLContext sslContext = SSLContexts
                    .custom()
                    .loadTrustMaterial(null, acceptingTrustStrategy)
                    .build();
            final SSLConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE);
            socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory> create()
                            .register("https", connectionSocketFactory)
                            .register("http", new PlainConnectionSocketFactory())
                            .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("Could not initialize the connection manager", e);
        }
        SOCKET_FACTORIES = socketFactoryRegistry;
    }

    /**
     * Creates a new {@link HttpClient} object
     * @param trustSsl {@code True} to bypass SSL certificate validation, {@code false} otherwise
     * @return A non-null {@code HttpClient} object
     */
    static HttpClient newClient(boolean trustSsl) {
        if (trustSsl) {
            return newClient(getTrustfulConnectionManager());
        }
        return newClient(null);
    }

    private static HttpClient newClient(HttpClientConnectionManager connectionManager) {
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setConnectionRequestTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                .setConnectTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                .setResponseTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder
                .create()
                .setDefaultRequestConfig(requestConfig)
                .setRedirectStrategy(new DefaultRedirectStrategy());
        if (connectionManager != null) {
            httpClientBuilder.setConnectionManager(connectionManager);
        }
        return httpClientBuilder.build();
    }

    private static HttpClientConnectionManager getTrustfulConnectionManager() {
        return SOCKET_FACTORIES != null ? new BasicHttpClientConnectionManager(SOCKET_FACTORIES) : null;
    }
}
