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
import com.exadel.etoolbox.anydiff.diff.MarkupFragment;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class FragmentTest {

    private static final String TEXT_SOURCE = "{{ins}}Lorem ipsum{{/}} dolor sit amet, consectetur adipiscing elit, " +
            "sed do \"{{del}}eiusmod tempor incididunt ut labore et dolore magna aliqua  {{/}}\". Ut enim ad minim " +
            "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea com{{ins}}modo consequat{{/}}";

    private static final String HTML_SOURCE = "<div><p class=\"first\">{{ins}}Lorem ipsum{{/}} dolor sit amet, " +
            "consectetur adipiscing elit,</p><p class=\"{{ins}}sec{{/}}ond\"><span class=\"span text\">sed do {{del}}eiusmod " +
            "tempor incididunt ut labore et dolore magna aliqua{{/}}</span>.</p><p>Ut enim ad minim veniam, {{ins}}<br/>" +
            "{{/}}quis nostrud exercitation ullamco laboris nisi ut aliquip</p></div>";

    private static final String JSON_SOURCE = "[\n{\"lorem\" : \"ipsum\"},\n{\"dolor\":  \"{{ins}}sit{{/}}\", \"amet\"}\n]";

    @Test
    public void shouldExtract() {
        List<Fragment> fragments = new MarkedString(TEXT_SOURCE).getFragments();
        Assert.assertEquals(3, fragments.size());
        Assert.assertEquals("Lorem ipsum", fragments.get(0).toString());
        Assert.assertEquals("modo consequat", fragments.get(2).toString());

        fragments = new MarkedString(HTML_SOURCE).getFragments();
        Assert.assertEquals(4, fragments.size());
        Assert.assertEquals("Lorem ipsum", fragments.get(0).toString());
        Assert.assertEquals("sec", fragments.get(1).toString());
    }

    @Test
    public void shouldTrim() {
        List<Fragment> fragments = new MarkedString(TEXT_SOURCE).getFragments();
        FragmentImpl fragment = (FragmentImpl) fragments.get(1);
        fragment.trim();
        Assert.assertEquals("eiusmod tempor incididunt ut labore et dolore magna aliqua", fragment.toString());
    }

    @Test
    public void shouldExpandToAttribute() {
        List<Fragment> fragments = new MarkedString(HTML_SOURCE).getMarkupFragments(true);
        Fragment attrValue = fragments.get(1);
        Assert.assertTrue(attrValue.as(MarkupFragment.class).isAttributeValue("class"));
        Assert.assertEquals("class=\"second\"", attrValue.as(MarkupFragmentImpl.class).toAttribute().toString());
    }

    @Test
    public void shouldExpandToProperty() {
        List<Fragment> fragments = new MarkedString(JSON_SOURCE).getMarkupFragments(true);
        Fragment attrValue = fragments.get(0);
        Assert.assertTrue(attrValue.as(MarkupFragment.class).isJsonValue("dolor"));
        Assert.assertEquals("\"dolor\":  \"sit\"", attrValue.as(MarkupFragmentImpl.class).toJsonProperty().toString());
    }

    @Test
    public void shouldExpandToTag() {
        List<Fragment> fragments = new MarkedString(HTML_SOURCE).getMarkupFragments(true);

        MarkupFragment p1 = fragments.get(0).as(MarkupFragmentImpl.class);
        Assert.assertFalse(p1.isTagContent("div"));
        Assert.assertEquals(
                "<p class=\"first\">Lorem ipsum dolor sit amet, consectetur adipiscing elit,</p>",
                p1.as(MarkupFragmentImpl.class).toTag().toString());

        MarkupFragment p2 = fragments.get(1).as(MarkupFragmentImpl.class);
        Assert.assertTrue(p2.isInsideTag("p"));
        Assert.assertTrue(p2.isInsideTag("p.second"));
        Assert.assertFalse(p2.isTagContent("p"));
        Assert.assertEquals(
                "<p class=\"second\"><span class=\"span text\">sed do eiusmod tempor incididunt ut labore et dolore " +
                        "magna aliqua</span>.</p>",
                p2.as(MarkupFragmentImpl.class).toTag().toString());

        MarkupFragment span = fragments.get(2).as(MarkupFragmentImpl.class);
        Assert.assertTrue(span.isTagContent("span"));
        Assert.assertTrue(span.isTagContent("span.span"));
        Assert.assertFalse(span.isTagContent("span[class=\"span\"]"));
        Assert.assertEquals(
                "<span class=\"span text\">sed do eiusmod tempor incididunt ut labore et dolore magna aliqua</span>",
                span.as(MarkupFragmentImpl.class).toTag().toString());
    }

    @Test
    public void shouldExpandToIncompleteTag() {
        List<Fragment> fragments = new MarkedString("<p>Lorem <b class=\"b\">{{ins}}ipsum{{/}}")
                .getMarkupFragments(true);
        MarkupFragment fragment = fragments.get(0).as(MarkupFragmentImpl.class);
         Assert.assertFalse(fragment.isTagContent("p"));
        Assert.assertTrue(fragment.isTagContent("b"));
        Assert.assertEquals(
                "<b class=\"b\">ipsum",
                fragment.as(MarkupFragmentImpl.class).toTag().toString());
    }
}
