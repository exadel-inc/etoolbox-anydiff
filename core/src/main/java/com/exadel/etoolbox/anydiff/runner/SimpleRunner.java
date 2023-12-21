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

import com.exadel.etoolbox.anydiff.ContentType;
import com.exadel.etoolbox.anydiff.comparison.DiffTask;
import com.exadel.etoolbox.anydiff.diff.Diff;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Extends {@link DiffRunner} to implement extracting data from text strings
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor
class SimpleRunner extends DiffRunner {

    private String left;
    private String right;

    @Override
    public List<Diff> runInternal() {
        if (StringUtils.equals(left, right)) {
            return Collections.singletonList(DiffTask.builder().build().run()); // This will produce an "equals" diff
        }
        ContentType effectiveContentType = getContentType() == null || getContentType() == ContentType.UNDEFINED
                ? ContentType.TEXT
                : getContentType();
        Diff diff = DiffTask
                .builder()
                .contentType(effectiveContentType)
                .leftId(getLeftLabel())
                .leftLabel(getLeftLabel())
                .leftContent(StringUtils.defaultString(left))
                .rightId(getRightLabel())
                .rightLabel(getRightLabel())
                .rightContent(StringUtils.defaultString(right))
                .filter(getEntryFilter())
                .taskParameters(getTaskParameters())
                .build()
                .run();
        return Collections.singletonList(diff);
    }
}
