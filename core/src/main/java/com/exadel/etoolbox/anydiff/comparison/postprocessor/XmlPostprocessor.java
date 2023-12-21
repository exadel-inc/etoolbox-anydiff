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
package com.exadel.etoolbox.anydiff.comparison.postprocessor;

import com.exadel.etoolbox.anydiff.Constants;
import com.exadel.etoolbox.anydiff.comparison.Marker;
import com.exadel.etoolbox.anydiff.comparison.TaskParameters;
import com.github.difflib.text.DiffRow;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a routine that post-processes the result of a comparison operation when performed over an XML or HTML
 * content
 */
@RequiredArgsConstructor
class XmlPostprocessor extends Postprocessor {

    private final TaskParameters parameters;

    @Override
    public List<DiffRow> apply(List<DiffRow> rows) {
        List<DiffRow> result = new ArrayList<>();
        for (DiffRow diffRow : rows) {
            if (diffRow.getTag() == DiffRow.Tag.CHANGE) {
                result.add(diffRow);
            } else if (diffRow.getTag() == DiffRow.Tag.INSERT || diffRow.getTag() == DiffRow.Tag.DELETE) {
                appendInsertDelete(diffRow, result);
            } else {
                appendContext(diffRow, result);
            }
        }
        return result;
    }

    private void appendInsertDelete(DiffRow value, List<DiffRow> rows) {
        if (!parameters.normalize()) {
            rows.add(value);
            return;
        }
        String oldLine = value.getOldLine();
        if (StringUtils.startsWith(oldLine, Marker.DELETE.toString())) {
            oldLine = extractMarkedSpaces(oldLine, Marker.DELETE.toString());
        }
        String newLine = value.getNewLine();
        if (StringUtils.startsWith(newLine, Marker.INSERT.toString())) {
            newLine = extractMarkedSpaces(newLine, Marker.INSERT.toString());
        }
        rows.add(new DiffRow(value.getTag(), oldLine, newLine));
    }

    private void appendContext(DiffRow value, List<DiffRow> rows) {
        String trimmedLine = StringUtils.trim(value.getOldLine());
        if (StringUtils.equalsAny(trimmedLine, Constants.TAG_CLOSE, Constants.TAG_AUTO_CLOSE) && !rows.isEmpty()) {
            String lineEnding = Marker.CONTEXT + trimmedLine + Marker.RESET;
            DiffRow last = rows.get(rows.size() - 1);
            DiffRow modified = new DiffRow(
                    last.getTag(),
                    last.getOldLine() + lineEnding,
                    last.getNewLine() + lineEnding);
            rows.remove(last);
            rows.add(modified);
        } else {
            rows.add(value);
        }
    }

    private static String extractMarkedSpaces(String value, String marker) {
        if (!value.startsWith(StringUtils.SPACE, marker.length())) {
            return value;
        }
        int spaceCount = 0;
        for (int i = marker.length(); i < value.length() && value.charAt(i) == ' '; i++) {
            spaceCount++;
        }
        return StringUtils.repeat(StringUtils.SPACE, spaceCount) + marker + value.substring(marker.length() + spaceCount);
    }
}
