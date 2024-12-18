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

import com.exadel.etoolbox.anydiff.Constants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okio.GzipSource;
import okio.Okio;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Contains utility methods for creating HTTP client instances
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
class HttpClientFactory {

    private static final int HTTP_TIMEOUT = 30000;

    private static final TrustManager[] TRUST_MANAGERS = prepareTrustManagers();
    private static final SSLSocketFactory SSL_SOCKET_FACTORY = prepareSSLSocketFactory(TRUST_MANAGERS);
    private static final HostnameVerifier HOSTNAME_VERIFIER = (hostname, session) -> true;

    private static final String SCHEMA_SOCKS = "socks";

    private static final HttpClientFactory INSTANCE = new HttpClientFactory();

    private final List<OkHttpClient> clients = new CopyOnWriteArrayList<>();

    /**
     * Retrieves an instance of an HTTP client. A new instance is created if no existing instances are available
     * @param trustSsl {@code True} to bypass SSL certificate validation, {@code false} otherwise
     * @return A non-null {@code HttpClient} object
     */
    OkHttpClient newClient(boolean trustSsl, String proxy) {
        String proxyAddress = StringUtils.contains(proxy, Constants.SCHEMA_SEPARATOR)
            ? StringUtils.substringAfter(proxy, Constants.SCHEMA_SEPARATOR)
            : proxy;
        OkHttpClient existingClient = clients
            .stream()
            .filter(client -> client.sslSocketFactory().equals(SSL_SOCKET_FACTORY)
                && isMatchingProxy(client.proxy(), proxyAddress))
            .findFirst()
            .orElse(null);
        if (existingClient != null) {
            return existingClient;
        }
        OkHttpClient.Builder newClientBuilder = new OkHttpClient.Builder()
            .addInterceptor(new GzipInterceptor())
            .connectTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
            .hostnameVerifier(HOSTNAME_VERIFIER);
        if (trustSsl) {
            newClientBuilder.sslSocketFactory(SSL_SOCKET_FACTORY, (X509TrustManager) TRUST_MANAGERS[0]);
        }
        if (StringUtils.isNotEmpty(proxy)) {
            newClientBuilder.proxy(createProxyInstance(proxy));
        }
        OkHttpClient client = newClientBuilder.build();
        clients.add(client);
        return client;
    }

    private static boolean isMatchingProxy(Proxy proxy, String proxyAddress) {
        if (StringUtils.isEmpty(proxyAddress)) {
            return proxy == null || proxy.address() == null;
        }
        return proxy != null
            && proxy.address() != null
            && proxy.address().toString().endsWith(proxyAddress);
    }

    private static Proxy createProxyInstance(String source) {
        if (StringUtils.isEmpty(source)) {
            return null;
        }
        String[] parts = source.split(Constants.SCHEMA_SEPARATOR);
        Proxy.Type type = StringUtils.startsWith(parts[0], SCHEMA_SOCKS) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
        String addressAndPort = parts.length > 1 ? parts[1] : parts[0];
        String address = StringUtils.substringBefore(addressAndPort, Constants.COLON);
        int port = StringUtils.contains(addressAndPort, Constants.COLON)
            ? Integer.parseInt(StringUtils.substringAfter(addressAndPort, Constants.COLON))
            : 80;
        return new Proxy(type, new InetSocketAddress(address, port)
        );
    }

    /* ---------------
       Factory methods
       --------------- */

    static HttpClientFactory getInstance() {
        return INSTANCE;
    }

    private static TrustManager[] prepareTrustManagers() {
        return new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
        };
    }

    @SuppressWarnings("SameParameterValue")
    private static SSLSocketFactory prepareSSLSocketFactory(TrustManager[] trustManagers) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            return null;
        }
    }

    /* ------------------
       Subsidiary classes
       ------------------ */

    /**
     * Processes a {@code gzip}-ped response body
     */
    private static class GzipInterceptor implements Interceptor {

        @NotNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            if ("gzip".equals(response.header("Content-Encoding"))) {
                return unzip(response);
            }
            return response;
        }

        private static Response unzip(final Response response) {
            if (response.body() == null) {
                return response;
            }
            MediaType contentType = response.body().contentType();
            String contentTypeString = contentType == null ? "text/html" : contentType.toString();
            long contentLength = response.body().contentLength();
            GzipSource gzipSource = new GzipSource(response.body().source());
            Headers strippedHeaders = response.headers().newBuilder()
                .removeAll("Content-Encoding")
                .removeAll("Content-Length")
                .build();

            RealResponseBody responseBody = new RealResponseBody(contentTypeString, contentLength, Okio.buffer(gzipSource));
            return response.newBuilder()
                .headers(strippedHeaders)
                .body(responseBody)
                .message(response.message())
                .build();

        }
    }
}
