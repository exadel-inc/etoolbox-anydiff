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
import com.exadel.etoolbox.anydiff.diff.DiffState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Implements {@link AbstractBlock} to represent the kind of disparity between two strings when one of them is missing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class MissBlockImpl extends AbstractBlock {

    private static final MarkedString MISSING = new MarkedString("Missing", Marker.CONTEXT);

    /* ---------
       Accessors
       --------- */

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public int getPendingCount() {
        return getLines().stream().anyMatch(line -> line.getPendingCount() > 0) ? 1 : 0;
    }

    @Override
    public DiffState getState() {
        return MISSING.equals(getLines().get(0).getLeftSide()) ? DiffState.LEFT_MISSING : DiffState.RIGHT_MISSING;
    }

    /* -------
       Content
       ------- */

    @Override
    public String getLeft(boolean includeContext) {
        return getLines().get(0).getLeft(false);
    }

    @Override
    public String getRight(boolean includeContext) {
        return getLines().get(0).getRight(false);
    }

    /* ----------
       Operations
       ---------- */

    @Override
    public void accept() {
        getLines().forEach(LineImpl::accept);
    }

    /* ------
       Output
       ------ */

    @Override
    public String toString(OutputType target) {
        if (target == OutputType.HTML) {
            return toHtml();
        } else {
            return toText(target);
        }
    }

    private String toHtml() {
        return HtmlTags
                .div().withClassAttr(Constants.CLASS_LEFT).withContent(getLeftLabel())
                .div().withClassAttr(Constants.CLASS_RIGHT).withContent(getRightLabel())
                .wrapIn(HtmlTags.line())
                .withClassAttr(Constants.CLASS_HEADER)
                .wrapIn(HtmlTags.section())
                .withClassAttr("no-highlight")
                .withContent(getLines().get(0).toString(OutputType.HTML))
                .toString();
    }

    private String toText(OutputType target) {
        int fullWidth = getColumnWidth() * 2 + 3;
        boolean hasLabels = StringUtils.isNoneBlank(getLeftLabel(), getRightLabel());

        StringBuilder builder = new StringBuilder();
        if (hasLabels) {
            String leftLabelTruncated = StringUtil.truncateMiddle(getLeftLabel(), getColumnWidth() - 2);
            String leftLabelCentered = StringUtils.center(leftLabelTruncated, getColumnWidth() + 1);
            String rightLabelTruncated = StringUtil.truncateMiddle(getRightLabel(), getColumnWidth() - 2);
            String rightLabelCentered = StringUtils.center(rightLabelTruncated, getColumnWidth() + 1);
            builder
                    .append(StringUtils.repeat(Constants.DASH_CHAR, fullWidth))
                    .append(StringUtils.LF).append(leftLabelCentered).append(Constants.PIPE).append(rightLabelCentered)
                    .append(StringUtils.LF).append(StringUtils.repeat(Constants.DASH_CHAR, fullWidth));
        } else {
            builder.append(StringUtils.repeat(Constants.DASH_CHAR, fullWidth));
        }
        builder.append(StringUtils.LF).append(getLines().get(0).toString(target));
        if (!hasLabels) {
            builder.append(StringUtils.repeat(Constants.DASH_CHAR, fullWidth));
        }
        return builder.toString();
    }

    /* ---------------
       Factory methods
       --------------- */

    /**
     * Creates a builder for constructing a new {@code MissBlockImpl} object with the left side missing
     * @param another The string to be used as the right side of the block
     * @return {@link Builder} instance
     */
    static Builder left(String another) {
        return new Builder(MISSING, new MarkedString(another, Marker.INSERT));
    }

    /**
     * Creates a builder for constructing a new {@code MissBlockImpl} object with the right side missing
     * @param another The string to be used as the left side of the block
     * @return {@link Builder} instance
     */
    static Builder right(String another) {
        return new Builder(new MarkedString(another, Marker.DELETE), MISSING);
    }

    /**
     * Constructs a new {@code MissBlockImpl} object
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class Builder {
        private final MarkedString left;
        private final MarkedString right;
        private String leftLabel;
        private String rightLabel;
        private int columnWidth;

        /**
         * Sets the label for the left side of the block
         * @param value String value. A non-null string is expected
         * @return This instance
         */
        Builder leftLabel(String value) {
            this.leftLabel = value;
            return this;
        }

        /**
         * Sets the label for the right side of the block
         * @param value String value. A non-null string is expected
         * @return This instance
         */
        Builder rightLabel(String value) {
            this.rightLabel = value;
            return this;
        }

        /**
         * Sets the column width for the block
         * @param value Integer value. A positive integer is expected
         * @return This instance
         */
        Builder columnWidth(int value) {
            this.columnWidth = value;
            return this;
        }

        /**
         * Creates a new {@code MissBlockImpl} object
         * @return {@link MissBlockImpl} instance
         */
        MissBlockImpl build() {
            MissBlockImpl miss = new MissBlockImpl();
            miss.setLeftLabel(leftLabel);
            miss.setRightLabel(rightLabel);
            miss.setColumnWidth(columnWidth);
            LineImpl line = new LineImpl(left, right);
            line.setBlock(miss);
            miss.getLines().add(line);
            return miss;
        }
    }
}
