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

import com.exadel.etoolbox.anydiff.OutputType;
import com.exadel.etoolbox.anydiff.diff.Fragment;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class MarkedStringTest {

    private static final String DEFAULT_TEXT = "Lorem ipsum";

    private static final String MARKED_TEXT_1 = "{{eq}}    Lorem ipsum{{/}}";
    private static final String MARKED_TEXT_2 = "{{del}}Lorem{{/}} {{ins}}ipsum{{/}}";

    private static final int DEFAULT_COLUMN = 60;

    private static final String DELETE_STRING = Marker.DELETE.toConsole();
    private static final String INSERT_STRING = Marker.INSERT.toConsole();
    private static final String RESET_STRING = Marker.RESET.toConsole();

    @Test
    public void shouldParseSimpleText() {
        MarkedString side = new MarkedString(DEFAULT_TEXT, false);
        Assert.assertEquals(DEFAULT_TEXT, side.toString());
        Assert.assertEquals(DEFAULT_TEXT, stripEnd(side.toText(OutputType.CONSOLE, DEFAULT_COLUMN).get(0)));
        Assert.assertEquals(DEFAULT_TEXT, stripEnd(side.toText(OutputType.LOG, DEFAULT_COLUMN).get(0)));
        Assert.assertEquals(DEFAULT_TEXT, side.toHtml());
    }

    @Test
    public void shouldParseTextWithMarkers() {
        MarkedString side = new MarkedString("Lorem {{ins}}ipsum{{/}}", false);
        Assert.assertEquals(DEFAULT_TEXT, side.toString());
        Assert.assertEquals("Lorem +ipsum+", stripEnd(side.toText(OutputType.LOG, DEFAULT_COLUMN).get(0)));
        Assert.assertEquals("Lorem <span class=\"ins\">ipsum</span>", side.toHtml());

        side = new MarkedString(MARKED_TEXT_2, false);
        Assert.assertEquals(DEFAULT_TEXT, side.toString());
        Assert.assertEquals("~Lorem~ +ipsum+", stripEnd(side.toText(OutputType.LOG, DEFAULT_COLUMN).get(0)));
        Assert.assertEquals("<span class=\"del\">Lorem</span> <span class=\"ins\">ipsum</span>", side.toHtml());

        side = new MarkedString("L{{/}}orem {{ins}}ipsum", false);
        Assert.assertEquals(DEFAULT_TEXT, side.toString());
        Assert.assertEquals("Lorem +ipsum+", side.toText(OutputType.LOG, DEFAULT_COLUMN).get(0).trim());
    }

    @Test
    public void shouldApplyMark() {
        MarkedString side = new MarkedString(MARKED_TEXT_2, true).mark(Marker.CONTEXT);
        Assert.assertEquals("=Lorem ipsum", stripEnd(side.toText(OutputType.LOG, DEFAULT_COLUMN).get(0)));

        side = new MarkedString(MARKED_TEXT_2, true);
        side.mark("Lorem", Marker.INSERT);
        Assert.assertEquals("+Lorem+ +ipsum+", stripEnd(side.toText(OutputType.LOG, DEFAULT_COLUMN).get(0)));

        side = new MarkedString(MARKED_TEXT_2, true);
        side.mark("ore", Marker.INSERT);
        Assert.assertEquals("~L~+ore+~m~ +ipsum+", stripEnd(side.toText(OutputType.LOG, DEFAULT_COLUMN).get(0)));

        side = new MarkedString(MARKED_TEXT_2, true);
        side.mark("ore", Marker.DELETE);
        Assert.assertEquals("~Lorem~ +ipsum+", stripEnd(side.toText(OutputType.LOG, DEFAULT_COLUMN).get(0)));

        side = new MarkedString("lorem ipsum dolor sit amet", false);
        side.mark("lor", Marker.INSERT);
        Assert.assertEquals("+lor+em ipsum do+lor+ sit amet", stripEnd(side.toText(OutputType.LOG, DEFAULT_COLUMN).get(0)));
    }


    @Test
    public void shouldUnmark() {
        MarkedString side = new MarkedString(MARKED_TEXT_1, true).unmark();
        Assert.assertEquals(DEFAULT_TEXT, side.toText(OutputType.LOG, DEFAULT_COLUMN).get(0).trim());

        side = new MarkedString(MARKED_TEXT_2, false);
        Fragment fragment = side.getFragments().get(0);
        side.unmark(fragment);
        Assert.assertEquals("Lorem +ipsum+", stripEnd(side.toText(OutputType.LOG, DEFAULT_COLUMN).get(0)));
    }

    @Test
    public void shouldManageIndent() {
        MarkedString side = new MarkedString(MARKED_TEXT_1, false);
        Assert.assertEquals(0, side.getIndent());

        side = new MarkedString(MARKED_TEXT_1, true, true);
        Assert.assertEquals(4, side.getIndent());
        side.cutLeft(2);
        Assert.assertEquals(2, side.getIndent());
        side.cutLeft(3);
        Assert.assertEquals(0, side.getIndent());
        Assert.assertEquals("=orem ipsum", stripEnd(side.toText(OutputType.LOG, DEFAULT_COLUMN).get(0)));
    }

    @Test
    public void shouldTransferToNextLine() {
        MarkedString side = new MarkedString(MARKED_TEXT_2, true);

        List<String> lines = side.toText(OutputType.LOG, 10);
        Assert.assertEquals(2, lines.size());
        Assert.assertEquals("~Lorem~ +i", lines.get(0));
        Assert.assertEquals("psum+     ", lines.get(1));

        lines = side.toText(OutputType.CONSOLE, 5);
        Assert.assertEquals(3, lines.size());
        Assert.assertEquals(DELETE_STRING + "Lorem" + RESET_STRING, lines.get(0));
        Assert.assertEquals(" " + INSERT_STRING + "ipsu" + RESET_STRING, lines.get(1));
        Assert.assertEquals(INSERT_STRING + "m" + RESET_STRING + "    ", lines.get(2));

        lines = side.toText(OutputType.CONSOLE, 10);
        Assert.assertEquals(2, lines.size());
        Assert.assertEquals(
                DELETE_STRING + "Lorem" + RESET_STRING + " " + INSERT_STRING + "ipsu" + RESET_STRING,
                lines.get(0));
        Assert.assertEquals(INSERT_STRING + "m" + RESET_STRING + "         ", lines.get(1));
    }

    @Test
    public void shouldRetrieveFragments() {
        MarkedString side = new MarkedString(DEFAULT_TEXT, false);
        Assert.assertEquals(0, side.getFragments().size());

        side = new MarkedString(MARKED_TEXT_1, false);
        Assert.assertEquals(1, side.getFragments(true).size());
        Assert.assertEquals(side.getFragments(true).get(0).toString(), side.toString());

        side = new MarkedString(MARKED_TEXT_2, true);
        Assert.assertEquals(2, side.getFragments().size());
        Assert.assertEquals("Lorem", side.getFragments().get(0).toString());
        Assert.assertTrue(side.getFragments().get(0).isDelete());
        Assert.assertEquals("ipsum", side.getFragments().get(1).toString());
        Assert.assertTrue(side.getFragments().get(1).isInsert());
    }

    private static String stripEnd(String value) {
        return StringUtils.stripEnd(value, StringUtils.SPACE);
    }
}