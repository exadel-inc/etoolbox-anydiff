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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.exadel.etoolbox.anydiff.Constants.SLASH;

/**
 * Extends {@link DiffRunner} to implement extracting data from archive files
 */
@RequiredArgsConstructor
@Slf4j
class ArchiveRunner extends DiffRunner {

    private final Path left;
    private final Path right;

    @Override
    public List<Diff> runInternal() {
        List<Diff> result = new ArrayList<>();

        log.debug("Reading archive {}", left);
        log.debug("Reading archive {}", right);
        try (ZipFile leftZip = new ZipFile(left.toFile()); ZipFile rightZip = new ZipFile(right.toFile())) {
            List<String> leftPaths = getEntryPaths(leftZip);
            log.debug("Retrieved {} entries from archive {}", leftPaths.size(), left);
            List<String> rightPaths = getEntryPaths(rightZip);
            log.debug("Retrieved {} entries from archive {}", rightPaths.size(), right);

            MissingEntriesHelper missingHelper = new MissingEntriesHelper(
                    leftPaths,
                    rightPaths,
                    path -> leftZip.getEntry(path).getCrc(),
                    path -> rightZip.getEntry(path).getCrc());

            for (String leftPath : leftPaths) {
                String completeLeftPath = left + SLASH + leftPath;
                String completeRightPath = right + SLASH + leftPath;
                ContentType contentType = getTypeOrDefault(completeLeftPath, getContentType());

                if (missingHelper.isMissingRight(leftPath)) {
                    Diff miss = reportRightMissing(completeLeftPath, completeRightPath);
                    result.add(miss);
                    continue;
                }
                String movedPath = missingHelper.getMoved(leftPath);
                if (movedPath != null) {
                    Diff moved = reportMoved(completeLeftPath, new FileMoveInfo(right + SLASH + movedPath));
                    result.add(moved);
                    continue;
                }

                Object leftContent = contentType != ContentType.UNDEFINED
                        ? getContent(leftZip, leftPath)
                        : getMetadata(leftZip, leftPath);
                Object rightContent = contentType != ContentType.UNDEFINED
                        ? getContent(rightZip, leftPath)
                        : getMetadata(rightZip, leftPath);
                DiffTask task = DiffTask
                        .builder()
                        .contentType(contentType)
                        .leftId(completeLeftPath)
                        .leftLabel(getLeftLabel())
                        .leftContent(leftContent)
                        .rightId(completeRightPath)
                        .rightLabel(getRightLabel())
                        .rightContent(rightContent)
                        .taskParameters(getTaskParameters())
                        .filter(getEntryFilter())
                        .build();
                Diff diff = task.run();
                result.add(diff);
            }
            for (String rightPath : rightPaths) {
                if (missingHelper.isMissingLeft(rightPath)) {
                    String completeLeftPath = left + SLASH + rightPath;
                    String completeRightPath = right + SLASH + rightPath;
                    Diff miss = reportRightMissing(completeLeftPath, completeRightPath);
                    result.add(miss);
                }
            }
        } catch (IOException e) {
            log.error("Error reading archive", e);
        }
        return result;
    }

    private static List<String> getEntryPaths(ZipFile file) {
        List<String> result = new ArrayList<>();
        Enumeration<? extends ZipEntry> entries = file.entries();
        for (ZipEntry entry : Collections.list(entries)) {
            if (!entry.isDirectory()) {
                result.add(entry.getName());
            }
        }
        return result;
    }

    private static ContentType getTypeOrDefault(String path, ContentType defaultType) {
        String extension = StringUtils.substringAfterLast(path, Constants.DOT);
        ContentType result = ContentType.fromExtension(extension);
        return result != ContentType.UNDEFINED ? result : defaultType;
    }

    private static String getContent(ZipFile file, String path) {
        try (InputStream input = file.getInputStream(file.getEntry(path))) {
            return IOUtils.toString(input, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading file {} from archive {}", path, file.getName(), e);
        }
        return null;
    }

    private static FileMetadata getMetadata(ZipFile file, String path) {
        ZipEntry entry = file.getEntry(path);
        return FileMetadata
                .builder()
                .path(path)
                .crc(entry.getCrc())
                .size(entry.getSize())
                .lastModified(new Date(entry.getLastModifiedTime().toMillis()))
                .build();
    }
}
