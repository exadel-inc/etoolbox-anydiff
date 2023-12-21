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

import com.exadel.etoolbox.anydiff.diff.DiffState;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implements {@link AbstractBlock} to manifest the kind of disparity when the left and the right sides of the comparison
 * cannot be compared text-wise (e.g., represent binary files)
 */
class DisparityBlockImpl extends BlockImpl {

    /* ---------
       Accessors
       --------- */

    @Override
    int getCount() {
        return 1;
    }

    @Override
    int getPendingCount() {
        return getLines().stream().anyMatch(line -> line.getPendingCount() > 0) ? 1 : 0;
    }

    @Override
    public DiffState getState() {
        return DiffState.CHANGE;
    }

    /* -------
       Content
       ------- */

    @Override
    public String getLeft(boolean includeContext) {
        return StringUtils.EMPTY;
    }

    @Override
    public String getRight(boolean includeContext) {
        return StringUtils.EMPTY;
    }

    /* -------------
       Factory logic
       ------------- */

    /**
     * Creates a builder for constructing a new {@code DisparityBlockImpl} object
     * @return {@link Builder} instance
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Constructs a new {@code DisparityBlockImpl} object
     */
    static class Builder extends BlockImpl.Builder  {

        public static final Pattern NEW_LINE = Pattern.compile(StringUtils.LF);

        private Object leftContent;
        private Object rightContent;

        /**
         * Sets the content of the left side of the block
         * @param value A non-null object
         * @return This instance
         */
        Builder leftContent(Object value) {
            this.leftContent = value;
            return this;
        }

        /**
         * Sets the content of the right side of the block
         * @param value A non-null object
         * @return This instance
         */
        Builder rightContent(Object value) {
            this.rightContent = value;
            return this;
        }

        /**
         * Builds a new {@code DisparityBlockImpl} instance using the provided constructor
         * @param constructor A {@link Supplier} that provides an instance of {@code DisparityBlockImpl}
         * @param <T>         A type that extends {@code DisparityBlockImpl}
         * @return An instance of {@code DisparityBlockImpl}
         */
        @Override
        <T extends AbstractBlock> T build(Supplier<T> constructor) {
            T block = super.build(constructor);
            List<MarkedString> leftStrings = getMarkedStrings(leftContent.toString(), Marker.DELETE);
            List<MarkedString> rightStrings = getMarkedStrings(rightContent.toString(), Marker.INSERT);
            for (int i = 0, length = Math.min(leftStrings.size(), rightStrings.size()); i < length; i++) {
                LineImpl line = new LineImpl(leftStrings.get(i), rightStrings.get(i));
                ((BlockImpl) block).add(line);
            }
            for (int i = leftStrings.size(); i < rightStrings.size(); i++) {
                LineImpl line = new LineImpl(null, rightStrings.get(i));
                ((BlockImpl) block).add(line);
            }
            for (int i = rightStrings.size(); i < leftStrings.size(); i++) {
                LineImpl line = new LineImpl(leftStrings.get(i), null);
                ((BlockImpl) block).add(line);
            }
            return block;
        }

        private static List<MarkedString> getMarkedStrings(String value, Marker marker) {
            return NEW_LINE
                    .splitAsStream(value)
                    .map(line -> new MarkedString(line).markPlaceholders(marker))
                    .collect(Collectors.toList());
        }
    }
}
