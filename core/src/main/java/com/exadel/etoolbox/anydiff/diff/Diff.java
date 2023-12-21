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

/**
 * Represents the aggregate difference between two strings. Usually, it includes several particular differences exposed
 * as a collection of {@link DiffEntry} objects
 */
public interface Diff extends PrintableEntry, EntryHolder {

    /**
     * Gets the "kind" of difference. E.g., "change", "insertion", "deletion", etc.
     * @return {@link DiffState} instance
     */
    DiffState getState();

    /**
     * Gets the number of differences detected between the two pieces of content represented by the current {@link Diff}
     * @return Integer value
     */
    int getCount();

    /**
     * Gets the number of differences detected between the two pieces of content represented by the current {@link Diff}.
     * Counts only the differences that have not been "silenced" (accepted) with a {@link Filter}
     * @return Integer value
     */
    int getPendingCount();

    /**
     * Gets the left part of the comparison
     * @return String value
     */
    String getLeft();

    /**
     * Gets the right part of the comparison
     * @return String value
     */
    String getRight();
}
