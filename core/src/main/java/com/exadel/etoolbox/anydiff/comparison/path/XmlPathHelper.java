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
import com.exadel.etoolbox.anydiff.comparison.Marker;
import com.exadel.etoolbox.anydiff.util.StringUtil;
import com.github.difflib.text.DiffRow;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Represents an implementation of {@link PathHelper} that is suitable for XML/HTML content
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class XmlPathHelper extends PathHelper {

    static final XmlPathHelper INSTANCE = new XmlPathHelper();

    @Override
    public String getPath(List<DiffRow> allRows, int position) {
        StringBuilder pathBuilder = new StringBuilder();
        SidedPosition tagPosition = getPrecedingTagPosition(allRows, position);
        if (!tagPosition.isValid()) {
            return StringUtils.EMPTY;
        }
        String tagName = getTagNameAt(allRows, tagPosition);
        int tagIndent = getIndentAt(allRows, tagPosition);
        int tagIndex = getSiblingIndex(allRows, tagPosition.stepBack(), tagName, tagIndent);
        pathBuilder
            .append(tagName)
            .append(tagIndex > 0 ? Constants.BRACKET_OPEN + String.valueOf(tagIndex) + Constants.BRACKET_CLOSE :
                StringUtils.EMPTY);
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
            tagIndex = getSiblingIndex(allRows, tagPosition.stepBack(), tagName, nextTagIndent);
            String indexedTagName = tagName
                + (tagIndex > 0 ? Constants.BRACKET_OPEN + String.valueOf(tagIndex) + Constants.BRACKET_CLOSE :
                StringUtils.EMPTY);
            if (pathBuilder.charAt(0) != Constants.SLASH_CHAR) {
                pathBuilder.insert(0, Constants.SLASH_CHAR);
            }
            pathBuilder.insert(0, indexedTagName);
            tagIndent -= Constants.DEFAULT_INDENT;
        }
        return (pathBuilder.charAt(0) != Constants.SLASH_CHAR ? Constants.SLASH : StringUtils.EMPTY) + pathBuilder;
    }

    @Override
    public int getPrecedingTagRowIndex(List<DiffRow> allRows, int position) {
        return getPrecedingTagPosition(allRows, position).getValue();
    }

    @Override
    public boolean isTag(String value) {
        return startsWithIgnoreSpaces(value, Constants.TAG_OPEN);
    }

    private int getSiblingIndex(List<DiffRow> allRows, SidedPosition position, String tagName, int indent) {
        int result = 0;
        SidedPosition precedingPosition = getPrecedingTagPosition(allRows, position);
        while (precedingPosition.isValid()) {
            if (getIndentAt(allRows, precedingPosition) < indent) {
                return result;
            }
            if (getTagNameAt(allRows, precedingPosition).equals(tagName)) {
                result++;
            }
            precedingPosition = getPrecedingTagPosition(allRows, precedingPosition.stepBack());
        }
        return result;
    }

    private static String getTagNameAt(List<DiffRow> allRows, SidedPosition position) {
        DiffRow tagRow = allRows.get(position.getValue());
        String tagNameSource = position.getSide() == Side.LEFT ? tagRow.getOldLine() : tagRow.getNewLine();
        int start = tagNameSource.indexOf(Constants.TAG_OPEN) + 1;
        int end = IntStream.of(
                tagNameSource.indexOf(Constants.TAG_CLOSE, start),
                tagNameSource.indexOf(StringUtils.SPACE, start))
            .filter(pos -> pos > start)
            .min()
            .orElse(tagNameSource.length());
        String result = StringUtil.removeAll(tagNameSource.substring(start, end), Marker.TOKENS);
        if (result.contains(Constants.TAG_AUTO_CLOSE)) {
            result = result.substring(0, result.length() - Constants.TAG_AUTO_CLOSE.length());
        }
        return "!--".equals(result) ? "#comment" : result;
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean startsWithIgnoreSpaces(CharSequence value, String prefix) {
        if (StringUtils.isAnyEmpty(value, prefix)) {
            return false;
        }
        int indentAmount = StringUtil.getIndent(value);
        return value.toString().startsWith(prefix, indentAmount);
    }
}
