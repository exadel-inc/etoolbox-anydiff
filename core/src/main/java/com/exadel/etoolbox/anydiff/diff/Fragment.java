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
 * Represents the most elementary difference between two strings (usually one or more characters within a line)
 */
public interface Fragment extends CharSequence, Adaptable {

    /**
     * Gets the string (either the left or the right part of the comparison) this {@link Fragment} belongs to
     * @return A non-null string
     */
    String getSource();

    /**
     * Gets whether this {@link Fragment} represents an insertion (i.e., a part of the line to the right that is missing
     * in the line to the left)
     * @return True or false
     */
    boolean isInsert();

    /**
     * Gets whether this {@link Fragment} represents a deletion (i.e., a part of the line to the left that is missing in
     * the line to the right)
     * @return True or false
     */
    boolean isDelete();

    /**
     * Gets whether the difference is "pending", that is, has not been silenced and will eventually make
     * {@link AnyDiff} report a mismatch
     * @return True or false
     */
    default boolean isPending() {
        return isInsert() || isDelete();
    }

    /**
     * Gets whether this {@link Fragment} equals to the provided other string content-wise
     * @param other String value to compare to
     * @return True or false
     */
    default boolean equals(String other) {
        return StringUtils.equals(toString(), other);
    }
}
