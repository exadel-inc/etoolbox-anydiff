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

import com.exadel.etoolbox.anydiff.diff.Fragment;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Implements {@link Fragment} to manage a char sequence in a string that differs from another string
 * @see Fragment
 */
@Getter
class FragmentImpl implements Fragment {

    /**
     * Gets the offset of the fragment in the source string
     */
    private int offset;

    /**
     * Gets the end offset of the fragment in the source string
     */
    private int endOffset;

    /**
     * Gets or sets the string the current fragment belongs to
     */
    @Setter(AccessLevel.PACKAGE)
    private String source;

    /**
     * Gets or sets the offset the current line (an {@code \n}-delimited substring) in the source string
     */
    @Setter(AccessLevel.PACKAGE)
    private int lineOffset;

    /**
     * Gets or sets whether the current fragment is an insertion
     */
    @Setter(value = AccessLevel.PACKAGE)
    private boolean isInsert;

    /**
     * Gets or sets whether the current fragment is a deletion
     */
    @Setter(value = AccessLevel.PACKAGE)
    private boolean isDelete;

    /**
     * Creates a new instance of {@link FragmentImpl}
     * @param offset    An offset of the fragment in the source string
     * @param endOffset End offset of the fragment in the source string
     */
    FragmentImpl(int offset, int endOffset) {
        this.offset = offset;
        this.endOffset = endOffset;
    }

    @Override
    public int length() {
        return endOffset - offset;
    }

    @Override
    public char charAt(int index) {
        return source.charAt(index + offset);
    }

    @NotNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return source.subSequence(start + this.offset, end + this.offset);
    }

    /**
     * Trims the fragment by removing leading and trailing whitespace characters
     */
    void trim() {
        while (offset < endOffset && Character.isWhitespace(source.charAt(offset))) {
            offset++;
            lineOffset++;
        }
        while (endOffset > offset && Character.isWhitespace(source.charAt(endOffset - 1))) {
            endOffset--;
        }
    }

    @NotNull
    @Override
    public String toString() {
        return source.subSequence(offset, endOffset).toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        FragmentImpl otherHighlight = (FragmentImpl) other;
        return this.getOffset() == otherHighlight.getOffset()
                && this.getEndOffset() == otherHighlight.getEndOffset()
                && source.equals(otherHighlight.source);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.getOffset())
                .append(this.getEndOffset())
                .append(source)
                .toHashCode();
    }
}
