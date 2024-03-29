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

import com.exadel.etoolbox.anydiff.Constants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Contains utility methods for extracting comparing-related data from text strings
 * <u>Note</u>: This class is not a part of public API and is subject to change. You should not use it directly
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentUtil {

    private static final String SCHEMA_SEPARATOR = "://";
    private static final int LABEL_MAX_WIDTH = 120;

    /* ----------
       Extraction
       ---------- */

    /**
     * Extracts the part of the string that is directly used for comparison. Skips the label if one is prepended to the
     * string
     * @param value Source string
     * @return String value without the optional label
     */
    public static String extractComparable(String value) {
        if (StringUtils.isEmpty(value)) {
            return StringUtils.EMPTY;
        }
        if (value.indexOf(Constants.BRACKET_OPEN) == 0) {
            int posOfClosingBracket = value.indexOf(Constants.BRACKET_CLOSE);
            if (posOfClosingBracket > 1 && posOfClosingBracket < value.length() - 1) {
                return value.substring(posOfClosingBracket + 1);
            }
        }
        if (value.startsWith("\\[") || value.startsWith("\\\\[")) {
            // This way we provide the possibility to compare arbitrary strings, either starting with {@code [}
            // By escaping the leading {@code [}, we mean this is not a label. We can also escape {@code \[} itself
            return value.substring(1);
        }
        return value;
    }

    /**
     * Extracts the optional label from the given string array
     * @param value Source strings
     * @return The label. Can be an empty string
     */
    public static String extractLabel(String[] value) {
        if (ArrayUtils.isEmpty(value)) {
            return StringUtils.EMPTY;
        }
        if (value.length == 1) {
            return extractLabel(value[0]);
        }
        return Arrays.stream(value)
                .filter(str -> StringUtils.indexOf(str, Constants.BRACKET_OPEN) == 0
                        && StringUtils.indexOf(str, Constants.BRACKET_CLOSE) > 0)
                .map(ContentUtil::extractLabel)
                .findFirst()
                .orElse(StringUtils.EMPTY);
    }

    /**
     * Extracts the optional label from the given string
     * @param value Source string
     * @return The label. Can be an empty string
     */
    public static String extractLabel(String value) {
        return extractLabel(value, null);
    }

    /**
     * Extracts the optional label from the given string
     * @param value Source string
     * @param fallback The value to return if the label is missing
     * @return The label. If missing from the source string, the {@code fallback} value is returned
     */
    public static String extractLabel(String value, String fallback) {
        if (StringUtils.isEmpty(value)) {
            return StringUtils.EMPTY;
        }
        if (value.indexOf(Constants.BRACKET_OPEN) == 0) {
            int posOfClosingBracket = value.indexOf(Constants.BRACKET_CLOSE);
            if (posOfClosingBracket > 1 && posOfClosingBracket < value.length() - 1) {
                return value.substring(1, posOfClosingBracket);
            }
        }
        return StringUtils.isNotBlank(fallback) ? fallback : truncateLabel(extractComparable(value).trim());
    }

    private static String truncateLabel(String value) {
        if (ContentUtil.isPathLike(value)) {
            return value.contains(SCHEMA_SEPARATOR)
                    ? cleanUpUrl(value)
                    : value;
        }
        return StringUtils.abbreviate(value.replaceAll("\\s+", StringUtils.SPACE), LABEL_MAX_WIDTH);
    }

    private static String cleanUpUrl(String value) {
        try {
            URIBuilder uriBuilder = new URIBuilder(value);
            uriBuilder.setUserInfo(StringUtils.EMPTY);
            uriBuilder.setScheme(StringUtils.EMPTY);
            uriBuilder.setParameters(uriBuilder
                    .getQueryParams()
                    .stream()
                    .filter(param -> !param.getName().startsWith(Constants.AT))
                    .collect(Collectors.toList()));
            return StringUtils.strip(uriBuilder.toString(), " /");
        } catch (URISyntaxException e) {
            return truncateLabel(StringUtils.substringAfter(value, SCHEMA_SEPARATOR));
        }
    }

    /* --------------------
       URI / Path detection
       -------------------- */

    /**
     * Gets whether the given string can represent a file system path or a URL
     * @param value Source string
     * @return True or false
     */
    public static boolean isPathLike(String value) {
        return isPathLike(value, true);
    }

    /**
     * Gets whether the given string can represent a file system path or a URL
     * @param value             Source string
     * @param allowQuestionMark Whether to allow the {@code ?} character in the string
     * @return True or false
     */
    public static boolean isPathLike(String value, boolean allowQuestionMark) {
        if (StringUtils.isEmpty(value)
                || StringUtils.containsAny(value, '\r', '\n', Constants.TAG_OPEN_CHAR, Constants.TAG_CLOSE_CHAR, '"',
                '|', '*')
                || (!allowQuestionMark && StringUtils.contains(value, '?'))
        ) {
            return false;
        }
        try {
            new URI(value.replace('\\', Constants.SLASH_CHAR));
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

}
