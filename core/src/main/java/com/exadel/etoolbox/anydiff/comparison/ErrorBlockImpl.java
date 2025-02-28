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
import com.exadel.etoolbox.anydiff.OutputType;
import com.exadel.etoolbox.anydiff.diff.DiffState;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

/**
 * Implements {@link AbstractBlock} to represent an exception in the comparison process
 */
class ErrorBlockImpl extends AbstractBlock {
    private final MarkedString message;

    ErrorBlockImpl(Exception e, int columnWidth) {
        String messageString = StringUtils.defaultIfEmpty(e.getMessage(), e.getClass().getName());
        this.message = new MarkedString(messageString, Marker.ERROR);
        setColumnWidth(columnWidth);
    }

    /* ---------
       Accessors
       --------- */

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public int getPendingCount() {
        return 0;
    }

    @Override
    public DiffState getState() {
        return DiffState.ERROR;
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

    /* ----------
       Operations
       ---------- */

    @Override
    public void accept() {
        throw new NotImplementedException();
    }

    /* ------
       Output
       ------ */

    @Override
    public String toString(OutputType target) {
        if (target == OutputType.HTML) {
            return toHtml();
        } else {
            return toText(target);
        }
    }

    private String toHtml() {
        return HtmlTags
                .div().withClassAttr(Constants.CLASS_LEFT).withContent(getLeftLabel())
                .div().withClassAttr(Constants.CLASS_RIGHT).withContent(getRightLabel())
                .wrapIn(HtmlTags.line())
                .withClassAttr(Constants.CLASS_HEADER)
                .wrapIn(HtmlTags.section())
                .withClassAttr("no-header")
                .withContent(message.toHtml())
                .toString();
    }

    private String toText(OutputType target) {
        int fullWidth = getColumnWidth() * 2 + 3;
        return String.join(StringUtils.LF, message.toText(target, fullWidth));
    }
}
