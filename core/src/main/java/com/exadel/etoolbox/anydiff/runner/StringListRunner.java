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
import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.util.ContentUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends {@link DiffRunner} to implement extracting data from string arrays
 */
@RequiredArgsConstructor
class StringListRunner extends DiffRunner {

    private final String[] left;
    private final String[] right;

    @Override
    public List<Diff> runInternal() {
        int commonLength = Math.min(ArrayUtils.getLength(left), ArrayUtils.getLength(right));
        List<Diff> result = new ArrayList<>();

        for (int i = 0; i < commonLength; i++) {
            String leftPart = left[i];
            String rightPart = right[i];

            DiffRunner diffRunner = DiffRunner.forValues(
                    ContentUtil.extractComparable(left[i]),
                    ContentUtil.extractLabel(
                            leftPart,
                            !Constants.LABEL_LEFT.equals(getLeftLabel()) ? getLeftLabel() :StringUtils.EMPTY),
                    ContentUtil.extractComparable(right[i]),
                    ContentUtil.extractLabel(
                            rightPart,
                            !Constants.LABEL_RIGHT.equals(getRightLabel()) ? getRightLabel() :StringUtils.EMPTY))
                    .withContentType(getContentType())
                    .withEntryFilter(getEntryFilter())
                    .withTaskParameters(getTaskParameters());
            result.addAll(diffRunner.run());
        }
        for (int i = commonLength; i < left.length; i++) {
            String leftLine = left[i];
            result.add(reportRightMissing(leftLine, StringUtils.EMPTY));
        }
        for (int i = commonLength; i < right.length; i++) {
            String rightLine = right[i];
            result.add(reportLeftMissing(StringUtils.EMPTY, rightLine));
        }
        return result;
    }
}
