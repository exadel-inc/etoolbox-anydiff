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
import com.exadel.etoolbox.anydiff.util.RichUri;
import com.exadel.etoolbox.anydiff.util.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Extends {@link DiffRunner} to implement extracting data from external endpoints available over HTTP connections
 */
@Slf4j
class HttpRunner extends DiffRunner {

    private static final String PROPERTY_HEADERS = "headers";
    private static final String PROPERTY_PROXY = "proxy";
    private static final String PROPERTY_TRUST_SSL = "nosslcheck";

    private static final String CIRCUMFLEX = "^";
    private static final String CMD_KEY_HEADER = "-H";

    private final RichUri leftUri;
    private final RichUri rightUri;

    public HttpRunner(String leftUri, String rightUri) {
        this.leftUri = createRichUri(leftUri);
        this.rightUri = createRichUri(rightUri);
    }

    @Override
    public List<Diff> runInternal() {
        if (leftUri == null || rightUri == null) {
            return Collections.emptyList();
        }
        Pair<HttpResult, HttpResult> httpResults = getHttpResults(leftUri, rightUri, getTaskParameters().handleErrorPages());
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
                .leftId(leftUri.getUri().toString())
                .leftLabel(getLeftLabel())
                .leftContent(leftContent)
                .rightId(rightUri.getUri().toString())
                .rightLabel(getRightLabel())
                .rightContent(rightContent)
                .filter(getEntryFilter())
                .taskParameters(getTaskParameters())
                .build();
        Diff diff = diffTask.run();
        return Collections.singletonList(diff);
    }

    /* ---------------------
       Request prerequisites
       --------------------- */

    private static RichUri createRichUri(String uri) {
        try {
            return new RichUri(uri);
        } catch (URISyntaxException | IOException e) {
            log.error("Invalid URI {}", uri, e);
        }
        return null;
    }

    private static ContentType getCommonTypeOrDefault(HttpResult left, HttpResult right, ContentType defaultType) {
        String leftContentType = left.getContentType();
        String rightContentType = right.getContentType();
        if (!StringUtils.equals(leftContentType, rightContentType)) {
            return defaultType;
        }
        ContentType byMimeType = ContentType.fromMimeType(leftContentType);
        if (byMimeType != ContentType.UNDEFINED) {
            return byMimeType;
        }
        String leftName = StringUtils.substringAfterLast(left.getPath(), Constants.SLASH);
        String rightName = StringUtils.substringAfterLast(right.getPath(), Constants.SLASH);
        if (StringUtils.isAnyEmpty(leftName, rightName)) {
            return defaultType;
        }
        ContentType byLeftName = ContentType.fromFileName(leftName);
        ContentType byRightName = ContentType.fromFileName(rightName);
        if (byLeftName != byRightName) {
            return defaultType;
        }
        return byLeftName != ContentType.UNDEFINED ? byLeftName : defaultType;
    }

    /* ------------------
       Request processing
       ------------------ */

    private static Pair<HttpResult, HttpResult> getHttpResults(RichUri leftUri, RichUri rightUri, boolean handleErrorPages) {
        CompletableFuture<HttpResult> leftPromise = getHttpResultAsync(leftUri, handleErrorPages);
        CompletableFuture<HttpResult> rightPromise = getHttpResultAsync(rightUri, handleErrorPages);
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

    private static CompletableFuture<HttpResult> getHttpResultAsync(RichUri uri, boolean handleErrorPages) {
        if (uri == null) {
            return CompletableFuture.completedFuture(new HttpResult(null));
        }
        OkHttpClient client = createHttpClient(uri.getOptions());
        return CompletableFuture.supplyAsync(() -> getHttpResult(uri, client, handleErrorPages));
    }

    private static HttpResult getHttpResult(RichUri uri, OkHttpClient client, boolean handleErrorPages) {
        log.info("Downloading from {}", uri);

        StopWatch stopWatch = StopWatch.createStarted();
        Request request = createHttpRequest(uri);
        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            stopWatch.stop();
            log.info("Download from {} completed in {} ms", uri, stopWatch.getTime(TimeUnit.MILLISECONDS));
            if (!response.isSuccessful() && !handleErrorPages) {
                return new HttpResult(uri);
            }
            return response.body() != null
                ? new HttpResult(uri, response.body().contentType(), response.body().string())
                : new HttpResult(uri);
        } catch (IOException e) {
            stopWatch.stop();
            log.error(
                "Download from {} failed after {} ms: {}",
                uri,
                stopWatch.getTime(TimeUnit.MILLISECONDS),
                e.getMessage());
            return new HttpResult(uri);
        } finally {
            closeQuietly(client);
        }
    }

    private static OkHttpClient createHttpClient(Map<String, String> requestOptions) {
        boolean trustSsl = false;
        String proxy = null;
        if (MapUtils.isNotEmpty(requestOptions)) {
            trustSsl = !Boolean.FALSE.toString().equals(requestOptions.get(PROPERTY_TRUST_SSL));
            proxy = requestOptions.get(PROPERTY_PROXY);
        }
        return HttpClientFactory.getInstance().newClient(trustSsl, proxy);
    }

    private static Request createHttpRequest(RichUri uri) {
        String uriString = uri.toString();
        Request.Builder requestBuilder = new Request.Builder().url(uriString);
        if (StringUtils.isNotBlank(uri.getUserInfo())) {
            requestBuilder.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(uri.getUserInfo().getBytes()));
        }
        if (MapUtils.isNotEmpty(uri.getOptions())) {
            populateHeadersFromFile(uri, requestBuilder);
            uri.getOptions().entrySet()
                .stream()
                .filter(entry -> !StringUtils.equalsAny(entry.getKey(), PROPERTY_TRUST_SSL, PROPERTY_HEADERS))
                .forEach(entry -> requestBuilder.addHeader(entry.getKey(), entry.getValue()));
        }
        return requestBuilder.build();
    }

    private static void populateHeadersFromFile(RichUri uri, Request.Builder requestBuilder) {
        String headersFile = uri.getOptions().get(PROPERTY_HEADERS);
        if (StringUtils.isBlank(headersFile)) {
            return;
        }
        String headersContent;
        try {
            headersContent = IOUtils.toString(Files.newInputStream(Paths.get(headersFile)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading headers from {}", headersFile, e);
            return;
        }
        boolean isCurlFile = headersContent.startsWith("curl ");
        StringUtil.splitByNewline(headersContent)
            .stream()
            .filter(StringUtils::isNotBlank)
            .map(line -> StringUtils.strip(line, " \\^"))
            .filter(line -> !isCurlFile || line.startsWith(CMD_KEY_HEADER))
            .map(line -> isCurlFile
                ? StringUtils.substringAfter(line, CMD_KEY_HEADER).trim()
                : line)
            .map(line -> line.startsWith(CIRCUMFLEX) ? line.replace(CIRCUMFLEX, StringUtils.EMPTY) : line)
            .map(line -> StringUtils.strip(line, " '\""))
            .filter(line -> StringUtils.contains(line, Constants.COLON))
            .forEach(line -> requestBuilder.addHeader(
                StringUtils.substringBefore(line, Constants.COLON).trim(),
                StringUtils.substringAfter(line, Constants.COLON).trim()));
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

    /* ------------------
       Subsidiary classes
       ------------------ */

    /**
     * Contains data retrieved from an HTTP endpoint
     */
    @AllArgsConstructor
    @RequiredArgsConstructor
    private static class HttpResult {

        private final RichUri richUri;

        private MediaType contentType;

        @Getter
        private String content;

        String getContentType() {
            return contentType != null ? contentType.toString() : StringUtils.EMPTY;
        }

        FileMetadata getMetadata() {
            return FileMetadata
                    .builder()
                    .path(richUri.toString())
                    .size(StringUtils.length(content))
                    .build();
        }

        String getPath() {
            return richUri.getUri().getPath();
        }
    }
}
