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
package com.exadel.etoolbox.anydiff.runner;

import com.exadel.etoolbox.anydiff.Constants;
import com.exadel.etoolbox.anydiff.ContentType;
import com.exadel.etoolbox.anydiff.comparison.DiffTask;
import com.exadel.etoolbox.anydiff.comparison.Marker;
import com.exadel.etoolbox.anydiff.comparison.TaskParameters;
import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.diff.DiffState;
import com.exadel.etoolbox.anydiff.filter.Filter;
import com.exadel.etoolbox.anydiff.util.ContentUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Extracts the data from the given sources and invokes the comparison routine
 * <u>Note</u>: This class is not a part of public API and is subject to change. You should not use it directly
 * @see DiffTask
 */
public abstract class DiffRunner {

    private static final DiffRunner EMPTY = new DiffRunner() {
        @Override
        public List<Diff> runInternal() {
            return Collections.emptyList();
        }
    };

    private String leftLabel;

    private String rightLabel;

    private Predicate<Diff> diffFilter;

    /**
     * Gets the {@code Predicate} instance that represents the aggregate filter for the results of the comparison
     */
    @Getter(value = AccessLevel.PACKAGE)
    private Predicate<DiffEntry> entryFilter;

    private ContentType contentType;

    /**
     * Gets the parameters that control the execution of a {@link DiffTask}
     */
    @Getter(value = AccessLevel.PACKAGE)
    private TaskParameters taskParameters = TaskParameters.DEFAULT;

    /* ----------
       Properties
       ---------- */

    /**
     * Gets the {@code ContentType} instance that represents the content type of the data to be compared
     * @return {@code ContentType} object
     */
    public ContentType getContentType() {
        return contentType != null ? contentType : ContentType.UNDEFINED;
    }

    /**
     * Gets the label for the left side of the comparison
     * @return A non-blank {@code String} value
     */
    String getLeftLabel() {
        return StringUtils.defaultIfBlank(leftLabel, Constants.LABEL_LEFT);
    }

    /**
     * Gets the label for the right side of the comparison
     * @return A non-blank {@code String} value
     */
    String getRightLabel() {
        return StringUtils.defaultIfBlank(rightLabel, Constants.LABEL_RIGHT);
    }

    /**
     * Assigns a collection of {@link Filter} instances that are used to produce the aggregate filter for the results
     * of the comparison. One can use either this method or {@link #withEntryFilter(Predicate)} to set the filter
     * @param filters {@code List} of {@code Filter} objects. A non-null value is expected
     * @return Current instance
     */
    public DiffRunner withFilters(List<Filter> filters) {
        diffFilter = FilterHelper.getDiffFilter(filters);
        entryFilter = FilterHelper.getEntryFilter(filters);
        return this;
    }

    /**
     * Assigns a {@link Predicate} instance that is used to filter the results of the comparison. One can use either
     * this method or {@link #withFilters(List)} to set the filter
     * @param entryFilter {@code Predicate} object. A non-null value is expected
     * @return Current instance
     */
    DiffRunner withEntryFilter(Predicate<DiffEntry> entryFilter) {
        this.entryFilter = entryFilter;
        return this;
    }

    /**
     * Assigns a {@link ContentType} instance that represents the content type of the data to be compared
     * @param contentType {@code ContentType} object. A non-null value is expected
     * @return Current instance
     */
    public DiffRunner withContentType(ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * Assigns a {@link com.exadel.etoolbox.anydiff.comparison.TaskParameters} instance that represents the
     * conversion/formatting rules for the comparison
     * @param parameters {@code TaskParameters} object. A non-null value is expected
     * @return Current instance
     */
    public DiffRunner withTaskParameters(TaskParameters parameters) {
        this.taskParameters = parameters;
        return this;
    }

    /* ---------
       Execution
       --------- */

    /**
     * Runs the data collection and subsequently invokes the comparison routine. The results are filtered according to
     * the previously assigned {@code Predicate}
     * @return A non-null list of {@link Diff} objects. Can be empty
     */
    public List<Diff> run() {
        return runInternal().stream().filter(diffFilter != null ? diffFilter : e -> true).collect(Collectors.toList());
    }

    /**
     * When overridden in a derived class, runs the data collection and invokes the comparison routine
     * @return An unfiltered list of {@link Diff} objects that are then passed to the {@link #run()} method
     */
    abstract List<Diff> runInternal();

    /**
     * Shortcuts the comparison for the case when the left part of the comparison is missing
     * @param left  An identifier of the left side of the comparison
     * @param right An identifier of the right side of the comparison
     * @return A {@link Diff} object representing the result of the comparison
     */
    Diff reportLeftMissing(String left, String right) {
        return DiffTask
                .builder()
                .leftId(left)
                .leftLabel(getLeftLabel())
                .rightId(right)
                .rightLabel(getRightLabel())
                .rightContent(right)
                .taskParameters(getTaskParameters())
                .anticipatedState(DiffState.LEFT_MISSING)
                .build()
                .run();
    }

    /**
     * Shortcuts the comparison for the case when the right part of the comparison is missing
     * @param left  An identifier of the left side of the comparison
     * @param right An identifier of the right side of the comparison
     * @return A {@link Diff} object representing the result of the comparison
     */
    Diff reportRightMissing(String left, String right) {
        return DiffTask
                .builder()
                .leftId(left)
                .leftLabel(getLeftLabel())
                .leftContent(left)
                .rightId(right)
                .rightLabel(getRightLabel())
                .taskParameters(getTaskParameters())
                .anticipatedState(DiffState.RIGHT_MISSING)
                .build()
                .run();
    }

    /**
     * Shortcuts the comparison for the case when the left and right parts of the comparison represent the content of a
     * file that has been moved
     * @param left  An identifier of the left side of the comparison
     * @param right An identifier of the right side of the comparison
     * @return A {@link Diff} object representing the result of the comparison
     */
    Diff reportMoved(String left, FileMoveInfo right) {
        return DiffTask
                .builder()
                .leftId(left)
                .leftLabel(getLeftLabel())
                .leftContent(Marker.PLACEHOLDER.wrap(left))
                .rightId(right.getPath())
                .rightLabel(getRightLabel())
                .rightContent(right)
                .taskParameters(getTaskParameters())
                .anticipatedState(DiffState.CHANGE)
                .build()
                .run();
    }

    /* ---------------
       Factory methods
       --------------- */

    /**
     * Creates a {@link DiffRunner} instance that corresponds to the specified labeled string arrays
     * @param left       An array of strings that represents the left side of the comparison
     * @param leftLabel  The label for the left side of the comparison
     * @param right      An array of strings that represents the right side of the comparison
     * @param rightLabel The label for the right side of the comparison
     * @return A {@code DiffRunner} instance
     */
    public static DiffRunner forValues(String[] left, String leftLabel, String[] right, String rightLabel) {
        if (ArrayUtils.isEmpty(left) || ArrayUtils.isEmpty(right)) {
            return EMPTY;
        }
        DiffRunner result = new StringListRunner(left, right);
        result.leftLabel = leftLabel;
        result.rightLabel = rightLabel;
        return result;
    }

    /**
     * Creates a {@link DiffRunner} instance that corresponds to the specified labeled strings
     * @param left       A string that represents the left side of the comparison
     * @param leftLabel  The label for the left side of the comparison
     * @param right      A string that represents the right side of the comparison
     * @param rightLabel The label for the right side of the comparison
     * @return A {@code DiffRunner} instance
     */
    static DiffRunner forValues(String left, String leftLabel, String right, String rightLabel) {
        if (StringUtils.isAnyBlank(left, right)) {
            return EMPTY;
        }
        DiffRunner result;
        if (isHttpEndpoint(left) && isHttpEndpoint(right)) {
            result = new HttpRunner(left, right);
            result.leftLabel = leftLabel;
            result.rightLabel = rightLabel;
        } else if (isFile(left) && isFile(right)) {
            Path currentFolder = Paths.get(StringUtils.EMPTY);
            result = forValues(
                    currentFolder.resolve(left).toFile().toPath(),
                    leftLabel,
                    currentFolder.resolve(right).toFile().toPath(),
                    rightLabel);
        } else {
            result = new SimpleRunner(left, right);
            result.contentType = ContentType.TEXT;
            result.leftLabel = leftLabel;
            result.rightLabel = rightLabel;
        }
        return result;
    }

    /**
     * Creates a {@link DiffRunner} instance that corresponds to the specified labeled {@link Path} arrays
     * @param left       An array of {@code Path} objects that represents the left side of the comparison
     * @param leftLabel  The label for the left side of the comparison
     * @param right      An array of {@code Path} objects that represents the right side of the comparison
     * @param rightLabel The label for the right side of the comparison
     * @return A {@code DiffRunner} instance
     */
    public static DiffRunner forValues(Path[] left, String leftLabel, Path[] right, String rightLabel) {
        if (ArrayUtils.isEmpty(left) || ArrayUtils.isEmpty(right)) {
            return EMPTY;
        }
        DiffRunner result = new PathListRunner(left, right);
        result.leftLabel = leftLabel;
        result.rightLabel = rightLabel;
        return result;
    }

    /**
     * Creates a {@link DiffRunner} instance that corresponds to the specified labeled {@link Path} objects
     * @param left       A {@code Path} object that represents the left side of the comparison
     * @param leftLabel  The label for the left side of the comparison
     * @param right      A {@code Path} object that represents the right side of the comparison
     * @param rightLabel The label for the right side of the comparison
     * @return A {@code DiffRunner} instance
     */
    static DiffRunner forValues(Path left, String leftLabel, Path right, String rightLabel) {
        if (left == null || right == null) {
            return EMPTY;
        }
        DiffRunner result = null;
        if (Files.isDirectory(left) && Files.isDirectory(right)) {
            result = new DirectoryRunner(left, right);
        } else if (Files.isRegularFile(left) && Files.isRegularFile(right)) {
            if (StringUtils.endsWithAny(left.toString().toLowerCase(), ".list", ".lst")
                    && StringUtils.endsWithAny(right.toString().toLowerCase(), ".list", ".lst")) {
                result = new FileListingRunner(left, right);
            } else if (StringUtils.endsWithAny(left.toString().toLowerCase(), ".zip", ".jar")
                    && StringUtils.endsWithAny(right.toString().toLowerCase(), ".zip", ".jar")) {
                result = new ArchiveRunner(left, right);
            } else {
                result = new FileRunner(left, right);
            }
        }
        if (result != null) {
            result.leftLabel = leftLabel;
            result.rightLabel = rightLabel;
            return result;
        }
        return EMPTY;
    }

    private static boolean isHttpEndpoint(String value) {
        if (!ContentUtil.isPathLike(value, true)) {
            return false;
        }
        return StringUtils.startsWithAny(value, "http://", "https://");
    }

    private static boolean isFile(String value) {
        if (!ContentUtil.isPathLike(value, false)) {
            return false;
        }
        File file = Paths.get(StringUtils.EMPTY).resolve(value).toFile();
        return file.exists();
    }
}
