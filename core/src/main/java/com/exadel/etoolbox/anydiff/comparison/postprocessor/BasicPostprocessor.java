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

import com.exadel.etoolbox.anydiff.comparison.Marker;
import com.github.difflib.text.DiffRow;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a routine that post-processes the result of a comparison operation when performed over a plain text
 */
class BasicPostprocessor extends Postprocessor {

    static final Postprocessor INSTANCE = new BasicPostprocessor();

    @Override
    public List<DiffRow> apply(List<DiffRow> diffRows) {
        return diffRows
                .stream()
                .map(BasicPostprocessor::insertNewlineMarker)
                .collect(Collectors.toList());
    }

    private static DiffRow insertNewlineMarker(DiffRow row) {
        if (row.getTag() == DiffRow.Tag.DELETE && row.getOldLine().equals(Marker.DELETE + Marker.RESET.toString())) {
            return new DiffRow(row.getTag(), Marker.DELETE + Marker.NEW_LINE + Marker.RESET,
                    StringUtils.EMPTY);
        }
        if (row.getTag() == DiffRow.Tag.INSERT && row.getNewLine().equals(Marker.INSERT + Marker.RESET.toString())) {
            return new DiffRow(row.getTag(), StringUtils.EMPTY,
                    Marker.INSERT + Marker.NEW_LINE + Marker.RESET);
        }
        return row;
    }
}
