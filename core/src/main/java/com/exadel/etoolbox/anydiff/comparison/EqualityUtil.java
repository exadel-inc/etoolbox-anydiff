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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.BiPredicate;

/**
 * Represents a faster way to compare two strings for equality, ignoring spaces. Intended for use as the
 * {@link com.github.difflib.text.DiffRowGenerator.Builder#equalizer(BiPredicate)} member
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class EqualityUtil {

    /**
     * Compares two strings for equality, ignoring spaces
     * @param string1 First string to compare
     * @param string2 Second string to compare
     * @return True or false
     */
    static boolean equalsIgnoreSpaces(String string1, String string2) {
        int i1 = 0;
        int i2 = 0;
        while (i1 < string1.length() || i2 < string2.length()) {
            i1 = advanceToBeginningOfWord(string1, i1);
            i2 = advanceToBeginningOfWord(string2, i2);
            if (i1 == -1 && i2 == -1) {
                break;
            }
            if (i1 == -1 || i2 == -1) {
                return false;
            }
            int commonLength = getCommonSequenceLength(string1, i1, string2, i2);
            if (commonLength == 0) {
                return false;
            }
            i1 += commonLength;
            i2 += commonLength;
        }
        return true;
    }

    private static int advanceToBeginningOfWord(String source, int index) {
        int cursor = index;
        while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) {
            cursor++;
        }
        return cursor >= source.length() ? -1 : cursor;
    }

    private static int advanceToEndOfWord(String source, int index) {
        int cursor = index;
        while (cursor < source.length() && !Character.isWhitespace(source.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static int getCommonSequenceLength(String str1, int index1, String str2, int index2) {
        int length1 = advanceToEndOfWord(str1, index1) - index1;
        int length2 = advanceToEndOfWord(str2, index2) - index2;
        if (length1 != length2 || length1 == 0) {
            return 0;
        }
        for (int i = 0; i < length1; i++) {
            if (str1.charAt(index1 + i) != str2.charAt(index2 + i)) {
                return 0;
            }
        }
        return length1;
    }
}
