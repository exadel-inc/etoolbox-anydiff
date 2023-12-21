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

import com.exadel.etoolbox.anydiff.AnyDiff;
import com.exadel.etoolbox.anydiff.ContentType;
import com.exadel.etoolbox.anydiff.diff.Diff;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class SpacesHandlingTest {

    private static final String LEFT_TEXT = "Lorem ipsum dolor sit amet,\nconsectetur adipiscing elit   ";
    private static final String RIGHT_TEXT = " Lorem ipsum   dolor sit amet,    \nconsectetur adipiscing elit";

    private static final String LEFT_HTML = "<div id=\"main\" class=\"main\"> <a href=\"main\">Lorem ipsum</a>   " +
            "<span data-attr=\"et velit laoreet\">Dolor sit amet\nconsectetur adipiscing elit</span></div>";
    private static final String RIGHT_HTML = "<div id=\" main\" class=\"main \"><a href=\"main\"> Lorem  ipsum</a>" +
            "<span data-attr=\"et  velit  laoreet\">Dolor   sit amet \nconsectetur adipiscing  elit  </span></div>";

    private static final String LEFT_XML = "<catalog> <items>" +
            "<item id=\"bk101\" tag=\"consectetur\">\n" +
            "    <title>Lorem ipsum dolor sit amet</title><description>Consectetur adipiscing elit</description></item>\n" +
            "<item id=\"bk102\" tag=\"eiusmod\">\n" +
            "    <title>Sed do eiusmod tempor</title><description>Ut labore \net dolore</description></item>" +
            "</items> </catalog>";
    private static final String RIGHT_XML = "<catalog><items>" +
            "<item id=\" bk101\" tag=\"consectetur \">" +
            "    <title>  Lorem ipsum dolor sit amet</title><description>Consectetur adipiscing elit  </description></item>" +
            "<item id=\"bk102\" tag=\"eiusmod\">\n" +
            "    <title>Sed do eiusmod   tempor</title><description>Ut labore \net  dolore </description></item>" +
            "</items></catalog>";

    @Test
    public void shouldHandleSpacesComparingTexts() {
        AnyDiff anyDiff = new AnyDiff().left(LEFT_TEXT).right(RIGHT_TEXT);
        List<Diff> differences = anyDiff.compare();
        Assert.assertEquals(1, differences.size());
        Assert.assertEquals(5, differences.get(0).getCount());

        differences = anyDiff.ignoreSpaces(true).compare();
        Assert.assertEquals(0, differences.size());
    }

    @Test
    public void shouldHandleSpacesComparingHtml() {
        AnyDiff anyDiff = new AnyDiff().left(LEFT_HTML).right(RIGHT_HTML).contentType(ContentType.HTML);
        List<Diff> differences = anyDiff.compare();
        Assert.assertEquals(1, differences.size());
        Assert.assertEquals(7, differences.get(0).getCount());

        differences = anyDiff.ignoreSpaces(true).compare();
        Assert.assertEquals(0, differences.size());
    }

    @Test
    public void shouldHandleSpacesComparingXml() {
        AnyDiff anyDiff = new AnyDiff().left(LEFT_XML).right(RIGHT_XML).contentType(ContentType.XML);
        List<Diff> differences = anyDiff.compare();
        Assert.assertEquals(1, differences.size());
        Assert.assertEquals(8, differences.get(0).getCount());

        differences = anyDiff.ignoreSpaces(true).compare();
        Assert.assertEquals(0, differences.size());
    }
}
