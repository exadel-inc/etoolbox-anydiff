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
import com.exadel.etoolbox.anydiff.ContentType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Contains the parameters that control the execution of a {@link DiffTask}. Every parameter is optional and therefore
 * presented by a nullable value. If not set, a default value is returned instead
 * <u>Note</u>: This class is not a part of public API and is subject to change. You should not use it directly
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(builderClassName = "Builder")
public class TaskParameters {

    private static final TaskParameters FOR_MARKUP = TaskParameters
            .builder()
            .arrangeAttributes(true)
            .normalize(true)
            .ignoreSpaces(false)
            .build();

    private static final TaskParameters FOR_TEXT = TaskParameters
            .builder()
            .arrangeAttributes(false)
            .normalize(false)
            .ignoreSpaces(false)
            .build();

    static final TaskParameters DEFAULT = FOR_TEXT;

    private static final int MIN_COLUMN_WIDTH = 10;

    private Boolean arrangeAttributes;

    private Integer columnWidth;

    private Boolean ignoreSpaces;

    private Boolean normalize;

    /**
     * Gets whether to uniformly arrange tag attributes in markup content (such as an HTML or XML file)
     * @return True or false
     */
    public boolean arrangeAttributes() {
        return arrangeAttributes != null ? arrangeAttributes : Constants.DEFAULT_ARRANGE_ATTRIBUTES;
    }

    /**
     * Gets the width of content column in side-by-side comparison log
     */
    public int getColumnWidth() {
        return columnWidth != null && columnWidth >= MIN_COLUMN_WIDTH ? columnWidth : Constants.DEFAULT_COLUMN_WIDTH;
    }

    /**
     * Gets whether to ignore spaces when comparing content
     * @return True or false
     */
    public boolean ignoreSpaces() {
        return ignoreSpaces != null ? ignoreSpaces : Constants.DEFAULT_IGNORE_SPACES;
    }

    /**
     * Gets whether to normalize markup content (such as in a HTML or XML file)
     * @return True or false
     */
    public boolean normalize() {
        return normalize != null ? normalize : Constants.DEFAULT_NORMALIZE;
    }

    /**
     * Retrieves an instance of {@link TaskParameters} composed of non-null values of the both provided arguments. If
     * both arguments contain a non-null value, the second one will override
     * @param first {@code TaskParameters} object
     * @param second {@code TaskParameters} object
     * @return The resulting object
     */
    static TaskParameters merge(TaskParameters first, TaskParameters second) {
        if (!isEmpty(first) && isEmpty(second)) {
            return first;
        }
        if (isEmpty(first)) {
            return second;
        }
        return TaskParameters
                .builder()
                .arrangeAttributes(second.arrangeAttributes != null ? second.arrangeAttributes : first.arrangeAttributes)
                .columnWidth(second.columnWidth != null ? second.columnWidth : first.columnWidth)
                .ignoreSpaces(second.ignoreSpaces != null ? second.ignoreSpaces : first.ignoreSpaces)
                .normalize(second.normalize != null ? second.normalize : first.normalize)
                .build();
    }

    private static boolean isEmpty(TaskParameters value) {
        if (value == null) {
            return true;
        }
        return value.arrangeAttributes == null
                && value.columnWidth == null
                && value.ignoreSpaces == null
                && value.normalize == null;
    }

    /* -------------
       Factory logic
       ------------- */

    /**
     * Retrieves the default set of task parameters based on the given {@link ContentType} value
     * @param contentType {@code ContentType} instance
     * @return {@link TaskParameters} value
     */
    static TaskParameters from(ContentType contentType) {
        if (contentType == ContentType.XML || contentType == ContentType.HTML) {
            return FOR_MARKUP;
        }
        return FOR_TEXT;
    }
}
