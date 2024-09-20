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
package com.exadel.etoolbox.anydiff.comparison.preprocessor;

import com.exadel.etoolbox.anydiff.Constants;
import com.exadel.etoolbox.anydiff.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pre-processes the Java manifest file content that needs to be compared
 */
@Slf4j
class ManifestPreprocessor extends Preprocessor {

    private static final String INDENT = StringUtils.repeat(StringUtils.SPACE, Constants.DEFAULT_INDENT);

    @Override
    public String apply(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        LineProcessor processor = new LineProcessor();
        try (Reader reader = new StringReader(value)) {
            IOUtils.readLines(reader).forEach(processor::consume);
        } catch (IOException e) {
            log.error("Error reading manifest content", e);
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : processor.getLines().entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            builder.append(key).append(Constants.COLON);
            if (values.size() == 1) {
                builder.append(StringUtils.SPACE).append(values.get(0)).append(StringUtils.LF);
            } else {
                builder.append(StringUtils.LF);
                values.forEach(v -> builder.append(INDENT).append(v).append(Constants.COMMA).append(StringUtils.LF));
            }
        }
        return builder.toString();
    }

    /**
     * Extracts the sequence of manifest lines organized by section
     */
    private static class LineProcessor {

        private static final List<String> ORDERED_SECTIONS = Arrays.asList(
            "Manifest-Version",
            "Created-By",
            "Build-Jdk-Spec",
            "Bundle-Category",
            "Bundle-Name",
            "Bundle-SymbolicName",
            "Bundle-Version",
            "Bundle-ManifestVersion",
            "Bundle-Description");

        private final List<String> lines = new ArrayList<>();
        private StringBuilder lineBuilder;

        /**
         * Consumes another line of manifest content
         * @param value String value; a non-null, non-empty string is expected
         */
        void consume(String value) {
            String stripped = StringUtils.strip(value, StringUtils.CR);
            if (stripped.startsWith(StringUtils.SPACE)) {
                lineBuilder.append(stripped.substring(1));
            } else {
                if (lineBuilder != null && lineBuilder.length() > 0) {
                    lines.add(lineBuilder.toString());
                }
                lineBuilder = new StringBuilder(stripped);
            }
        }

        /**
         * Gets the manifest lines organized by section
         * @return A non-null map where keys are section names and values are lists of lines
         */
        Map<String, List<String>> getLines() {
            if (lineBuilder != null && lineBuilder.length() > 0) {
                lines.add(lineBuilder.toString());
            }
            Map<String, List<String>> result = new TreeMap<>(LineProcessor::sort);
            for (String line : lines) {
                if (!line.contains(Constants.COLON)) {
                    continue;
                }
                String key = StringUtils.substringBefore(line, Constants.COLON);
                String value = StringUtils.substringAfter(line, Constants.COLON).trim();
                if (value.contains(Constants.COMMA)) {
                    List<String> valueList = StringUtil.splitByUnescaped(value, Constants.COMMA_CHAR, Constants.QUOTE_CHAR);
                    valueList.sort(String::compareTo);
                    result.put(key, valueList);
                } else {
                    result.put(key, Collections.singletonList(value));
                }
            }
            return result;
        }

        private static int sort(String first, String second) {
            int firstIndex = ORDERED_SECTIONS.indexOf(first);
            int secondIndex = ORDERED_SECTIONS.indexOf(second);
            if (firstIndex >= 0 && secondIndex >= 0) {
                return Integer.compare(firstIndex, secondIndex);
            } else if (firstIndex >= 0) {
                return -1;
            } else if (secondIndex >= 0) {
                return 1;
            }
            return first.compareTo(second);
        }
    }
}
