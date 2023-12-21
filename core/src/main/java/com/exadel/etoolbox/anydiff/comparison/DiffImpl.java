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
import com.exadel.etoolbox.anydiff.OutputType;
import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.diff.DiffState;
import com.exadel.etoolbox.anydiff.diff.PrintableEntry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implements {@link Diff} to represent an aggregate difference between two strings
 * @see Diff
 */
@RequiredArgsConstructor
class DiffImpl implements Diff, PrintableEntry {

    private static final String ARCHIVE_EXTENSION = ".zip/";

    /**
     * Gets the identifier of the left part of the comparison (such as a file name or a) when available
     */
    @Getter
    private final String left;

    /**
     * Gets the identifier of the right part of the comparison (such as a file name or a) when available
     */
    @Getter
    private final String right;

    private List<AbstractBlock> children = Collections.emptyList();

    /* ---------------
       State accessors
       --------------- */

    @Override
    public DiffState getState() {
        if (CollectionUtils.isEmpty(children) || children.stream().allMatch(e -> e.getState() == DiffState.UNCHANGED)) {
            return DiffState.UNCHANGED;
        }
        if (children.stream().allMatch(e -> e.getState() == DiffState.LEFT_MISSING)) {
            return DiffState.LEFT_MISSING;
        }
        if (children.stream().allMatch(e -> e.getState() == DiffState.RIGHT_MISSING)) {
            return DiffState.RIGHT_MISSING;
        }
        return DiffState.CHANGE;
    }

    @Override
    public int getCount() {
        if (getState() == DiffState.UNCHANGED) {
            return 0;
        }
        return children.stream().mapToInt(AbstractBlock::getCount).sum();
    }

    @Override
    public int getPendingCount() {
        if (getState() == DiffState.UNCHANGED) {
            return 0;
        }
        return children.stream().mapToInt(AbstractBlock::getPendingCount).sum();
    }

    /* -------
       Content
       ------- */

    @Override
    public List<? extends DiffEntry> children() {
        return children;
    }

    /**
     * Appends child blocks to the current instance
     * @param children One or more descendants of {@link AbstractBlock}
     * @return This instance
     */
    DiffImpl withChildren(AbstractBlock... children) {
        if (ArrayUtils.isEmpty(children)) {
            return this;
        }
        return withChildren(Arrays.asList(children));
    }

    /**
     * Appends child blocks to the current instance
     * @param children List of {@link AbstractBlock} descendants. A non-empty list is expected
     * @return This instance
     */
    DiffImpl withChildren(List<AbstractBlock> children) {
        this.children = children;
        if (CollectionUtils.isNotEmpty(children)) {
            children.forEach(c -> c.setDiff(this));
        }
        return this;
    }

    /* ------------------
       Discarding entries
       ------------------ */

    @Override
    public void exclude(DiffEntry value) {
        if (!(value instanceof AbstractBlock)) {
            return;
        }
        try {
            children.remove(value);
        } catch (UnsupportedOperationException e) {
            children = new ArrayList<>(children);
            children.remove(value);
        }
    }

    /* ------
       Output
       ------ */

    @Override
    public String toString() {
        return toString(OutputType.LOG);
    }

    @Override
    public String toString(OutputType target) {
        if (target == OutputType.HTML) {
            return toHtml();
        }
        return toText(target);
    }

    @Override
    public String toString(OutputType target, String element) {
        if (target == OutputType.HTML && "toc".equals(element)) {
            return HtmlTags
                    .a()
                    .withAttr(Constants.ATTR_HREF, Constants.HASH + getHtmlId())
                    .withContent(getHtmlLabel())
                    .wrapIn("li")
                    .toString();
        }
        return toString(target);
    }

    private String toHtml() {
        StringBuilder builder = new StringBuilder();
        String headerAnchor = HtmlTags
                .a()
                .withAttr(Constants.ATTR_ID, getHtmlId())
                .toString();
        String header = HtmlTags
                .a()
                .withContent("↑")
                .withAttr(Constants.ATTR_HREF, "#toc")
                .withClassAttr("${toc-class}")
                .wrapIn(HtmlTags.h4())
                .withContent(getHtmlLabel(), true)
                .toString();
        builder.append(headerAnchor).append(header);
        if (CollectionUtils.isEmpty(children)) {
            return builder.toString();
        }
        for (DiffEntry diffEntry : children) {
            if (diffEntry instanceof PrintableEntry) {
                builder.append(((PrintableEntry) diffEntry).toString(OutputType.HTML));
            }
        }
        return builder.toString();
    }

    private String toText(OutputType target) {
        return children
                .stream()
                .filter(Objects::nonNull)
                .map(PrintableEntry.class::cast)
                .map(entry -> entry.toString(target).trim())
                .collect(Collectors.joining(StringUtils.LF + StringUtils.LF)) +
                StringUtils.LF;
    }

    private String getHtmlId() {
        return StringUtils.isNoneEmpty(getLeft(), getRight()) && !getLeft().equals(getRight())
                ? getLeft() + "-vs-" + getRight()
                : StringUtils.firstNonEmpty(getLeft(), getRight());
    }

    private String getHtmlLabel() {
        String leftLabel = stripArchiveLabelPrefix(getLeft());
        String rightLabel = stripArchiveLabelPrefix(getRight());
        if (StringUtils.isNoneEmpty(leftLabel, rightLabel) && !leftLabel.equals(rightLabel)) {
            return HtmlTags
                    .span().withClassAttr(Constants.CLASS_LEFT).withContent(leftLabel)
                    .text(" vs ")
                    .span().withClassAttr(Constants.CLASS_RIGHT).withContent(rightLabel)
                    .toString();
        }
        return HtmlTags
                .span()
                .withClassAttr("center")
                .withContent(StringUtils.firstNonEmpty(leftLabel, rightLabel))
                .toString();
    }

    private static String stripArchiveLabelPrefix(String value) {
        return StringUtils.contains(value, ARCHIVE_EXTENSION)
                ? StringUtils.substringAfter(value, ARCHIVE_EXTENSION)
                : value;
    }
}
