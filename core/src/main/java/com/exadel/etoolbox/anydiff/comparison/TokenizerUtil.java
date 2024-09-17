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
package com.exadel.etoolbox.anydiff.comparison;

import com.exadel.etoolbox.anydiff.Constants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains utility methods for splitting strings into tokens
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class TokenizerUtil {

    /**
     * Splits the specified string into tokens for a more granular comparison
     * @param value String to split
     * @return A non-null list of tokens
     */
    static List<String> getTokens(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.singletonList(StringUtils.defaultString(value));
        }
        List<String> split = new ArrayList<>();
        StringBuilder builder = new StringBuilder().append(value.charAt(0));
        for (int i = 1; i < value.length(); i++) {
            if (shouldSplit(value, i)) {
                split.add(builder.toString());
                builder = new StringBuilder();
            }
            builder.append(value.charAt(i));
        }
        if (StringUtils.isNotEmpty(builder)) {
            split.add(builder.toString());
        }
        return split;
    }

    private static boolean shouldSplit(String value, int position) {
        char previous = value.charAt(position - 1);
        char current = value.charAt(position);

        boolean isPreviousWhitespace = Character.isWhitespace(previous);
        boolean isCurrentWhitespace = Character.isWhitespace(current);
        if ((isPreviousWhitespace && !isCurrentWhitespace)
                || (!isPreviousWhitespace && isCurrentWhitespace)) {
            return true;
        }
        if (current == Constants.TAG_OPEN_CHAR || previous == Constants.TAG_CLOSE_CHAR) {
            return true; // To split between different HTML/XML tags
        }
        return (isLetterOrDigit(value, position - 1) && !isLetterOrDigit(value, position))
                || (!isLetterOrDigit(value, position - 1) && isLetterOrDigit(value, position));
    }

    private static boolean isLetterOrDigit(String value, int position) {
        char current = value.charAt(position);
        if (Character.isLetterOrDigit(current) || current == Constants.DASH_CHAR || current == Constants.UNDERSCORE_CHAR) {
            return true;
        }
        if (current == Constants.COLON_CHAR) {
            return position < value.length() - 1
                    && CharUtils.isAsciiAlphanumeric(value.charAt(position - 1))
                    && CharUtils.isAsciiAlphanumeric(value.charAt(position + 1));
        }
        return false;
    }
}
