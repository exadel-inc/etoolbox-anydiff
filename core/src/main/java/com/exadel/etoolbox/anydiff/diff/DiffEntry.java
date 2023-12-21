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

import com.exadel.etoolbox.anydiff.AnyDiff;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a single item of differences
 */
public interface DiffEntry extends Adaptable {

    /* ---------
       Accessors
       --------- */

    /**
     * Gets the {@link Diff} instance this entry belongs to
     * @return {@link Diff} instance
     */
    Diff getDiff();

    /**
     * Gets the name to distinguish the type of difference. Defaults to the name of the implementing class
     * @return String value. A non-empty string is expected
     */
    default String getName() {
        return StringUtils.removeEnd(getClass().getSimpleName(), "Impl");
    }

    /*
     * Gets the "kind" of difference. E.g., "change", "insertion", "deletion", etc.
     * @return {@link DiffState} instance
     */
    DiffState getState();

    /* -------
       Content
       ------- */

    /**
     * Gets the left part of the comparison included in the current difference
     * @return String value
     */
    default String getLeft() {
        return getLeft(false);
    }

    /**
     * Gets the left part of the comparison included in the current difference
     * @param includeContext If set to true, the "context" elements (those going before and after the actual difference)
     *                       are added to the result. Otherwise, only the difference itself is returned
     * @return String value
     */
    String getLeft(boolean includeContext);

    /**
     * Gets the right part of the comparison included in the current difference
     * @return String value
     */
    default String getRight() {
        return getRight(false);
    }

    /**
     * Gets the right part of the comparison included in the current difference
     * @param includeContext If set to true, the "context" elements (those going before and after the actual
     *                       difference)
     * @return String value
     */
    String getRight(boolean includeContext);

    /* ----------
       Operations
       ---------- */

    /**
     * Accepts the difference, that is, silences it so that it no longer leads to the {@link AnyDiff} reporting a
     * mismatch. However, the difference is still included in the {@link Diff} and can be displayed to the user
     */
    void accept();
}
