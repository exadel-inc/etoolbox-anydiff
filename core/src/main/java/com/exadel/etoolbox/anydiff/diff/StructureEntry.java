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

/**
 * Represents a single difference within a {@link Diff} that can be identified by a path in the compared content
 */
public interface StructureEntry {

    /**
     * Gets the path of the current entry in the compared content. E.g. an {@code XPath} of an XML node that contains
     * the difference
     * @return A nullable string value
     */
    String getPath();
}
