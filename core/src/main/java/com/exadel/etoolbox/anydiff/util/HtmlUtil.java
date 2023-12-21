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
package com.exadel.etoolbox.anydiff.util;

import com.exadel.etoolbox.anydiff.AnyDiff;
import com.exadel.etoolbox.anydiff.OutputType;
import com.exadel.etoolbox.anydiff.diff.Diff;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains utility methods for generating HTML reports
 * <u>Note</u>: This class is not a part of public API and is subject to change. You should not use it directly
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HtmlUtil {

    /**
     * Generates an HTML report based on the provided list of differences
     * @param differences List of differences. An empty list will result in an empty string
     * @return String value representing the HTML report
     * @throws IOException If the report template or assets cannot be read
     */
    public static String toHtml(List<Diff> differences) throws IOException {
        if (CollectionUtils.isEmpty(differences)) {
            return StringUtils.EMPTY;
        }
        String template = readAsset("/html/report.html");
        String diffContent =
                differences.stream().map(diff -> diff.toString(OutputType.HTML)).collect(Collectors.joining());
        return template
                .replace("${bodyclass}", differences.size() <= 1 ? "no-toc" : StringUtils.EMPTY)
                .replace("${assets}", "<style>" + readAsset("/html/style.css") + "</style>")
                .replace("${diff}", diffContent)
                .replace("${toc}", getTocContent(differences))
                .replace("${toc-class}", differences.size() <= 1 ? "hidden" : StringUtils.EMPTY);
    }

    private static String getTocContent(List<Diff> differences) {
        StringBuilder builder = new StringBuilder();
        for (Diff diff : differences) {
            builder.append(diff.toString(OutputType.HTML, "toc"));
        }
        return builder.toString();
    }

    private static String readAsset(String path) throws IOException {
        try (InputStream input = AnyDiff.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Resource not found");
            }
            return IOUtils.toString(input, StandardCharsets.UTF_8);
        }
    }
}
