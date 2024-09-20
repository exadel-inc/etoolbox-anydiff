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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains utility methods for string manipulation
 * <u>Note</u>: This class is not a part of public API and is subject to change. You should not use it directly
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtil {

    /**
     * Escapes the specified string to be safely displayed in HTML
     * @param value String to escape
     * @return Escaped string
     */
    public static String escape(String value) {
        return StringUtils.defaultString(value)
                .replace(Constants.TAG_OPEN, "&lt;")
                .replace(Constants.TAG_CLOSE, "&gt;");
    }

    /**
     * Gets the number of leading spaces in the specified collection of strings as if they were placed in a single line
     * @param value Collection of strings to analyze
     * @return Number of leading spaces
     */
    public static int getIndent(List<? extends CharSequence> value) {
        if (CollectionUtils.isEmpty(value)) {
            return 0;
        }
        int result = 0;
        for (CharSequence entry : value) {
            int indent = getIndent(entry);
            result += indent;
            if (indent < entry.length()) {
                break;
            }
        }
        return result;
    }

    /**
     * Gets the number of leading spaces in the specified string
     * @param value String to analyze
     * @return Number of leading spaces
     */
    public static int getIndent(CharSequence value) {
        if (StringUtils.isEmpty(value)) {
            return 0;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return value.length();
    }

    /**
     * Gets the smallest number representing the position of one of the provided substrings in the given string
     * @param value      The string to analyze
     * @param candidates Substrings to search for
     * @return A {@link Pair} object containing the position of the selected substring and the substring itself
     */
    public static Pair<Integer, String> getNearestSubstring(CharSequence value, String... candidates) {
        int lastPosition = value.length();
        String lastCandidate = null;
        for (String candidate : candidates) {
            int position = StringUtils.indexOf(value, candidate);
            if (position >= 0 && position < lastPosition) {
                lastPosition = position;
                lastCandidate = candidate;
            }
        }
        return lastCandidate != null ? Pair.of(lastPosition, lastCandidate) : null;
    }

    /**
     * Removes all occurrences of the specified substrings from the given string
     * @param value   String to process
     * @param entries Substrings to remove
     * @return A string with all occurrences of the specified substrings removed
     */
    public static String removeAll(String value, String... entries) {
        if (StringUtils.isEmpty(value) || ArrayUtils.isEmpty(entries)) {
            return value;
        }
        Pair<Integer, String> substring = getNearestSubstring(value, entries);
        if (substring == null) {
            return value;
        }
        StringBuilder result = new StringBuilder(value);
        while (substring != null) {
            result.delete(substring.getLeft(), substring.getLeft() + substring.getRight().length());
            substring = getNearestSubstring(result, entries);
        }
        return result.toString();
    }

    /**
     * Splits the specified string into chunks not exceeding the given length
     * @param value  The string to split
     * @param length The maximum length of each chunk
     * @return A non-null list of chunks
     */
    public static List<String> splitByLength(CharSequence value, int length) {
        if (length <= 0) {
            return Collections.emptyList();
        }
        if (StringUtils.length(value) <= length) {
            return Collections.singletonList(String.valueOf(value));
        }
        List<String> result = new ArrayList<>(value.length() / length + 1);
        for (int i = 0; i < value.length(); i += length) {
            result.add(value.subSequence(i, Math.min(value.length(), i + length)).toString());
        }
        return result;
    }

    /**
     * Splits the specified string into lines. Note: this is a Java 8 method polyfill for {@code String#lines()}
     * @param value The string to split
     * @return A non-null list of lines
     */
    public static List<String> splitByNewline(CharSequence value) {
        if (StringUtils.isEmpty(value) || StringUtils.equals(value, StringUtils.LF)) {
            return Collections.emptyList();
        }
        try (Reader reader = new StringReader(value.toString())) {
            return IOUtils.readLines(reader);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Splits the specified string by the specified delimiter, ignoring any occurrences of the delimiter that are
     * situated between the specified escape characters
     * @param value     The string to split
     * @param delimiter The character to split by
     * @param escapeChar The character to escape the delimiter
     * @return A non-null list of chunks
     */
    public static List<String> splitByUnescaped(CharSequence value, char delimiter, char escapeChar) {
        if (StringUtils.isEmpty(value)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        boolean isEscaped = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == escapeChar) {
                isEscaped = !isEscaped;
            }
            if (c == delimiter && !isEscaped) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result;
    }

    /**
     * Truncates the specified string to the given length by removing the middle part and replacing it with "{@code ...}"
     * @param value The string to truncate
     * @param limit The maximum length of the resulting string
     * @return A truncated string
     */
    public static String truncateMiddle(CharSequence value, int limit) {
        return truncateMiddle(value, limit, Constants.ELLIPSIS);
    }

    /**
     * Truncates the specified string to the given length by removing the middle part and replacing it with the
     * specified ellipsis string
     * @param value    The string to truncate
     * @param limit    The maximum length of the resulting string
     * @param ellipsis The ellipsis string to use
     * @return A truncated string
     */
    public static String truncateMiddle(CharSequence value, int limit, String ellipsis) {
        if (value == null) {
            return StringUtils.EMPTY;
        }
        if (value.length() < limit
                || (StringUtils.endsWith(value, ellipsis) && value.length() < limit + ellipsis.length())) {
            return value.toString();
        }
        String stripped = StringUtils.removeEnd(value.toString(), Constants.ELLIPSIS);
        String leftPart = StringUtils.substring(stripped, 0, (limit - 3) / 2);
        String rightPart = StringUtils.substring(stripped, value.length() - (limit - 3) / 2);
        return leftPart + ellipsis + rightPart;
    }

    /**
     * Truncates the specified string to the given length by removing the right part and replacing it with "{@code ...}"
     * @param value The string to truncate
     * @param limit The maximum length of the resulting string
     * @return A truncated string
     */
    public static String truncateRight(CharSequence value, int limit) {
        if (value == null) {
            return StringUtils.EMPTY;
        }
        return value.length() > limit
                ? StringUtils.substring(value.toString(), 0, limit - 3) + Constants.ELLIPSIS
                : value.toString();
    }
}
