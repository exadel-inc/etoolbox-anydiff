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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Extends {@link DiffRunner} to implement extracting data from file system files
 */
@RequiredArgsConstructor
@Slf4j
class FileRunner extends DiffRunner {

    private final Path left;
    private final Path right;

    @Override
    public List<Diff> runInternal() {
        ContentType contentType = getCommonTypeOrDefault(
                left.toAbsolutePath().toString(),
                right.toAbsolutePath().toString(),
                getContentType());
        Object leftContent = contentType != ContentType.UNDEFINED ? getContent(left) : getMetadata(left);
        Object rightContent = contentType != ContentType.UNDEFINED ? getContent(right) : getMetadata(right);
        DiffTask diffTask = DiffTask
                .builder()
                .contentType(contentType)
                .leftId(left.toAbsolutePath().toString())
                .leftLabel(getLeftLabel())
                .leftContent(leftContent)
                .rightId(right.toAbsolutePath().toString())
                .rightLabel(getRightLabel())
                .rightContent(rightContent)
                .filter(getEntryFilter())
                .taskParameters(getTaskParameters())
                .build();
        Diff diff = diffTask.run();
        return Collections.singletonList(diff);
    }

    private static ContentType getCommonTypeOrDefault(String left, String right, ContentType defaultType) {
        ContentType commonContentTypeByMime = getCommonTypeByMime(left, right);
        if (commonContentTypeByMime != ContentType.UNDEFINED) {
            return commonContentTypeByMime;
        }
        String leftExtension = StringUtils.substringAfterLast(left, Constants.DOT);
        String rightExtension = StringUtils.substringAfterLast(right, Constants.DOT);
        if (!StringUtils.equals(leftExtension, rightExtension)) {
            return defaultType;
        }
        ContentType result = ContentType.fromExtension(leftExtension);
        return result != ContentType.UNDEFINED ? result : defaultType;
    }

    private static ContentType getCommonTypeByMime(String left, String right) {
        try {
            String leftContentType = Files.probeContentType(Paths.get(left));
            String rightContentType = Files.probeContentType(Paths.get(right));
            if (!StringUtils.equals(leftContentType, rightContentType)) {
                return ContentType.UNDEFINED;
            }
            return ContentType.fromMimeType(leftContentType);
        } catch (IOException e) {
            log.error("Error detecting a content type", e);
        }
        return ContentType.UNDEFINED;
    }

    private static String getContent(Path value) {
        log.debug("Reading content file {}", value);
        try (InputStream inputStream = Files.newInputStream(value)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading file {}", value, e);
        }
        return null;
    }

    private static FileMetadata getMetadata(Path value) {
        long size = -1;
        long lastModified = 0;
        try {
            size = Files.size(value);
            lastModified = Files.getLastModifiedTime(value).toMillis();
        } catch (IOException e) {
            log.error("Error reading file {}", value, e);
        }
        long crc = FileMetadata.computeCrc(value, size);
        return FileMetadata
                .builder()
                .path(value.toAbsolutePath().toString())
                .size(size)
                .crc(crc)
                .lastModified(new Date(lastModified))
                .build();
    }
}
