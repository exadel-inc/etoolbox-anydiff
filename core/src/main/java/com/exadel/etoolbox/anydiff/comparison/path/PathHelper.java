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

import com.exadel.etoolbox.anydiff.ContentType;
import com.exadel.etoolbox.anydiff.util.StringUtil;
import com.github.difflib.text.DiffRow;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Predicate;

/**
 * Contains utility methods for retrieving positioning and {@code XPath}-like path identifiers for a difference within
 * an XML/HTML document
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class PathHelper {

    /* -----------------
       Interface methods
       ----------------- */

    /**
     * Gets the {@code XPath}-like path identifier for the difference at the specified position
     * @param allRows  The collection of {@link DiffRow} objects that contain both the differences and the context
     * @param position Position of the line for which we compute the path identifier
     * @return {@code XPath}-like path identifier
     */
    public abstract String getPath(List<DiffRow> allRows, int position);

    /**
     * Gets the index within the given collection of {@link DiffRow} objects at which the previous XML/HTML tag is
     * located
     * @param allRows  The collection of {@link DiffRow} objects that contain both the differences and the context
     * @param position The current position within the collection
     * @return A {@link SidedPosition} object that contains the index of the previous XML/HTML tag as well as the
     * pointer to the side of the difference
     */
    public abstract int getPrecedingTagRowIndex(List<DiffRow> allRows, int position);

    /**
     * Checks if the specified string value represents a tag - that is, an eligible point inside the content path
     * @param value String value to check
     * @return True or false
     */
    public abstract boolean isTag(String value);

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
    public static PathHelper forType(ContentType type) {
        if (type == ContentType.XML || type == ContentType.HTML) {
            return XmlPathHelper.INSTANCE;
        }
        if (type == ContentType.MANIFEST) {
            return ManifestPathHelper.INSTANCE;
        }
        return null;
    }
}
