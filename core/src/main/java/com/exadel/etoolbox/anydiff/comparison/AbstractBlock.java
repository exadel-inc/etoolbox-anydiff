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

import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.diff.PrintableEntry;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Represents a block of lines that contain a difference
 */
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
abstract class AbstractBlock implements DiffEntry, PrintableEntry {

    /**
     * Gets the {@link Diff} object this block belongs to
     */
    @Getter(AccessLevel.PUBLIC)
    private Diff diff;

    /**
     * Gets the label that is displayed before the left side of the block
     */
    private String leftLabel;

    /**
     * Gets the label that is displayed before the right side of the block
     */
    private String rightLabel;

    /**
     * Gets the width of the column in the console and logfile output
     */
    private int columnWidth;

    @Setter(AccessLevel.NONE)
    private final List<LineImpl> lines = new ArrayList<>();

    /**
     * Gets the number of differences contained in the block
     */
    abstract int getCount();

    /**
     * Gets the number of differences contained in the block that have not need "silenced" (accepted) via a
     * {@link com.exadel.etoolbox.anydiff.filter.Filter}
     */
    abstract int getPendingCount();

    /**
     * Constructs an object that subclasses {@link AbstractBlock}. This class must be extended by a concrete
     * implementation
     */
    @RequiredArgsConstructor
    static abstract class Builder {

        private String leftLabel;
        private String rightLabel;
        private List<LineImpl> lines;
        private int columnWidth;

        /**
         * Sets the label that is displayed before the left side of the block
         * @param value A string value. A non-empty string is expected
         * @return This instance
         */
        Builder leftLabel(String value) {
            this.leftLabel = value;
            return this;
        }

        /**
         * Sets the label that is displayed before the right side of the block
         * @param value A string value. A non-empty string is expected
         * @return This instance
         */
        Builder rightLabel(String value) {
            this.rightLabel = value;
            return this;
        }

        /**
         * Assigns the collection lines that contain the difference
         * @param value A list of {@link LineImpl} instances. A non-null list is expected
         * @return This instance
         */
        Builder lines(List<LineImpl> value) {
            this.lines = value;
            return this;
        }

        /**
         * Sets the width of the column in the console and logfile output
         * @param value An integer value. A positive integer is expected
         * @return This instance
         */
        Builder columnWidth(int value) {
            this.columnWidth = value;
            return this;
        }

        /**
         * When called from a subclass class, helps to build a descendant instance of {@link AbstractBlock} using the
         * provided constructor
         * @param constructor A {@link Supplier} that provides an instance of an {@link AbstractBlock}'s subclass
         * @param <T>         A type that extends {@link AbstractBlock}
         * @return An instance of {@link AbstractBlock} or its subclass
         */
        <T extends AbstractBlock> T build(Supplier<T> constructor) {
            T block = constructor.get();
            block.setLeftLabel(leftLabel);
            block.setRightLabel(rightLabel);
            block.setColumnWidth(columnWidth);
            if (CollectionUtils.isNotEmpty(lines)) {
                lines.forEach(l -> l.setBlock(block));
                block.getLines().addAll(lines);
            }
            return block;
        }
    }
}
