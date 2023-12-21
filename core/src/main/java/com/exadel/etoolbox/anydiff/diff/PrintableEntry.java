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

import com.exadel.etoolbox.anydiff.OutputType;

/**
 * Represents an item of detected differences that can be printed to the user as a text or HTML
 */
public interface PrintableEntry {

    /**
     * Retrieves the string representation of the difference in the specified format
     * @param target {@code OutputType} value representing the desired output format
     * @return A non-null string value
     */
    String toString(OutputType target);

    /**
     * Retrieves the string representation of the difference in the specified format
     * @param target {@code OutputType} value representing the desired output format
     * @param element An optional token that specifies the selection of element(-s) to be printed
     * @return A non-null string value
     */
    default String toString(OutputType target, String element) {
        return toString(target);
    }
}
