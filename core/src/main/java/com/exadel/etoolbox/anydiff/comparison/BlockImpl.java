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
import com.exadel.etoolbox.anydiff.OutputType;
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.diff.DiffState;
import com.exadel.etoolbox.anydiff.diff.EntryHolder;
import com.exadel.etoolbox.anydiff.diff.Fragment;
import com.exadel.etoolbox.anydiff.diff.FragmentHolder;
import com.exadel.etoolbox.anydiff.diff.StructureEntry;
import com.github.difflib.text.DiffRow;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Represents a block of lines that contain a difference
 * @see DiffEntry
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
class BlockImpl extends AbstractBlock implements EntryHolder, StructureEntry, FragmentHolder {

    private String path;

    private boolean compactify;

    private ContentType contentType;

    private boolean ignoreSpaces;

    private int ellipsisPosition = -1;

    /* ---------
       Accessors
       --------- */

    @Override
    int getCount() {
        return getLines().stream().mapToInt(LineImpl::getCount).sum();
    }

    @Override
    int getPendingCount() {
        return getLines().stream().mapToInt(LineImpl::getPendingCount).sum();
    }

    @Override
    public DiffState getState() {
        if (getLines().isEmpty() || getLines().stream().allMatch(line -> line.getState() == DiffState.UNCHANGED)) {
            return DiffState.UNCHANGED;
        }
        if (getLines().stream().anyMatch(line -> line.getState() == DiffState.CHANGE)) {
            return DiffState.CHANGE;
        }
        if (getLines().stream().allMatch(line -> line.getState() == DiffState.LEFT_MISSING || line.getState() == DiffState.UNCHANGED)) {
            return DiffState.LEFT_MISSING;
        }
        if (getLines().stream().allMatch(line -> line.getState() == DiffState.RIGHT_MISSING || line.getState() == DiffState.UNCHANGED)) {
            return DiffState.RIGHT_MISSING;
        }
        return DiffState.CHANGE;
    }

    @Override
    public String getPath() {
        return path;
    }

    /* -------
       Content
       ------- */

    @Override
    public List<? extends DiffEntry> children() {
        return getLines();
    }

    @Override
    public String getLeft(boolean includeContext) {
        return getLines()
                .stream()
                .filter(line -> includeContext || !line.isContext())
                .map(line -> line.getLeft(includeContext))
                .collect(Collectors.joining(StringUtils.LF));
    }

    @Override
    public String getRight(boolean includeContext) {
        return getLines()
                .stream()
                .filter(line -> includeContext || !line.isContext())
                .map(line -> line.getRight(includeContext))
                .collect(Collectors.joining(StringUtils.LF));
    }

    @Override
    public List<Fragment> getLeftFragments() {
        return getLines()
                .stream()
                .flatMap(line -> line.getLeftFragments().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<Fragment> getRightFragments() {
        return getLines()
                .stream()
                .flatMap(line -> line.getRightFragments().stream())
                .collect(Collectors.toList());
    }

    /* --------------
       Adding entries
       -------------- */

    /**
     * Adds a line to the block
     * @param value {@link DiffRow} value that is subsequently converted to a line
     */
    void add(DiffRow value) {
        if (value.getTag() == DiffRow.Tag.CHANGE) {
            add(new LineImpl(
                    new MarkedString(value.getOldLine(), ignoreSpaces, compactify),
                    new MarkedString(value.getNewLine(), ignoreSpaces, compactify)));
        } else if (value.getTag() == DiffRow.Tag.DELETE) {
            add(new LineImpl(
                    new MarkedString(value.getOldLine(), ignoreSpaces, compactify),
                    null));
        } else if (value.getTag() == DiffRow.Tag.INSERT) {
            add(new LineImpl(
                    null,
                    new MarkedString(value.getNewLine(), ignoreSpaces, compactify)));
        }
    }

    /**
     * Adds a line to the block
     * @param value {@link LineImpl} value representing a diff line
     */
    void add(LineImpl value) {
        value.setBlock(this);
        if (contentType == ContentType.HTML || contentType == ContentType.XML) {
            value.setIsMarkup();
        }
        LineImpl lastLine = !getLines().isEmpty() ? getLines().get(getLines().size() - 1) : null;
        if (lastLine != null) {
            value.getLeftSide().setPrevious(lastLine.getLeftSide());
            value.getRightSide().setPrevious(lastLine.getRightSide());
            lastLine.getLeftSide().setNext(value.getLeftSide());
            lastLine.getRightSide().setNext(value.getRightSide());
        }
        getLines().add(value);
    }

    /**
     * Adds a context line (the one that does not expose a difference itself but helps to visualize the position of the
     * difference in the content) to the block
     * @param value {@link DiffRow} value that is subsequently converted to a line
     */
    void addContext(DiffRow value) {
        addContext(Collections.singletonList(value));
    }

    /**
     * Adds context lines (the ones that do not expose a difference itself but help to visualize the position of the
     * difference in the content) to the block
     * @param value List of {@link DiffRow} values that are subsequently converted to lines
     */
    void addContext(List<DiffRow> value) {
        if (CollectionUtils.isEmpty(value)) {
            return;
        }
        for (DiffRow diffRow : value) {
            if (Marker.ELLIPSIS.equals(diffRow.getOldLine()) && Marker.ELLIPSIS.equals(diffRow.getNewLine())) {
                ellipsisPosition = getLines().size();
                continue;
            }
            LineImpl newLine = new LineImpl(
                    new MarkedString(diffRow.getOldLine(), ignoreSpaces, compactify),
                    new MarkedString(diffRow.getNewLine(), ignoreSpaces, compactify));
            newLine.setIsContext();
            add(newLine);
        }
    }

    /* ------------------
       Discarding entries
       ------------------ */

    @Override
    public void accept() {
        getLines().forEach(LineImpl::accept);
    }

    @Override
    public void accept(Fragment value) {
        getLines().forEach(line -> line.accept(value));
    }

    @Override
    public void exclude(DiffEntry value) {
        if (value instanceof LineImpl && getLines().contains(value)) {
            ((LineImpl) value).setIsContext();
        } else if (value instanceof FragmentPairImpl) {
            getLines().forEach(line -> line.exclude(value));
        }
    }

    @Override
    public void exclude(Fragment value) {
        getLines().forEach(line -> line.exclude(value));
    }

    /* ------
       Output
       ------ */

    @Override
    public String toString(OutputType target) {
        if (getLines().isEmpty()) {
            return StringUtils.EMPTY;
        }
        compactSpaces();
        if (target == OutputType.HTML) {
            return toHtml();
        } else {
            return toText(target);
        }
    }

    private String toHtml() {
        String path = HtmlTags.div().withClassAttr(Constants.CLASS_PATH).withContent(this.path).toString();
        String header = HtmlTags
                .div().withClassAttr(Constants.CLASS_LEFT).withContent(getLeftLabel())
                .div().withClassAttr(Constants.CLASS_RIGHT).withContent(getRightLabel())
                .wrapIn(HtmlTags.line())
                .withClassAttr(Constants.CLASS_HEADER)
                .toString();
        StringBuilder lineBuilder = new StringBuilder();
        int position = 0;
        for (LineImpl line : getLines()) {
            if (position++ == ellipsisPosition) {
                lineBuilder.append(HtmlTags.line().withClassAttr("ellipsis").withContent(Constants.ELLIPSIS));
            }
            lineBuilder.append(line.toString(OutputType.HTML));
        }
        return HtmlTags.section().withContent(path).withContent(header).withContent(lineBuilder).toString();
    }

    private String toText(OutputType target) {
        int fullWidth = (getColumnWidth() + 1) * 2 + 1;
        StringBuilder builder = new StringBuilder().append(StringUtils.LF);
        if (StringUtils.isNotBlank(path)) {
            String truncatedPath = StringUtil.truncateRight(path, (fullWidth - 4) / 4 * 3);
            String delimiter = StringUtils.leftPad(
                    truncatedPath + "----",
                    fullWidth,
                    Constants.DASH_CHAR);
            if (target == OutputType.CONSOLE) {
                MarkedString coloredDelimiter = new MarkedString(delimiter);
                coloredDelimiter.mark(truncatedPath, Marker.CONTEXT);
                delimiter = coloredDelimiter.toText(OutputType.CONSOLE, fullWidth).get(0);
            }
            builder.append(delimiter);
        } else {
            builder.append(StringUtils.repeat(Constants.DASH_CHAR, fullWidth));
        }

        String leftTruncatedLabel = StringUtil.truncateMiddle(getLeftLabel(), getColumnWidth() - 2);
        String rightTruncatedLabel = StringUtil.truncateMiddle(getRightLabel(), getColumnWidth() - 2);
        builder
                .append(StringUtils.LF)
                .append(String.format(
                        "%s|%s",
                        StringUtils.center(leftTruncatedLabel, getColumnWidth() + 1),
                        StringUtils.center(rightTruncatedLabel, getColumnWidth() + 1)));

        builder.append(StringUtils.LF).append(StringUtils.repeat(Constants.DASH_CHAR, fullWidth));

        int position = 0;
        for (LineImpl line : getLines()) {
            if (position++ == ellipsisPosition) {
                builder
                        .append(StringUtils.LF)
                        .append(StringUtils.center(Constants.ELLIPSIS, fullWidth));
            }
            builder.append(StringUtils.LF).append(line.toString(target));
        }
        return builder.toString();
    }

    private void compactSpaces() {
        int minIndent = getLines().stream().mapToInt(LineImpl::getIndent).min().orElse(0);
        if (minIndent > 0) {
            getLines().forEach(line -> line.cutLeft(minIndent));
        }
    }

    /* -------------
       Factory logic
       ------------- */

    /**
     * Creates a builder for constructing a new {@code BlockImpl} object
     * @return {@link Builder} instance
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Constructs a new {@code BlockImpl} instance
     */
    static class Builder extends AbstractBlock.Builder {
        private boolean compactify;
        private ContentType contentType;
        private boolean ignoreSpaces;
        private String path;

        /**
         * Sets whether white spaces will be ignored when performing comparison over fragments of the block
         * @param value True or false
         * @return This instance
         */
        Builder ignoreSpaces(boolean value) {
            this.ignoreSpaces = value;
            return this;
        }

        /**
         * Sets whether the block content should be compacted before printing out
         * @param value True or false
         * @return This instance
         */
        Builder compactify(boolean value) {
            this.compactify = value;
            return this;
        }

        /**
         * Sets the content type of the block
         * @param value {@link ContentType} value
         * @return This instance
         */
        Builder contentType(ContentType value) {
            this.contentType = value;
            return this;
        }

        /**
         * Sets the optional path used to identify the current block in the content structure
         * @param value A string value. A non-empty string is expected
         * @return This instance
         * @see StructureEntry#getPath()
         */
        Builder path(String value) {
            this.path = value;
            return this;
        }

        /**
         * Builds a new {@code BlockImpl} instance using the provided constructor
         * @param constructor A {@link Supplier} that provides an instance of {@code BlockImpl}
         * @param <T>         A type that extends {@code BlockImpl}
         * @return An instance of {@code BlockImpl}
         */
        @Override
        <T extends AbstractBlock> T build(Supplier<T> constructor) {
            T block = super.build(constructor);
            ((BlockImpl) block).path = path;
            ((BlockImpl) block).compactify = compactify;
            ((BlockImpl) block).contentType = contentType;
            ((BlockImpl) block).ignoreSpaces = ignoreSpaces;
            return block;
        }
    }
}
