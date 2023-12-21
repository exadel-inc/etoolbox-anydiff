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

import com.exadel.etoolbox.anydiff.Constants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper class user to build strings representing HTML tags
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
class HtmlTag {

    private final StringBuilder cumulativeBuilder = new StringBuilder();

    private String pendingTag;

    private Map<String, String> pendingAttributes;

    private String pendingContent;

    /**
     * Adds an attribute to the current tag
     * @param key   Attribute name
     * @param value Attribute value
     * @return Current instance
     */
    HtmlTag withAttr(String key, String value) {
        if (StringUtils.isAnyBlank(key, value)) {
            return this;
        }
        if (pendingAttributes == null) {
            pendingAttributes = new LinkedHashMap<>();
        }
        pendingAttributes.put(key, value);
        return this;
    }

    /**
     * Adds a {@code class} attribute to the current tag
     * @param value Attribute value
     * @return Current instance
     */
    HtmlTag withClassAttr(String value) {
        return withAttr("class", value);
    }

    /**
     * Adds text content to the current tag
     * @param value Text content
     * @return Current instance
     */
    HtmlTag withContent(CharSequence value) {
        return withContent(value, false);
    }

    /**
     * Adds text content to the current tag
     * @param value   Text content
     * @param prepend If {@code true}, the content is prepended to the existing content; otherwise, it is appended
     * @return Current instance
     */
    HtmlTag withContent(CharSequence value, boolean prepend) {
        if (StringUtils.isEmpty(value)) {
            return this;
        }
        if (prepend) {
            pendingContent = value + StringUtils.defaultString(pendingContent);
        } else {
            pendingContent = StringUtils.defaultString(pendingContent) + value;
        }
        return this;
    }

    /**
     * Creates a new {@code <a>} tag and adds it to the current string
     * @return Current instance
     */
    HtmlTag a() {
        return tag("a");
    }

    /**
     * Creates a new {@code <div>} tag and adds it to the current string
     * @return Current instance
     */
    HtmlTag div() {
        return tag("div");
    }

    /**
     * Creates a new {@code <h4>} tag and adds it to the current string
     * @return Current instance
     */
    HtmlTag h4() {
        return tag("h4");
    }

    /**
     * Creates a new {@code <line>} tag and adds it to the current string
     * @return Current instance
     */
    HtmlTag line() {
        return tag("line");
    }

    /**
     * Creates a new {@code <section>} tag and adds it to the current string
     * @return Current instance
     */
    HtmlTag section() {
        return tag("section");
    }

    /**
     * Creates a new {@code <span>} tag and adds it to the current string
     * @return Current instance
     */
    HtmlTag span() {
        return tag("span");
    }

    /**
     * Creates a new named tag and adds it to the current string
     * @param name Tag name
     * @return Current instance
     */
    private HtmlTag tag(String name) {
        if (StringUtils.isEmpty(name)) {
            return this;
        }
        conclude();
        pendingTag = name;
        return this;
    }

    /**
     * Adds text content to the current string
     * @param value Text content
     * @return Current instance
     */
    @SuppressWarnings("SameParameterValue")
    HtmlTag text(CharSequence value) {
        conclude();
        cumulativeBuilder.append(value);
        return this;
    }

    /**
     * Wraps the current string in a tag with the specified name
     * @param tag Tag name
     * @return Current instance
     */
    @SuppressWarnings("SameParameterValue")
    HtmlTag wrapIn(String tag) {
        if (StringUtils.isEmpty(tag)) {
            return this;
        }
        return wrapIn(new HtmlTag().tag(tag));
    }

    /**
     * Wraps the current string in the specified {@link HtmlTag} object
     * @param tag {@code HtmlTag} instance. A non-null value is expected
     * @return Current instance
     */
    HtmlTag wrapIn(HtmlTag tag) {
        if (tag == null) {
            return this;
        }
        return tag.withContent(toString());
    }

    @Override
    public String toString() {
        return cumulativeBuilder + fromPending();
    }

    private void conclude() {
        cumulativeBuilder.append(fromPending());
        pendingTag = null;
        pendingContent = null;
        pendingAttributes = null;
    }

    private String fromPending() {
        if (StringUtils.isEmpty(pendingTag)) {
            return StringUtils.EMPTY;
        }
        StringBuilder pendingBuilder = new StringBuilder();
        pendingBuilder.append(Constants.TAG_OPEN).append(pendingTag);
        if (MapUtils.isNotEmpty(pendingAttributes)) {
            pendingAttributes.forEach((key, value) ->
                    pendingBuilder
                            .append(StringUtils.SPACE)
                            .append(key)
                            .append(Constants.TAG_ATTR_OPEN)
                            .append(value)
                            .append(Constants.TAG_ATTR_CLOSE));
        }
        pendingBuilder
                .append(Constants.TAG_CLOSE)
                .append(StringUtils.defaultString(pendingContent))
                .append(Constants.TAG_PRE_CLOSE)
                .append(pendingTag)
                .append(Constants.TAG_CLOSE);
        return pendingBuilder.toString();
    }
}
