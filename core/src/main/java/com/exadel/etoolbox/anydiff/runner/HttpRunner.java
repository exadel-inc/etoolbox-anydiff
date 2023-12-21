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
import com.exadel.etoolbox.anydiff.ContentType;
import com.exadel.etoolbox.anydiff.comparison.DiffTask;
import com.exadel.etoolbox.anydiff.diff.Diff;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Extends {@link DiffRunner} to implement extracting data from external endpoints available over HTTP connections
 */
@RequiredArgsConstructor
@Slf4j
class HttpRunner extends DiffRunner {

    private static final String PROPERTY_TRUST_SSL = "@nosslcheck";
    private final String leftUri;
    private final String rightUri;

    @Override
    public List<Diff> runInternal() {
        Pair<HttpResult, HttpResult> httpResults = getHttpResults(leftUri, rightUri);
        ContentType contentType = getCommonTypeOrDefault(httpResults.getLeft(), httpResults.getRight(), getContentType());
        Object leftContent = contentType != ContentType.UNDEFINED
            ? httpResults.getLeft().getContent()
            : httpResults.getLeft().getMetadata();
        Object rightContent = contentType != ContentType.UNDEFINED
            ? httpResults.getRight().getContent()
            : httpResults.getRight().getMetadata();
        DiffTask diffTask = DiffTask
                .builder()
                .contentType(contentType)
                .leftId(leftUri)
                .leftLabel(getLeftLabel())
                .leftContent(leftContent)
                .rightId(rightUri)
                .rightLabel(getRightLabel())
                .rightContent(rightContent)
                .filter(getEntryFilter())
                .taskParameters(getTaskParameters())
                .build();
        Diff diff = diffTask.run();
        return Collections.singletonList(diff);
    }

    private static ContentType getCommonTypeOrDefault(HttpResult left, HttpResult right, ContentType defaultType) {
        String leftContentType = left.getContentType();
        String rightContentType = right.getContentType();
        if (!StringUtils.equals(leftContentType, rightContentType)) {
            return defaultType;
        }
        ContentType result = ContentType.fromMimeType(leftContentType);
        if (result != ContentType.UNDEFINED) {
            return result;
        }
        String leftExtension = StringUtils.substringAfterLast(left.getUri().getPath(), Constants.DOT);
        String rightExtension = StringUtils.substringAfterLast(right.getUri().getPath(), Constants.DOT);
        if (!StringUtils.equals(leftExtension, rightExtension) || StringUtils.isEmpty(leftExtension)) {
            return defaultType;
        }
        result = ContentType.fromExtension(leftExtension);
        return result != ContentType.UNDEFINED ? result : defaultType;
    }

    private static Pair<HttpResult, HttpResult> getHttpResults(String leftUri, String rightUri) {
        CompletableFuture<HttpResult> leftPromise = getFutureHttpResult(leftUri);
        CompletableFuture<HttpResult> rightPromise = getFutureHttpResult(rightUri);
        CompletableFuture.allOf(leftPromise, rightPromise).join();
        HttpResult leftContent = null;
        HttpResult rightContent = null;
        try {
            leftContent = leftPromise.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting content from {}", leftUri, e);
        }
        try {
            rightContent = rightPromise.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting content from {}", leftUri, e);
        }
        return Pair.of(leftContent, rightContent);
    }

    private static CompletableFuture<HttpResult> getFutureHttpResult(String uri) {
        URIBuilder uriBuilder = getUriBuilder(uri);
        Map<String, String> requestOptions = getRequestOptions(uriBuilder);
        HttpClient client = getHttpClient(requestOptions);
        requestOptions.remove(PROPERTY_TRUST_SSL.substring(1));
        return CompletableFuture.supplyAsync(() -> getHttpResult(client, uriBuilder, requestOptions));
    }

    private static URIBuilder getUriBuilder(String uri) {
        try {
            return new URIBuilder(uri);
        } catch (URISyntaxException e) {
            log.error("Invalid URI {}", uri, e);
        }
        return null;
    }

    private static Map<String, String> getRequestOptions(URIBuilder uriBuilder) {
        if (uriBuilder == null) {
            return Collections.emptyMap();
        }
        return uriBuilder
                .getQueryParams()
                .stream()
                .filter(param -> param.getName().startsWith(Constants.AT))
                .collect(Collectors.toMap(
                        param -> param.getName().substring(1),
                        param -> StringUtils.defaultString(param.getValue()),
                        (a, b) -> b,
                        HashMap::new));
    }

    private static HttpClient getHttpClient(Map<String, String> requestOptions) {
        boolean trustSsl = requestOptions.keySet().stream().anyMatch(key -> StringUtils.endsWithIgnoreCase(PROPERTY_TRUST_SSL, key));
        return HttpClientFactory.newClient(trustSsl);
    }

    private static HttpResult getHttpResult(HttpClient client, URIBuilder uriBuilder, Map<String, String> requestOptions) {
        if (uriBuilder == null) {
            return new HttpResult(null);
        }
        String userInfo = uriBuilder.getUserInfo();
        uriBuilder.setParameters(uriBuilder
                .getQueryParams()
                .stream()
                .filter(param -> !param.getName().startsWith(Constants.AT))
                .collect(Collectors.toList()));
        uriBuilder.setUserInfo(StringUtils.EMPTY);

        log.info("Downloading from {}", uriBuilder);

        StopWatch stopWatch = StopWatch.createStarted();
        ClassicHttpRequest request = getRequest(uriBuilder.toString(), userInfo, requestOptions);
        HttpResponse response = null;
        HttpEntity entity = null;
        try {
            response = client.execute(request);
            stopWatch.stop();
            log.info("Download from {} completed in {} ms", uriBuilder, stopWatch.getTime(TimeUnit.MILLISECONDS));
            entity = response instanceof HttpEntityContainer ? ((HttpEntityContainer) response).getEntity() : null;
            return entity != null
                    ? new HttpResult(
                        uriBuilder,
                        entity.getContentType(),
                        IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8))
                    : null;
        } catch (IOException e) {
            stopWatch.stop();
            log.error(
                    "Download from {} failed after {} ms: {}",
                    uriBuilder,
                    stopWatch.getTime(TimeUnit.MILLISECONDS),
                    e.getMessage());
            return new HttpResult(uriBuilder);
        } finally {
            EntityUtils.consumeQuietly(entity);
            closeQuietly(response);
            closeQuietly(client);
        }
    }

    private static ClassicHttpRequest getRequest(String uri, String userInfo, Map<String, String> headers) {
        HttpGet request = new HttpGet(uri);
        if (StringUtils.isNotBlank(userInfo)) {
            request.setHeader(HttpHeaders.AUTHORIZATION, userInfo);
        }
        headers.forEach(request::setHeader);
        return request;
    }

    private static void closeQuietly(Object value) {
        if (!(value instanceof Closeable)) {
            return;
        }
        try {
            ((Closeable) value).close();
        } catch (IOException e) {
            log.error("Error closing HTTP entity", e);
        }
    }

    /**
     * Contains data retrieved from an HTTP endpoint
     */
    @AllArgsConstructor
    @RequiredArgsConstructor
    @Getter
    private static class HttpResult {
        private final URIBuilder uri;
        private String contentType;
        private String content;

        FileMetadata getMetadata() {
            return FileMetadata
                    .builder()
                    .path(uri.toString())
                    .size(StringUtils.length(content))
                    .build();
        }
    }
}
