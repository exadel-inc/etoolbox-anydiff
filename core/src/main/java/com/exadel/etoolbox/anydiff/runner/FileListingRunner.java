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

import com.exadel.etoolbox.anydiff.util.ContentUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extends {@link StringListRunner} to implement extracting data that is specified in listing files. An entry of the
 * listing can be a file, a directory, a URL, etc.
 */
@Slf4j
class FileListingRunner extends StringListRunner {

    public FileListingRunner(Path left, Path right) {
        super(getLines(left), getLines(right));
    }

    private static String[] getLines(Path value) {
        log.debug("Reading listing file {}", value);
        List<String> result = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(value, StandardCharsets.UTF_8);
            log.debug("Retrieved {} lines from listing file {}", lines.size(), value);
            for (Object linesEntry : lines) {
                String line = linesEntry.toString();
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                if (ContentUtil.isPathLike(line)) {
                    result.add(value.toAbsolutePath().getParent().resolve(line).toString());
                } else {
                    result.add(line.trim());
                }
            }
        } catch (IOException e) {
            log.error("Error reading listing file {}", value, e);
        }
        return result.toArray(new String[0]);
    }
}
