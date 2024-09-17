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
import com.exadel.etoolbox.anydiff.comparison.postprocessor.Postprocessor;
import com.exadel.etoolbox.anydiff.comparison.preprocessor.Preprocessor;
import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.diff.DiffState;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Runs a comparison with the specified parameters and produces a {@link Diff} object
 * <u>Note</u>: This class is not a part of public API and is subject to change. You should not use it directly
 */
@Builder(builderClassName = "Builder")
public class DiffTask {

    private static final UnaryOperator<String> EMPTY_NORMALIZER = StringUtils::defaultString;

    private final ContentType contentType;

    private final String leftId;
    private String leftLabel;
    private final Object leftContent;

    private final String rightId;
    private String rightLabel;
    private final Object rightContent;

    private final Predicate<DiffEntry> filter;

    private DiffState anticipatedState;

    private TaskParameters taskParameters;

    /* ------------------
       Property accessors
       ------------------ */

    private Preprocessor getPreprocessor(String contentId) {
        return Preprocessor.forType(contentType, taskParameters).withContentId(contentId);
    }

    private Postprocessor getPostprocessor() {
        return Postprocessor.forType(contentType, taskParameters);
    }

    /* ---------
       Execution
       --------- */

    /**
     * Performs the comparison between the left and right content
     * @return {@link Diff} object
     */
    public Diff run() {
        if (Objects.equals(leftContent, rightContent)) {
            return new DiffImpl(leftId, rightId); // This object will report the "equals" state
        }
        int columnWidth = taskParameters.getColumnWidth() - 1;
        if (anticipatedState == DiffState.CHANGE) {
            BlockImpl change = BlockImpl
                    .builder()
                    .leftLabel(leftLabel)
                    .rightLabel(rightLabel)
                    .columnWidth(columnWidth)
                    .lines(Collections.singletonList(new LineImpl(
                            new MarkedString(String.valueOf(leftContent)).markPlaceholders(Marker.DELETE),
                            new MarkedString(String.valueOf(rightContent)).markPlaceholders(Marker.INSERT)
                    )))
                    .build(DisparityBlockImpl::new);
            return new DiffImpl(leftId, rightId).withChildren(change);
        }
        if (leftContent == null || anticipatedState == DiffState.LEFT_MISSING) {
            MissBlockImpl miss = MissBlockImpl
                    .left(rightId)
                    .leftLabel(leftLabel)
                    .rightLabel(rightLabel)
                    .columnWidth(columnWidth)
                    .build();
            return new DiffImpl(leftId, rightId).withChildren(miss);
        }
        if (rightContent == null || anticipatedState == DiffState.RIGHT_MISSING) {
            MissBlockImpl miss = MissBlockImpl
                    .right(leftId)
                    .leftLabel(leftLabel)
                    .rightLabel(rightLabel)
                    .columnWidth(columnWidth)
                    .build();
            return new DiffImpl(leftId, rightId).withChildren(miss);
        }
        return contentType == ContentType.UNDEFINED ? runForBinary() : runForText();
    }

    private Diff runForBinary() {
        DiffImpl result = new DiffImpl(leftId, rightId);
        DisparityBlockImpl disparity = DisparityBlockImpl
                .builder()
                .leftContent(leftContent)
                .rightContent(rightContent)
                .columnWidth(taskParameters.getColumnWidth() - 1)
                .leftLabel(leftLabel)
                .rightLabel(rightLabel)
                .build(DisparityBlockImpl::new);
        return result.withChildren(disparity);
    }

    private Diff runForText() {
        DiffRowGenerator generator = DiffRowGenerator
                .create()
                .ignoreWhiteSpaces(taskParameters.ignoreSpaces())
                .showInlineDiffs(true)
                .oldTag(isStart -> isStart ? Marker.DELETE.toString() : Marker.RESET.toString())
                .newTag(isStart -> isStart ? Marker.INSERT.toString() : Marker.RESET.toString())
                .lineNormalizer(EMPTY_NORMALIZER) // One needs this to override the OOTB preprocessor that spoils HTML
                .inlineDiffBySplitter(SplitterUtil::getTokens)
                .build();
        String leftPreprocessed = getPreprocessor(leftId).apply(leftContent.toString());
        String rightPreprocessed = getPreprocessor(rightId).apply(rightContent.toString());
        List<String> leftLines = StringUtil.splitByNewline(leftPreprocessed);
        List<String> rightLines = StringUtil.splitByNewline(rightPreprocessed);
        List<DiffRow> diffRows = getPostprocessor().apply(generator.generateDiffRows(leftLines, rightLines));

        DiffImpl result = new DiffImpl(leftId, rightId);
        List<AbstractBlock> blocks = getDiffBlocks(diffRows)
                .stream()
                .peek(block -> block.setDiff(result))
                .filter(filter != null ? filter : entry -> true)
                .collect(Collectors.toList());
        return result.withChildren(blocks);
    }

    private List<AbstractBlock> getDiffBlocks(List<DiffRow> allRows) {
        List<AbstractBlock> result = new ArrayList<>();
        BlockImpl pendingDiffBlock = null;
        for (int i = 0; i < allRows.size(); i++) {
            DiffRow row = allRows.get(i);
            boolean isNeutralRow = row.getTag() == DiffRow.Tag.EQUAL;
            boolean isNonNeutralStreakEnding = isNeutralRow && pendingDiffBlock != null;
            if (isNonNeutralStreakEnding) {
                pendingDiffBlock.addContext(row);
                result.add(pendingDiffBlock);
                pendingDiffBlock = null;
            }
            if (isNeutralRow) {
                continue;
            }
            if (pendingDiffBlock == null) {
                List<DiffRow> lookbehindContext = getLookbehindContext(allRows, i);
                pendingDiffBlock = BlockImpl
                        .builder()
                        .path(getContextPath(allRows, i))
                        .compactify(taskParameters.normalize())
                        .contentType(contentType)
                        .ignoreSpaces(taskParameters.ignoreSpaces())
                        .leftLabel(leftLabel)
                        .rightLabel(rightLabel)
                        .columnWidth(taskParameters.getColumnWidth() - 1)
                        .build(BlockImpl::new);
                pendingDiffBlock.addContext(lookbehindContext);
            }
            pendingDiffBlock.add(row);
        }
        if (pendingDiffBlock != null) {
            result.add(pendingDiffBlock);
        }
        return result;
    }

    private String getContextPath(List<DiffRow> allRows, int position) {
        PathHelper pathHelper = PathHelper.forType(contentType);
        if (pathHelper == null) {
            return StringUtils.EMPTY;
        }
        return pathHelper.getPath(allRows, position);
    }

    private List<DiffRow> getLookbehindContext(List<DiffRow> allRows, int position) {
        if (position == 0) {
            return null;
        }

        PathHelper pathHelper = PathHelper.forType(contentType);
        if (pathHelper == null) {
            return Collections.singletonList(allRows.get(position - 1));
        }

        int nearestTagRowIndex = pathHelper.getPrecedingTagRowIndex(allRows, position - 1);
        if (nearestTagRowIndex >= 0) {
            return truncateContext(allRows.subList(nearestTagRowIndex, position));
        }
        return Collections.singletonList(allRows.get(position - 1));
    }

    private static List<DiffRow> truncateContext(List<DiffRow> rows) {
        if (rows.size() <= Constants.MAX_CONTEXT_LENGTH) {
            return rows;
        }
        List<DiffRow> upperPart = rows.subList(0, Constants.MAX_CONTEXT_LENGTH / 2);
        List<DiffRow> lowerPart = rows.subList(rows.size() - Constants.MAX_CONTEXT_LENGTH / 2, rows.size());
        List<DiffRow> result = new ArrayList<>(upperPart);
        result.add(new DiffRow(DiffRow.Tag.EQUAL, Marker.ELLIPSIS, Marker.ELLIPSIS));
        result.addAll(lowerPart);
        return result;
    }

    /**
     * Creates a builder for constructing a new {@code DiffTask} object
     * @return {@code DiffTask.Builder} instance
     */
    public static Builder builder() {
        return new InitializingBuilder();
    }

    /**
     * Constructs a new {@code DiffTask} instance with critical values initialized
     */
    private static class InitializingBuilder extends DiffTask.Builder {
        @Override
        public DiffTask build() {
            DiffTask result = super.build();

            TaskParameters perRequest = result.taskParameters;
            TaskParameters perContentType = TaskParameters.from(result.contentType);
            result.taskParameters = TaskParameters.merge(perContentType, perRequest);

            if (result.anticipatedState == null) {
                result.anticipatedState = DiffState.UNCHANGED;
            }

            result.leftLabel = StringUtils.defaultIfBlank(result.leftLabel, Constants.LABEL_LEFT);
            result.rightLabel = StringUtils.defaultIfBlank(result.rightLabel, Constants.LABEL_RIGHT);

            return result;
        }
    }
}
