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
package com.exadel.etoolbox.anydiff.comparison.path;

import com.exadel.etoolbox.anydiff.Constants;
import com.exadel.etoolbox.anydiff.util.StringUtil;
import com.github.difflib.text.DiffRow;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents an implementation of {@link PathHelper} that is suitable for Java manifest content
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ManifestPathHelper extends PathHelper {

    static final ManifestPathHelper INSTANCE = new ManifestPathHelper();

    private static final Pattern TAG_PATTERN = Pattern.compile("^\\s*[A-Z][\\w-]+:");

    @Override
    public String getPath(List<DiffRow> allRows, int position) {
        StringBuilder pathBuilder = new StringBuilder();
        SidedPosition tagPosition = getPrecedingTagPosition(allRows, position);
        if (!tagPosition.isValid()) {
            return StringUtils.EMPTY;
        }
        String tagName = getTagNameAt(allRows, tagPosition);
        int tagIndent = getIndentAt(allRows, tagPosition);
        pathBuilder.append(tagName);
        while (tagIndent >= 0) {
            int nextTagIndent = tagIndent - Constants.DEFAULT_INDENT;
            tagPosition = getPrecedingTagPosition(
                allRows,
                tagPosition.stepBack(),
                str -> StringUtil.getIndent(str) == nextTagIndent);
            if (!tagPosition.isValid()) {
                break;
            }
            tagName = getTagNameAt(allRows, tagPosition);
            if (pathBuilder.charAt(0) != Constants.SLASH_CHAR) {
                pathBuilder.insert(0, Constants.SLASH_CHAR);
            }
            pathBuilder.insert(0, tagName);
            tagIndent -= Constants.DEFAULT_INDENT;
        }
        return pathBuilder.toString();
    }

    @Override
    public int getPrecedingTagRowIndex(List<DiffRow> allRows, int position) {
        return -1;
    }

    @Override
    public boolean isTag(String value) {
        return TAG_PATTERN.matcher(value).find();
    }

    private static String getTagNameAt(List<DiffRow> allRows, SidedPosition position) {
        DiffRow tagRow = allRows.get(position.getValue());
        String tagNameSource = position.getSide() == Side.LEFT ? tagRow.getOldLine() : tagRow.getNewLine();
        int indentAmount = StringUtil.getIndent(tagNameSource);
        int colonIndex = tagNameSource.indexOf(Constants.COLON);
        int end = colonIndex > 0 ? colonIndex : tagNameSource.length();
        return tagNameSource.substring(indentAmount, end);
    }
}
