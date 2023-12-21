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
package com.exadel.etoolbox.anydiff.comparison;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Contains factory methods for creating {@link HtmlTag} instances
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class HtmlTags {

    /**
     * Creates a new {@link HtmlTag} instance representing an {@code <a>} tag
     * @return {@link HtmlTag} instance
     */
    static HtmlTag a() {
        return new HtmlTag().a();
    }

    /**
     * Creates a new {@link HtmlTag} instance representing a {@code <div>} tag
     * @return {@link HtmlTag} instance
     */
    static HtmlTag div() {
        return new HtmlTag().div();
    }

    /**
     * Creates a new {@link HtmlTag} instance representing a {@code <h1>} tag
     * @return {@link HtmlTag} instance
     */
    static HtmlTag h4() {
        return new HtmlTag().h4();
    }

    /**
     * Creates a new {@link HtmlTag} instance representing a {@code <line>} tag
     * @return {@link HtmlTag} instance
     */
    static HtmlTag line() {
        return new HtmlTag().line();
    }

    /**
     * Creates a new {@link HtmlTag} instance representing a {@code <section>} tag
     * @return {@link HtmlTag} instance
     */
    static HtmlTag section() {
        return new HtmlTag().section();
    }

    /**
     * Creates a new {@link HtmlTag} instance representing a {@code <span>} tag
     * @return {@link HtmlTag} instance
     */
    static HtmlTag span() {
        return new HtmlTag().span();
    }
}
