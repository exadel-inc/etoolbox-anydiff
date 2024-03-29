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
package com.exadel.etoolbox.anydiff;

import com.exadel.etoolbox.anydiff.comparison.TaskParameters;
import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.filter.Filter;
import com.exadel.etoolbox.anydiff.runner.DiffRunner;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compares two sets of values and returns a list of differences
 * @see Diff
 * @see DiffEntry
 * @see Filter
 */
public class AnyDiff {

    private String[] leftStrings;
    private Path[] leftPaths;
    private String leftLabel;
    private String[] rightStrings;
    private Path[] rightPaths;
    private String rightLabel;

    private Boolean arrangeAttributes;
    private ContentType contentType;
    private Integer columnWidth;
    private Boolean renderErrors;
    private Boolean ignoreSpaces;
    private Boolean normalize;
    private List<Filter> filters;

    /* -------
       Strings
       ------- */

    // Left

    /**
     * Assigns the left side of the comparison
     * @param value The left-side value to compare. Can represent either a simple text, a path, or an URI
     * @return This instance
     */
    public AnyDiff left(String value) {
        if (value == null) {
            return this;
        }
        return left(new String[]{value});
    }

    /**
     * Assigns the left side of the comparison
     * @param value The left-side strings to compare. Can represent either a simple text, a path, or an URI
     * @return This instance
     */
    public AnyDiff left(String[] value) {
        return left(value, null);
    }

    /**
     * Assigns the left side of the comparison
     * @param value The left-side strings to compare. Can represent either a simple text, a path, or an URI
     * @param label The label to use for the left side of the comparison in the report
     * @return This instance
     */
    public AnyDiff left(String[] value, String label) {
        if (ArrayUtils.isEmpty(value)) {
            return this;
        }
        leftStrings = value;
        leftLabel = label;
        return this;
    }

    // Right

    /**
     * Assigns the right side of the comparison
     * @param value The right-side value to compare. Can represent either a simple text, a path, or an URI
     * @return This instance
     */
    public AnyDiff right(String value) {
        if (value == null) {
            return this;
        }
        return right(new String[] {value});
    }

    /**
     * Assigns the right side of the comparison
     * @param value The right-side strings to compare. Can represent either a simple text, a path, or an URI
     * @return This instance
     */
    public AnyDiff right(String[] value) {
        return right(value, null);
    }

    /**
     * Assigns the right side of the comparison
     * @param value The right-side strings to compare. Can represent either a simple text, a path, or an URI
     * @param label The label to use for the right side of the comparison in the report
     * @return This instance
     */
    public AnyDiff right(String[] value, String label) {
        if (ArrayUtils.isEmpty(value)) {
            return this;
        }
        rightStrings = value;
        rightLabel = label;
        return this;
    }

    /* -----
       Paths
       ----- */

    // Left

    /**
     * Assigns the left side of the comparison
     * @param value A {@link Path} that represents the left-side data to compare
     * @param label The label to use for the left side of the comparison in the report
     * @return This instance
     */
    public AnyDiff left(Path value, String label) {
        if (value == null) {
            return this;
        }
        return left(new Path[] {value}, label);
    }

    /**
     * Assigns the left side of the comparison
     * @param value An array of {@link Path} objects that represent the left-side data to compare
     * @param label The label to use for the left side of the comparison in the report
     * @return This instance
     */
    public AnyDiff left(Path[] value, String label) {
        if (ArrayUtils.isEmpty(value)) {
            return this;
        }
        leftPaths = value;
        leftLabel = label;
        return this;
    }

    // Right

    /**
     * Assigns the right side of the comparison
     * @param value A {@link Path} that represents the right-side data to compare
     * @param label The label to use for the right side of the comparison in the report
     * @return This instance
     */
    public AnyDiff right(Path value, String label) {
        if (value == null) {
            return this;
        }
        return right(new Path[] {value}, label);
    }

    /**
     * Assigns the right side of the comparison
     * @param value An array of {@link Path} objects that represent the right-side data to compare
     * @param label The label to use for the right side of the comparison in the report
     * @return This instance
     */
    public AnyDiff right(Path[] value, String label) {
        if (ArrayUtils.isEmpty(value)) {
            return this;
        }
        rightPaths = value;
        rightLabel = label;
        return this;
    }

    /* -------
       Filters
       ------- */

    /**
     * Assigns a {@link Filter} to the comparison
     * @param value A {@code Filter} object to use for the comparison
     * @return This instance
     */
    public AnyDiff filter(Filter value) {
        return filter(Collections.singletonList(value));
    }

    /**
     * Assigns a list of {@link Filter} objects to the comparison
     * @param value A list of {@code Filter} objects to use for the comparison
     * @return This instance
     */
    public AnyDiff filter(List<Filter> value) {
        if (CollectionUtils.isEmpty(value)) {
            return this;
        }
        if (filters == null) {
            filters = new ArrayList<>();
        }
        filters.addAll(value);
        return this;
    }

    /* --------------
       Misc arguments
       -------------- */

    /**
     * Assigns the flag telling whether to arrange tag attributes of markup content before comparison for more accurate
     * results
     * @param value Boolean value
     * @return This instance
     */
    public AnyDiff arrangeAttributes(boolean value) {
        this.arrangeAttributes = value;
        return this;
    }

    /**
     * Assigns the content type to use for the comparison
     * @param value A {@link ContentType} value to use for the comparison
     * @return This instance
     */
    public AnyDiff contentType(ContentType value) {
        this.contentType = value;
        return this;
    }

    /**
     * Assigns the column width to use for the comparison reports
     * @param value A positive integer value
     * @return This instance
     */
    public AnyDiff columnWidth(int value) {
        this.columnWidth = value;
        return this;
    }

    public AnyDiff displayErrorPages(boolean value) {
        this.renderErrors = value;
        return this;
    }

    /**
     * Assigns the flag telling whether to ignore spaces between words in comparison
     * @param value Boolean value
     * @return This instance
     */
    public AnyDiff ignoreSpaces(boolean value) {
        this.ignoreSpaces = value;
        return this;
    }

    /**
     * Assigns the flag telling whether to normalize markup content before comparison for more granular results
     * @param value Boolean value
     * @return This instance
     */
    public AnyDiff normalize(boolean value) {
        this.normalize = value;
        return this;
    }

    /* -------
       Actions
       ------- */

    /**
     * Performs the comparison
     * @return A list of {@link Diff} objects that represent the differences between the left and right sides of the
     * comparison. Can be empty but not {@code null}
     */
    public List<Diff> compare() {
        DiffRunner diffRunner = ArrayUtils.isNotEmpty(leftPaths) && ArrayUtils.isNotEmpty(rightPaths)
                ? DiffRunner.forValues(leftPaths, leftLabel, rightPaths, rightLabel)
                : DiffRunner.forValues(leftStrings, leftLabel, rightStrings, rightLabel);
        TaskParameters taskParameters = TaskParameters
                .builder()
                .arrangeAttributes(arrangeAttributes)
                .columnWidth(columnWidth)
                .renderErrors(renderErrors)
                .normalize(normalize)
                .ignoreSpaces(ignoreSpaces)
                .build();
        return diffRunner
                .withFilters(filters)
                .withContentType(contentType)
                .withTaskParameters(taskParameters)
                .run();
    }

    /**
     * Checks if the left and right sides of the comparison do not have pending differences. Either there are no
     * differences or all differences have been filtered out or else accepted as passable
     * @return True or false
     */
    public boolean isMatch() {
        List<Diff> differences = compare();
        return isMatch(differences);
    }

    /* ----------------
       Actions (static)
       ---------------- */

    /**
     * Checks if the specified list of {@link Diff} objects does not have pending differences. Either there are no
     * differences or all differences have been filtered out or else accepted as passable
     * @param value A list of {@code Diff} objects
     * @return True or false
     */
    public static boolean isMatch(List<Diff> value) {
        return CollectionUtils.isEmpty(value) || value.stream().allMatch(diff -> diff.getPendingCount() == 0);
    }
}
