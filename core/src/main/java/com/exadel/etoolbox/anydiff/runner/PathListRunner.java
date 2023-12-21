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

import com.exadel.etoolbox.anydiff.diff.Diff;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extends {@link DiffRunner} to implement extracting data from file system directories given as {@link Path} arrays
 */
@RequiredArgsConstructor
class PathListRunner extends DiffRunner {

    private final Path[] left;
    private final Path[] right;

    @Override
    public List<Diff> runInternal() {
        int commonLength = Math.min(ArrayUtils.getLength(left), ArrayUtils.getLength(right));
        List<Diff> result = new ArrayList<>();

        for (int i = 0; i < commonLength; i++) {
            Path leftPart = left[i];
            Path rightPart = right[i];

            DiffRunner diffRunner = forValues(
                    leftPart,
                    getLeftLabel(),
                    rightPart,
                    getRightLabel())
                    .withContentType(getContentType())
                    .withEntryFilter(getEntryFilter())
                    .withTaskParameters(getTaskParameters());
            result.addAll(diffRunner.run());
        }
        for (int i = commonLength; i < left.length; i++) {
            result.add(reportRightMissing(left[i].toAbsolutePath().toString(), StringUtils.EMPTY));
        }
        for (int i = commonLength; i < right.length; i++) {
            result.add(reportLeftMissing(StringUtils.EMPTY, right[i].toAbsolutePath().toString()));
        }
        return result;
    }
}
