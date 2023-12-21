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

import com.exadel.etoolbox.anydiff.diff.Diff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Extends {@link DiffRunner} to implement extracting data from file system directories
 */
@RequiredArgsConstructor
@Slf4j
class DirectoryRunner extends DiffRunner {

    private final Path left;
    private final Path right;

    @Override
    public List<Diff> runInternal() {
        List<String> leftPaths = getRelativePaths(left);
        log.debug("Retrieved {} entries from directory {}", leftPaths.size(), left);
        List<String> rightPaths = getRelativePaths(right);
        log.debug("Retrieved {} entries from directory {}", rightPaths.size(), right);

        MissingEntriesHelper missingHelper = new MissingEntriesHelper(
                leftPaths,
                rightPaths,
                path -> FileMetadata.computeCrc(left.resolve(path)),
                path -> FileMetadata.computeCrc(right.resolve(path)));
        List<Diff> result = new ArrayList<>();

        for (String leftPath : leftPaths) {
            if (missingHelper.isMissingRight(leftPath)) {
                Diff miss = reportRightMissing(
                        left.resolve(leftPath).toAbsolutePath().toString(),
                        right.resolve(leftPath).toAbsolutePath().toString());
                result.add(miss);
                continue;
            }
            String movedPath = missingHelper.getMoved(leftPath);
            if (movedPath != null) {
                Diff moved = reportMoved(
                        left.resolve(leftPath).toAbsolutePath().toString(),
                        new FileMoveInfo(right.resolve(movedPath).toAbsolutePath().toString()));
                result.add(moved);
                continue;
            }
            DiffRunner diffRunner = forValues(
                            left.resolve(leftPath),
                            getLeftLabel(),
                            right.resolve(leftPath),
                            getRightLabel())
                    .withContentType(getContentType())
                    .withEntryFilter(getEntryFilter())
                    .withTaskParameters(getTaskParameters());
            result.addAll(diffRunner.runInternal());
        }

        for (String rightPath : rightPaths) {
            if (missingHelper.isMissingLeft(rightPath)) {
                Diff miss = reportLeftMissing(
                        left.resolve(rightPath).toAbsolutePath().toString(),
                        right.resolve(rightPath).toAbsolutePath().toString());
                result.add(miss);
            }
        }
        return result;
    }

    private static List<String> getRelativePaths(Path value) {
        log.debug("Reading directory {}", value);

        List<String> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(value)) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .map(path -> StringUtils.removeStart(path, value.toString()))
                    .map(path -> StringUtils.stripStart(path, "\\/"))
                    .forEach(result::add);
        } catch (IOException e) {
            log.error("Error reading directory {}", value, e);
        }
        return result;
    }
}
