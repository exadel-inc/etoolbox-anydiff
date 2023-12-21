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
package com.exadel.etoolbox.anydiff.diff;

import com.exadel.etoolbox.anydiff.filter.Filter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * Represents a type of difference represented by a {@link DiffEntry} instance. This is usually needed to determine
 * a relevant {@link Filter} from a list of filters to handle a particular difference
 */
public enum DiffEntryType {
    UNDEFINED(),
    BLOCK("Block", "entry"),
    LINE("Line"),
    FRAGMENT_PAIR("FragmentPair", "fragments"),
    FRAGMENT("Fragment", "fragment");

    private final String[] tokens;

    /**
     * Creates a {@code DiffEntryType} object
     * @param tokens One or more tokens that can be used to identify the type of difference
     */
    DiffEntryType(String... tokens) {
        this.tokens = tokens;
    }

    /**
     * Retrieves a matching {@code DiffEntryType} object for the given token (as specified in a {@link Filter} object
     * @param token The token to match. A non-null string value is expected
     * @return A non-null {@code DiffEntryType} object
     */
    public static DiffEntryType from(String token) {
        return Arrays.stream(DiffEntryType.values())
                .filter(kind -> Arrays.stream(kind.tokens).anyMatch(t -> StringUtils.equalsIgnoreCase(token, t)))
                .findFirst()
                .orElse(UNDEFINED);
    }
}
