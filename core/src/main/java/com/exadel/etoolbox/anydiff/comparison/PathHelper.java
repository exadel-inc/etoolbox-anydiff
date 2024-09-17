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
import com.github.difflib.text.DiffRow;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Contains utility methods for retrieving positioning and {@code XPath}-like path identifiers for a difference within
 * an XML/HTML document
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
abstract class PathHelper {

    /* -----------------
       Interface methods
       ----------------- */

    /**
     * Gets the {@code XPath}-like path identifier for the difference at the specified position
     * @param allRows  The collection of {@link DiffRow} objects that contain both the differences and the context
     * @param position Position of the line for which we compute the path identifier
     * @return {@code XPath}-like path identifier
     */
    abstract String getPath(List<DiffRow> allRows, int position);

    /**
     * Gets the index within the given collection of {@link DiffRow} objects at which the previous XML/HTML tag is
     * located
     * @param allRows  The collection of {@link DiffRow} objects that contain both the differences and the context
     * @param position The current position within the collection
     * @return A {@link SidedPosition} object that contains the index of the previous XML/HTML tag as well as the
     * pointer to the side of the difference
     */
    abstract int getPrecedingTagRowIndex(List<DiffRow> allRows, int position);

    /**
     * Checks if the specified string value represents a tag - that is, an eligible point inside the content path
     * @param value String value to check
     * @return True or false
     */
    abstract boolean isTag(String value);

    /* ----------------
       Internal methods
       ---------------- */

    /**
     * Gets the index within the given collection of {@link DiffRow} objects at which the previous tag is located
     * @param allRows  The collection of {@link DiffRow} objects that contain both the differences and the context
     * @param position The current position (i.e., the position of the currently rendered difference) within the
     *                 collection
     * @param filter   A predicate that filters the tag names
     * @return A {@link SidedPosition} object that contains the index of the previous tag as well as the pointer to the
     * side of the difference
     */
    SidedPosition getPrecedingTagPosition(
        List<DiffRow> allRows,
        SidedPosition position,
        Predicate<String> filter) {

        for (int i = position.getValue(); i >= 0; i--) {
            DiffRow current = allRows.get(i);
            boolean isTagToTheLeft = isTag(current.getOldLine());
            boolean isTagToTheRight = isTag(current.getNewLine());
            if (!isTagToTheLeft && !isTagToTheRight) {
                continue;
            }
            boolean isMatchToTheLeft = filter.test(current.getOldLine());
            boolean isMatchToTheRight = filter.test(current.getNewLine());
            boolean isMatchOnEitherSide = isMatchToTheLeft || isMatchToTheRight;

            if ((position.getSide() == Side.LEFT && isMatchToTheLeft)
                || (position.getSide() == Side.RIGHT && isMatchToTheRight)
                || (position.getSide() == Side.ANY && isMatchOnEitherSide)) {
                return new SidedPosition(i, position.getSide());
            }
        }
        return SidedPosition.INVALID;
    }

    /**
     * Gets the index within the given collection of {@link DiffRow} objects at which the previous tag is located
     * @param allRows The collection of {@link DiffRow} objects that contain both the differences and the context
     * @param position The current position within the collection
     * @return A {@link SidedPosition} object that contains the index of the previous tag as well as the pointer to the
     * side of the difference
     */
    SidedPosition getPrecedingTagPosition(List<DiffRow> allRows, int position) {
        return getPrecedingTagPosition(allRows, new SidedPosition(position, Side.ANY));
    }

    /**
     * Gets the index within the given collection of {@link DiffRow} objects at which the previous tag is located
     * @param allRows The collection of {@link DiffRow} objects that contain both the differences and the context
     * @param position The current position within the collection
     * @return A {@link SidedPosition} object that contains the index of the previous tag as well as the pointer to the
     * side of the difference
     */
    SidedPosition getPrecedingTagPosition(List<DiffRow> allRows, SidedPosition position) {
        return getPrecedingTagPosition(allRows, position, str -> true);
    }

    /**
     * Gets the number of leading spaces in the specified string
     * @param allRows The collection of {@link DiffRow} objects that contain both the differences and the context
     * @param position The current position within the collection
     * @return Number of leading spaces
     */
    int getIndentAt(List<DiffRow> allRows, SidedPosition position) {
        DiffRow tagRow = allRows.get(position.getValue());
        return position.getSide() == Side.LEFT
            ? StringUtil.getIndent(tagRow.getOldLine())
            : StringUtil.getIndent(tagRow.getNewLine());
    }

    /* -------------
       Factory logic
       ------------- */

    /**
     * Retrieves an instance of {@link PathHelper} that is suitable for the specified content type. Returns {@code null}
     * if the content type is not supported
     * @param type {@link ContentType} value
     * @return A nullable {@link PathHelper} instance
     */
    static PathHelper forType(ContentType type) {
        if (type == ContentType.XML || type == ContentType.HTML) {
            return XmlPathHelper.INSTANCE;
        }
        if (type == ContentType.MANIFEST) {
            return ManifestPathHelper.INSTANCE;
        }
        return null;
    }

    /* ----------------------
       Implementation classes
       ---------------------- */

    /**
     * Represents an implementation of {@link PathHelper} that is suitable for XML/HTML content
     */
    private static class XmlPathHelper extends PathHelper {
        private static final XmlPathHelper INSTANCE = new XmlPathHelper();

        // Interface methods

        /**
         * {@inheritDoc}
         */
        @Override
        String getPath(List<DiffRow> allRows, int position) {
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

        /**
         * {@inheritDoc}
         */
        @Override
        int getPrecedingTagRowIndex(List<DiffRow> allRows, int position) {
            return getPrecedingTagPosition(allRows, position).getValue();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean isTag(String value) {
            return startsWithIgnoreSpaces(value, Constants.TAG_OPEN);
        }

        // Internal methods

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

    /**
     * Represents an implementation of {@link PathHelper} that is suitable for Java manifest content
     */
    private static class ManifestPathHelper extends PathHelper {
        private static final ManifestPathHelper INSTANCE = new ManifestPathHelper();

        private static final Pattern TAG_PATTERN = Pattern.compile("^\\s*[A-Z][\\w-]+:");

        // Interface methods

        /**
         * {@inheritDoc}
         */
        @Override
        String getPath(List<DiffRow> allRows, int position) {
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
            return (pathBuilder.charAt(0) != Constants.SLASH_CHAR ? Constants.SLASH : StringUtils.EMPTY) + pathBuilder;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        int getPrecedingTagRowIndex(List<DiffRow> allRows, int position) {
            return -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean isTag(String value) {
            return TAG_PATTERN.matcher(value).find();
        }

        // Internal methods

        private static String getTagNameAt(List<DiffRow> allRows, SidedPosition position) {
            DiffRow tagRow = allRows.get(position.getValue());
            String tagNameSource = position.getSide() == Side.LEFT ? tagRow.getOldLine() : tagRow.getNewLine();
            int indentAmount = StringUtil.getIndent(tagNameSource);
            int colonIndex = tagNameSource.indexOf(Constants.COLON);
            int end = colonIndex > 0 ? colonIndex : tagNameSource.length();
            return tagNameSource.substring(indentAmount, end);
        }
    }

    /**
     * Stores a position within a collection of {@link DiffRow} objects and the side of the difference
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    private static class SidedPosition {
        static final SidedPosition INVALID = new SidedPosition(-1, Side.ANY);

        private int value;
        private Side side;

        boolean isValid() {
            return value >= 0;
        }

        SidedPosition stepBack() {
            return new SidedPosition(value - 1, side);
        }
    }

    /**
     * Enumerates the possible options for specifying the side of the difference
     * @see SidedPosition
     */
    private enum Side {
        ANY, LEFT, RIGHT
    }
}
