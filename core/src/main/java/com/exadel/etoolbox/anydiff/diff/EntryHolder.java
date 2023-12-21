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

import java.util.List;

/**
 * Represents an item of detected differences that can be split into more elementary differences (e.g. a line block to
 * separate lines). Each "elementary" difference is represented by a {@link DiffEntry} object
 */
public interface EntryHolder {

    /**
     * Gets the list of child {@link DiffEntry} instances
     * @return {@code List} of {@code DiffEntry} instances
     */
    List<? extends DiffEntry> children();

    /**
     * Instructs the {@code EntryHolder} to exclude the specified {@link DiffEntry} from the list of its children
     * @param value {@code DiffEntry} instance to exclude
     */
    void exclude(DiffEntry value);
}
