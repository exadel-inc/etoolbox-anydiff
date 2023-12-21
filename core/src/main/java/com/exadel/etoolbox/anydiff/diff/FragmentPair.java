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

/**
 * Represents a pair of mutually correspondent fragments from the compared content. Two fragments are considered a pair
 * if found in lines by the same index and either start at the same position in each line or share the same ordinal
 * among other fragments of each line
 */
public interface FragmentPair {

    /**
     * Gets the left fragment of the pair
     * @return String value
     */
    String getLeft();

    /**
     * Gets the right fragment of the pair
     * @return String value
     */
    String getRight();

    /**
     * Gets the left fragment of the pair as a {@link Fragment} instance
     * @return {@code Fragment} instance
     */
    Fragment getLeftFragment();

    /**
     * Gets the right fragment of the pair as a {@link Fragment} instance
     * @return {@code Fragment} instance
     */
    Fragment getRightFragment();

    /**
     * Accepts the pair of fragments, that is, silences them so that they no longer lead to the {@link AnyDiff}
     * reporting a mismatch. However, the pair is still included in the {@link Diff} and can be displayed to the
     * user
     */
    void accept();
}
