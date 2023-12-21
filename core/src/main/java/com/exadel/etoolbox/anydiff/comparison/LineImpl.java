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
import com.exadel.etoolbox.anydiff.diff.EntryHolder;
import com.exadel.etoolbox.anydiff.diff.Fragment;
import com.exadel.etoolbox.anydiff.diff.FragmentHolder;
import com.exadel.etoolbox.anydiff.diff.PrintableEntry;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Implements {@link DiffEntry} to represent a line within a block of text that either contains a difference or poses
 * a visual context
 * @see DiffEntry
 */
class LineImpl implements DiffEntry, FragmentHolder, EntryHolder, PrintableEntry {

    private static final MarkedString CONTEXT_ELLIPSIS = new MarkedString(Constants.ELLIPSIS, Marker.CONTEXT);

    private final MarkedString left;

    private final MarkedString right;

    /**
     * Gets whether the current line is a context line
     */
    @Getter(value = lombok.AccessLevel.PACKAGE)
    private boolean isContext;

    private boolean isMarkupType;

    private List<Fragment> leftFragments;
    private List<Fragment> rightFragments;
    private List<FragmentPairImpl> fragmentPairs;

    /**
     * Assigns the reference to the block this line belongs to
     */
    @Setter(value = lombok.AccessLevel.PACKAGE)
    private AbstractBlock block;

    /**
     * Initializes a new instance of {@link LineImpl} class
     * @param left  The left side of the line
     * @param right The right side of the line
     */
    LineImpl(MarkedString left, MarkedString right) {
        this.left = left != null ? left : new MarkedString(null);
        this.right = right != null ? right : new MarkedString(null);
    }

    /* ---------------
       State accessors
       --------------- */

    /**
     * Gets the number of differences in the current line
     * @return An integer value
     */
    int getCount() {
        if (getState() == DiffState.UNCHANGED) {
            return 0;
        }
        // Will sum up to X "unpaired" fragments + Y pairs (every pair count as 1)
        return getLeftFragments().size() + getRightFragments().size() - children().size();
    }

    int getPendingCount() {
        if (getState() == DiffState.UNCHANGED) {
            return 0;
        }
        // Will sum up to X "unpaired" fragments + Y pairs (every pair count as 1)
        long result = getLeftFragments().stream().filter(Fragment::isPending).count() +
                        getRightFragments().stream().filter(Fragment::isPending).count() -
                        children().stream().filter(c -> ((FragmentPairImpl) c).isPending()).count();
        return (int) result;
    }

    @Override
    public DiffState getState() {
        if (isContext) {
            return DiffState.UNCHANGED;
        }
        if (MarkedString.isEmpty(left) && !MarkedString.isEmpty(right)) {
            return right.hasChanges() ? DiffState.LEFT_MISSING : DiffState.UNCHANGED;
        }
        if (!MarkedString.isEmpty(left) && MarkedString.isEmpty(right)) {
            return left.hasChanges() ? DiffState.RIGHT_MISSING : DiffState.UNCHANGED;
        }
        if (MarkedString.isEmpty(left)) {
            return DiffState.UNCHANGED;
        }
        return left.hasChanges() || right.hasChanges() ? DiffState.CHANGE : DiffState.UNCHANGED;
    }

    /* ---------------
       Other accessors
       --------------- */

    @Override
    public Diff getDiff() {
        return block != null ? block.getDiff() : null;
    }

    /**
     * Calculates the number of spaces that indent both the left and right sides of the current line
     * @return An integer value
     */
    int getIndent() {
        int leftIndent = left.getIndent();
        int rightIndent = right.getIndent();
        if (leftIndent == 0 && left.toString().isEmpty()) {
            return rightIndent;
        } else if (rightIndent == 0 && right.toString().isEmpty()) {
            return leftIndent;
        }
        return Math.min(leftIndent, rightIndent);
    }

    /**
     * Sets the flag saying that the current line is a context line
     */
    void setIsContext() {
        isContext = true;
        left.mark(Marker.CONTEXT);
        right.mark(Marker.CONTEXT);
    }

    /**
     * Sets the flag saying that the current line contains XML or HTML markup
     */
    void setIsMarkup() {
        isMarkupType = true;
    }

    /**
     * Removes the specified number of characters from the left side of the current line. Used to trim leading spaces
     * and provide a more compact output
     */
    void cutLeft(int count) {
        if (count == 0) {
            return;
        }
        left.cutLeft(count);
        right.cutLeft(count);
    }

    private int getColumnWidth() {
        return block != null ? block.getColumnWidth() : Constants.DEFAULT_COLUMN_WIDTH;
    }

    /* -------
       Content
       ------- */

    @Override
    public List<? extends DiffEntry> children() {
        if (fragmentPairs == null) {
            initFragmentsCache();
        }
        return fragmentPairs;
    }

    @Override
    public String getLeft(boolean includeContext) {
        if (!includeContext && isContext) {
            return StringUtils.EMPTY;
        }
        return left.toString();
    }

    @Override
    public String getRight(boolean includeContext) {
        if (!includeContext && isContext) {
            return StringUtils.EMPTY;
        }
        return right.toString();
    }

    @Override
    public List<Fragment> getLeftFragments() {
        if (leftFragments == null) {
            initFragmentsCache();
        }
        return leftFragments;
    }

    @Override
    public List<Fragment> getRightFragments() {
        if (rightFragments == null) {
            initFragmentsCache();
        }
        return rightFragments;
    }

    /**
     * Gets the left side of the current line
     * @return A {@link MarkedString} instance
     */
    MarkedString getLeftSide() {
        return left;
    }

    /**
     * Gets the right side of the current line
     * @return A {@link MarkedString} instance
     */
    MarkedString getRightSide() {
        return right;
    }

    private void initFragmentsCache() {
        leftFragments = isMarkupType ? left.getMarkupFragments() : left.getFragments();
        rightFragments = isMarkupType ? right.getMarkupFragments() : right.getFragments();
        if (leftFragments.size() != rightFragments.size()
                || leftFragments.isEmpty()
                || ((FragmentImpl) leftFragments.get(0)).getLineOffset() != ((FragmentImpl) rightFragments.get(0)).getLineOffset()) {
            fragmentPairs = Collections.emptyList();
            return;
        }
        Iterator<Fragment> leftIterator = leftFragments.iterator();
        Iterator<Fragment> rightIterator = rightFragments.iterator();
        fragmentPairs = new ArrayList<>();
        while (leftIterator.hasNext()) {
            Fragment leftFragment = leftIterator.next();
            Fragment rightFragment = rightIterator.next();
            fragmentPairs.add(new FragmentPairImpl(this, leftFragment, rightFragment));
        }
    }

    private void resetFragmentsCache() {
        leftFragments = null;
        rightFragments = null;
        fragmentPairs = null;
    }

    /* ----------
       Operations
       ---------- */

    @Override
    public void accept() {
        left.accept();
        right.accept();
        resetFragmentsCache();
    }

    @Override
    public void accept(Fragment value) {
        left.accept(value);
        right.accept(value);
        resetFragmentsCache();
    }

    @Override
    public void exclude(DiffEntry value) {
        if (!(value instanceof FragmentPairImpl)) {
            return;
        }
        FragmentPairImpl fragmentPair = (FragmentPairImpl) value;
        left.unmark(fragmentPair.getLeftFragment());
        right.unmark(fragmentPair.getRightFragment());
        resetFragmentsCache();
    }

    @Override
    public void exclude(Fragment value) {
        left.unmark(value);
        right.unmark(value);
        resetFragmentsCache();
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
                .div().withClassAttr(Constants.CLASS_LEFT).withContent(ellipsizeHtmlOutput(left.toHtml()))
                .div().withClassAttr(Constants.CLASS_RIGHT).withContent(ellipsizeHtmlOutput(right.toHtml()))
                .wrapIn(HtmlTags.line())
                .toString();
    }

    private String toText(OutputType target) {
        List<String> leftColumn = left.toText(target, getColumnWidth());
        List<String> rightColumn = right.toText(target, getColumnWidth());
        ellipsizeTextOutput(leftColumn, target);
        ellipsizeTextOutput(rightColumn, target);
        alignTextOutput(leftColumn, rightColumn, getColumnWidth());
        StringBuilder builder = new StringBuilder();
        for (int j = 0; j < leftColumn.size(); j++) {
            if (j > 0) {
                builder.append(StringUtils.LF);
            }
            builder.append(String.format(
                    "%s | %s",
                    leftColumn.get(j),
                    rightColumn.get(j)));
        }
        return builder.toString();
    }

    private void ellipsizeTextOutput(List<String> value, OutputType target) {
        if (!isContext || value.size() <= Constants.MAX_CONTEXT_LENGTH) {
            return;
        }
        int chunkLength = Constants.MAX_CONTEXT_LENGTH / 2;
        int cutLength = value.size() - Constants.MAX_CONTEXT_LENGTH;
        value.add(chunkLength, CONTEXT_ELLIPSIS.toText(target, getColumnWidth()).get(0));
        value.subList(chunkLength + 1, chunkLength + 1 + cutLength).clear();
    }

    private String ellipsizeHtmlOutput(String value) {
        if (!isContext) {
            return value;
        }
        return StringUtil.truncateMiddle(
                value,
                Constants.MAX_CONTEXT_LENGTH * getColumnWidth() * 2,
                "&lt;...&gt;");
    }

    private static void alignTextOutput(List<String> left, List<String> right, int width) {
        while (left.size() < right.size()) {
            left.add(StringUtils.repeat(StringUtils.SPACE, width));
        }
        while (right.size() < left.size()) {
            right.add(StringUtils.EMPTY);
        }
    }
}
