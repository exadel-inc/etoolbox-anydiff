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
import com.exadel.etoolbox.anydiff.diff.Fragment;
import com.exadel.etoolbox.anydiff.diff.MarkupFragment;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Predicate;

/**
 * Implements {@link Fragment} to manage a char sequence within an XML or HTML string that differs from another string
 * @see MarkupFragment
 */
class MarkupFragmentImpl extends FragmentImpl implements MarkupFragment {

    private static final Predicate<Character> IS_NAME_PART = chr -> Character.isLetterOrDigit(chr)
            || chr == Constants.DASH_CHAR
            || chr == Constants.UNDERSCORE_CHAR
            || chr == Constants.COLON_CHAR;

    /**
     * Creates a new instance of {@link MarkupFragmentImpl} class
     * @param offset    An offset of the fragment in the source string
     * @param endOffset End offset of the fragment in the source string
     */
    MarkupFragmentImpl(int offset, int endOffset) {
        super(offset, endOffset);
    }

    private MarkupFragmentImpl(String source, int offset, int endOffset) {
        super(offset, endOffset);
        setSource(source);
    }

    @Override
    public boolean isAttributeValue(String name) {
        Fragment attribute = toAttribute();
        return attribute != null
                && StringUtils.substringBefore(attribute.toString(), Constants.TAG_ATTR_OPEN).equalsIgnoreCase(name);
    }

    @Override
    public boolean isInsideTag(String name) {
        Fragment tag = toTag();
        if (tag == null) {
            return false;
        }
        TagIdentifier identifier = TagIdentifier.from(name);
        return identifier.matches(tag);
    }

    @Override
    public boolean isTagContent(String name) {
        Fragment tag = toTag();
        if (tag == null) {
            return false;
        }
        TagIdentifier identifier = TagIdentifier.from(name);
        return identifier.matches(tag)
                && getOffset() > getSource().indexOf(Constants.TAG_CLOSE, ((FragmentImpl) tag).getOffset());
    }

    /**
     * Retrieves a {@link Fragment} representing an XML/HTML attribute inside which the current fragment is situated
     * @return {@link Fragment} instance or {@code null} if the current fragment is not situated inside an attribute
     */
    Fragment toAttribute() {
        int startQuote = moveLeftTo(getOffset(), Constants.TAG_ATTR_OPEN, Constants.TAG_ATTR_CLOSE);
        if (startQuote == -1) {
            return null;
        }
        int endQuote = moveRightTo(getEndOffset(), Constants.TAG_ATTR_CLOSE);
        int namedOffset = moveLeftWhile(getSource(), startQuote, IS_NAME_PART);
        return namedOffset < startQuote
                ? new MarkupFragmentImpl(getSource(), namedOffset, endQuote)
                : null;
    }

    /**
     * Retrieves a {@link Fragment} representing an XML/HTML tag inside which the current fragment is situated
     * @return {@link Fragment} instance or {@code null} if the current fragment is not situated inside a tag
     */
    Fragment toTag() {
        return toTag(getOffset(), null);
    }

    /**
     * Retrieves a {@link Fragment} representing an XML/HTML tag inside which the current fragment is situated
     * @param position An offset in the source string from where the search for the nesting tag should be started
     * @param fallback A {@link Fragment} instance to return if the nesting tag is not found
     * @return A {@link Fragment} instance that can be equal to the value of {@code fallback}
     */
    private Fragment toTag(int position, Fragment fallback) {
        int openingTagOffset = moveLeftTo(position, Constants.TAG_OPEN, null);
        if (openingTagOffset < 0) {
            return fallback;
        }
        int openingTagEndOffset = moveRightTo(openingTagOffset, Constants.TAG_CLOSE);
        if (openingTagEndOffset < openingTagOffset) {
            return fallback;
        }
        boolean isSelfClosedTag = getSource().charAt(openingTagEndOffset - 1) == Constants.SLASH_CHAR;
        if (isSelfClosedTag && openingTagEndOffset > getEndOffset()) {
            return new MarkupFragmentImpl(getSource(), openingTagOffset, openingTagEndOffset + 1);
        } else if (isSelfClosedTag) {
            return toTag(openingTagOffset, fallback);
        }
        String tagName = getName(getSource(), openingTagOffset);
        int closingTagOffset = getSource().indexOf(
                Constants.TAG_PRE_CLOSE + tagName + Constants.TAG_CLOSE,
                openingTagOffset);
        if (closingTagOffset >= getEndOffset()) {
            return new MarkupFragmentImpl(
                    getSource(),
                    openingTagOffset,
                    closingTagOffset + Constants.TAG_PRE_CLOSE.length() + tagName.length() + Constants.TAG_CLOSE.length());
        }
        Fragment incompleteTag = new MarkupFragmentImpl(getSource(), openingTagOffset, getSource().length());
        return toTag(openingTagOffset - 1, fallback != null ? fallback : incompleteTag);
    }

    private int moveLeftTo(int position, String target, String stopper) {
        if (getSource().startsWith(target, position)) {
            return getOffset();
        }
        int left = position - target.length();
        while (left >= 0 && !getSource().startsWith(target, left)) {
            if (stopper != null && getSource().startsWith(stopper, left)) {
                return -1;
            }
            left--;
        }
        return getSource().startsWith(target, left) ? left : -1;
    }

    private int moveRightTo(int position, String ending) {
        if (position + ending.length() > getSource().length()) {
            return -1;
        }
        int right = position;
        while (right < getSource().length() && !getSource().startsWith(ending, right)) {
            right++;
        }
        return getSource().startsWith(ending, right) ? right + ending.length() : -1;
    }

    @SuppressWarnings("SameParameterValue")
    private static int moveLeftWhile(String source, int offset, Predicate<Character> filter) {
        int left = offset - 1;
        while (left >= 0 && filter.test(source.charAt(left))) {
            left--;
        }
        return left + 1;
    }

    @SuppressWarnings("SameParameterValue")
    private static int moveRightWhile(String source, int offset, Predicate<Character> filter) {
        int right = offset;
        while (right < source.length() && filter.test(source.charAt(right))) {
            right++;
        }
        return right;
    }

    private static String getName(String source, int offset) {
        int beginOffset = source.charAt(offset) == Constants.TAG_OPEN_CHAR ? offset + 1 : offset;
        int endOffset = moveRightWhile(source, beginOffset, IS_NAME_PART);
        return endOffset > beginOffset ? source.substring(beginOffset, endOffset) : StringUtils.EMPTY;
    }

    /**
     * Represents a tag identifier used to match a tag by its name and/or attributes
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class TagIdentifier {
        private String name;
        private TagSelector selector;

        boolean matches(Fragment tag) {
            return StringUtils.equals(name, getName(tag.getSource(), ((FragmentImpl) tag).getOffset()))
                    && (selector == null || selector.matches(tag));
        }

        static TagIdentifier from(String value) {
            TagIdentifier result = new TagIdentifier();
            int openBracket = value.indexOf(Constants.BRACKET_OPEN);
            int closeBracket = value.indexOf(Constants.BRACKET_CLOSE);
            if (openBracket >= 0 && closeBracket >= openBracket) {
                result.name = value.substring(0, openBracket).trim();
                result.selector = TagSelector.from(value.substring(openBracket + 1, closeBracket).trim());
            } else if (value.indexOf(Constants.DOT) > 0) {
                result.name = value.substring(0, value.indexOf(Constants.DOT)).trim();
                result.selector = new TagSelector(
                        TagSelectorOperator.CONTAINS,
                        "class",
                        value.substring(value.indexOf(Constants.DOT) + 1).trim());
            } else if (value.indexOf(Constants.HASH) > 0) {
                result.name = value.substring(0, value.indexOf(Constants.HASH)).trim();
                result.selector = new TagSelector(
                        TagSelectorOperator.EQUALS,
                        "id",
                        value.substring(value.indexOf(Constants.HASH) + 1).trim());
            } else {
                result.name = value.trim();
            }
            return result;
        }
    }

    /**
     * Represents a tag selector used to match a tag by its attributes. The selection is made with either an attribute
     * value being equal, or the attribute value containing a specified string, or else the attribute with the given name
     * present in the tag
     */
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class TagSelector {

        private TagSelectorOperator operator;
        private String name;
        private String value;

        boolean matches(Fragment tag) {
            String tagCaption = StringUtils.substringBetween(tag.toString(), Constants.TAG_OPEN, Constants.TAG_CLOSE);
            if (StringUtils.isEmpty(tagCaption)) {
                return false;
            }
            if (operator == TagSelectorOperator.EXISTS) {
                return StringUtils.contains(tagCaption, name);
            } else if (operator == TagSelectorOperator.EQUALS) {
                return StringUtils.contains(
                        tagCaption,
                        name + Constants.TAG_ATTR_OPEN + value + Constants.TAG_ATTR_CLOSE);
            }
            String attributeValue = StringUtils.substringBetween(
                    tagCaption,
                    name + Constants.TAG_ATTR_OPEN, Constants.TAG_ATTR_CLOSE);

            return StringUtils.contains(attributeValue, value);
        }

        static TagSelector from(String value) {
            TagSelector result = new TagSelector();
            if (!value.contains(Constants.QUOTE)) {
                result.operator = TagSelectorOperator.EXISTS;
                result.name = value.replaceAll("[^\\w-]+", StringUtils.EMPTY);
                return result.name.isEmpty() ? null : result;
            }
            result.value = StringUtils.substringBetween(value, Constants.QUOTE, Constants.QUOTE);
            String nameAndOperator = StringUtils.substringBefore(value, Constants.QUOTE).trim();
            if (StringUtils.isAnyEmpty(nameAndOperator, value)) {
                return null;
            }
            if (nameAndOperator.endsWith("*=")) {
                result.name = StringUtils.stripEnd(nameAndOperator, "*= ");
                result.operator = TagSelectorOperator.CONTAINS;
            } else if (nameAndOperator.endsWith(Constants.EQUALS)) {
                result.name = StringUtils.stripEnd(nameAndOperator, "= ");
                result.operator = TagSelectorOperator.EQUALS;
            } else {
                return null;
            }
            return result;
        }
    }

    /**
     * Enumerates the types of matching tag attributes that are used by {@link TagSelector}
     */
    private enum TagSelectorOperator {
        EQUALS,
        EXISTS,
        CONTAINS
    }
}
