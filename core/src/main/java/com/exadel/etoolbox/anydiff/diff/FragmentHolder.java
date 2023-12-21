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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an item of detected differences that can be split into {@link Fragment}s
 */
public interface FragmentHolder {

    /* -------
       Content
       ------- */

    /**
     * Gets the list of {@link Fragment} instances from the left side of the comparison
     * @return A list of {@code Fragment} objects
     */
    List<Fragment> getLeftFragments();

    /**
     * Gets the list of {@link Fragment} instances from the right side of the comparison
     * @return A list of {@code Fragment} objects
     */
    List<Fragment> getRightFragments();

    /**
     * Gets the list of {@link Fragment} instances from both sides of the comparison
     * @return A list of {@code Fragment} objects
     */
    default List<Fragment> getFragments() {
        return Stream.concat(getLeftFragments().stream(), getRightFragments().stream()).collect(Collectors.toList());
    }

    /* ----------
       Operations
       ---------- */

    /**
     * Accepts the fragment, that is, silences it so that it no longer leads to the {@link AnyDiff} reporting a
     * mismatch. However, the fragment is still included in the {@link Diff} and can be displayed to the user
     */
    void accept(Fragment value);

    /**
     * Instructs the {@code FragmentHolder} to exclude the specified {@link Fragment} from the list of its children
     * @param value {@code Fragment} object to exclude
     */
    void exclude(Fragment value);
}
