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
package com.exadel.etoolbox.anydiff.filter;

import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.diff.Fragment;
import com.exadel.etoolbox.anydiff.diff.FragmentPair;

/**
 * Represents a custom filter that can be applied to a {@link Diff} object or its parts to remove some of the detected
 * differences as irrelevant or else "silence" them so that they are still displayed to the user but do not affect the
 * result of the comparison
 */
public interface Filter {

    /**
     * When overridden in a derived class, instructs the {@code Filter} to accept ("silence") the specified {@link Diff}
     * object
     * @param value {@code Diff} object. A non-null value is expected
     * @return {@code True} if the {@code Diff} object is processed and accepted, {@code false} otherwise
     */
    default boolean acceptDiff(Diff value) {
        return false;
    }

    /**
     * When overridden in a derived class, instructs the {@code Filter} to exclude the specified {@link Diff} object
     * from the result of the comparison
     * @param value {@code Diff} object. A non-null value is expected
     * @return {@code True} if the {@code Diff} object is processed and accepted, {@code false} otherwise
     */
    default boolean skipDiff(Diff value) {
        return false;
    }

    /**
     * When overridden in a derived class, instructs the {@code Filter} to accept ("silence") the specified
     * {@link DiffEntry} object representing a text block
     * @param value {@code DiffEntry} object. A non-null value is expected
     * @return {@code True} if the {@code DiffEntry} object is processed and accepted, {@code false} otherwise
     */
    default boolean acceptBlock(DiffEntry value) {
        return false;
    }

    /**
     * When overridden in a derived class, instructs the {@code Filter} to exclude the specified {@link DiffEntry}
     * object representing a text block from the result of the comparison
     * @param value {@code DiffEntry} object. A non-null value is expected
     * @return {@code True} if the {@code DiffEntry} object is processed and accepted, {@code false} otherwise
     */
    default boolean skipBlock(DiffEntry value) {
        return false;
    }

    /**
     * When overridden in a derived class, instructs the {@code Filter} to accept ("silence") the specified
     * {@link DiffEntry} object representing a line
     * @param value {@code DiffEntry} object. A non-null value is expected
     * @return {@code True} if the {@code DiffEntry} object is processed and accepted, {@code false} otherwise
     */
    default boolean acceptLine(DiffEntry value) {
        return false;
    }

    /**
     * When overridden in a derived class, instructs the {@code Filter} to exclude the specified {@link DiffEntry}
     * object representing a line from the result of the comparison
     * @param value {@code DiffEntry} object. A non-null value is expected
     * @return {@code True} if the {@code DiffEntry} object is processed and accepted, {@code false} otherwise
     */
    default boolean skipLine(DiffEntry value) {
        return false;
    }

    /**
     * When overridden in a derived class, instructs the {@code Filter} to accept ("silence") the specified
     * {@link FragmentPair} object
     * @param value {@code FragmentPair} object. A non-null value is expected
     * @return {@code True} if the {@code FragmentPair} object is processed and accepted, {@code false} otherwise
     */
    default boolean acceptFragments(FragmentPair value) {
        return false;
    }

    default boolean skipFragments(FragmentPair value) {
        return false;
    }

    /**
     * When overridden in a derived class, instructs the {@code Filter} to accept ("silence") the specified
     * {@link Fragment}
     * @param value {@code Fragment} object. A non-null value is expected
     * @return {@code True} if the {@code Fragment} object is processed and accepted, {@code false} otherwise
     */
    default boolean acceptFragment(Fragment value) {
        return false;
    }

    /**
     * When overridden in a derived class, instructs the {@code Filter} to exclude the specified {@link Fragment} from
     * the result of the comparison
     * @param value {@code Fragment} object. A non-null value is expected
     * @return {@code True} if the {@code Fragment} object is processed and accepted, {@code false} otherwise
     */
    default boolean skipFragment(Fragment value) {
        return false;
    }
}
