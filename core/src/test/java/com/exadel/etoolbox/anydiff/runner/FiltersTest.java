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
package com.exadel.etoolbox.anydiff.runner;

import com.exadel.etoolbox.anydiff.AnyDiff;
import com.exadel.etoolbox.anydiff.ContentType;
import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.diff.EntryHolder;
import com.exadel.etoolbox.anydiff.filter.FilterFactory;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

public class FiltersTest {

    @Test
    public void shouldUseSimpleRule() throws IOException {
        testSimpleRule("skipAllBlocks");
    }

    @Test
    public void shouldUseDiffRule() throws IOException {
        testSimpleRule("skipChangedDiff");
    }

    @Test
    public void shouldUseDiffRule2() throws IOException, URISyntaxException {
        URL left = FiltersTest.class.getResource("/sample/left/html/file2.html");
        URL right = FiltersTest.class.getResource("/sample/right/html/file2.html");
        Assert.assertNotNull(left);
        Assert.assertNotNull(right);

        File leftFile = Paths.get(left.toURI()).toFile();
        File rightFile = Paths.get(right.toURI()).toFile();

        String jsRule = readRule("skipDiffByFileName");
        try (FilterFactory ruleFactory = new FilterFactory()) {
            ruleFactory.useScript(jsRule);
            List<Diff> result = new AnyDiff()
                    .left(leftFile.getAbsolutePath())
                    .right(rightFile.getAbsolutePath())
                    .contentType(ContentType.HTML)
                    .filter(ruleFactory.getFilters())
                    .compare();
            Assert.assertTrue(AnyDiff.isMatch(result));
        }
    }

    @Test
    public void shouldUseBlockRule() throws IOException {
        String left = readSource("left/html/file2.html");
        String right = readSource("right/html/file2.html");

        String jsRule = readRule("skipBlock");
        try (FilterFactory ruleFactory = new FilterFactory()) {
            ruleFactory.useScript(jsRule);
            List<Diff> result = new AnyDiff()
                    .left(left)
                    .right(right)
                    .contentType(ContentType.HTML)
                    .filter(ruleFactory.getFilters())
                    .compare();
            Assert.assertTrue(AnyDiff.isMatch(result));
        }
    }

    @Test
    public void shouldUseLineRule() throws IOException {
        String left = readSource("left/html/file2.html");
        String right = readSource("right/html/file2.html");

        List<Diff> result = new AnyDiff()
                .left(left)
                .right(right)
                .contentType(ContentType.HTML)
                .ignoreSpaces(true)
                .compare();
        Assert.assertEquals(2, result.get(0).children().size());
        DiffEntry block = result.get(0).children().get(0);
        Assert.assertEquals(4, block.as(EntryHolder.class).children().size());
        Assert.assertFalse(AnyDiff.isMatch(result));

        String jsRule = readRule("skipLine");
        try (FilterFactory ruleFactory = new FilterFactory()) {
            ruleFactory.useScript(jsRule);
            result = new AnyDiff()
                    .left(left)
                    .right(right)
                    .contentType(ContentType.HTML)
                    .ignoreSpaces(true)
                    .filter(ruleFactory.getFilters())
                    .compare();
            Assert.assertEquals(1, result.get(0).children().size());
            Assert.assertFalse(AnyDiff.isMatch(result));
        }

        String jsRule2 = readRule("skipLine2");
        try (FilterFactory ruleFactory = new FilterFactory()) {
            ruleFactory.useScript(jsRule);
            ruleFactory.useScript(jsRule2);
            result = new AnyDiff()
                    .left(left)
                    .right(right)
                    .contentType(ContentType.HTML)
                    .ignoreSpaces(true)
                    .filter(ruleFactory.getFilters())
                    .compare();
            Assert.assertTrue(AnyDiff.isMatch(result));
        }
    }

    @Test
    public void shouldUseFragmentRule() throws IOException {
        String left = readSource("left/html/file2.html");
        String right = readSource("right/html/file2.html");

        String jsRule = readRule("skipFragment");
        try (FilterFactory ruleFactory = new FilterFactory()) {
            ruleFactory.useScript(jsRule);
            List<Diff> result = new AnyDiff()
                    .left(left)
                    .right(right)
                    .contentType(ContentType.HTML)
                    .ignoreSpaces(true)
                    .filter(ruleFactory.getFilters())
                    .compare();
            Assert.assertTrue(AnyDiff.isMatch(result));
        }
    }

    @Test
    public void shouldUseFragmentRule2() throws IOException {
        String left = readSource("left/xml/file3.xml");
        String right = readSource("right/xml/file3.xml");

        String jsRule = readRule("skipFragment2");
        try (FilterFactory ruleFactory = new FilterFactory()) {
            ruleFactory.useScript(jsRule);
            List<Diff> result = new AnyDiff()
                    .left(left)
                    .right(right)
                    .contentType(ContentType.HTML)
                    .filter(ruleFactory.getFilters())
                    .compare();
            Assert.assertTrue(AnyDiff.isMatch(result));
        }
    }
    @Test
    public void shouldUseFragmentPairRule() throws IOException {
        testSimpleRule("skipFragmentPair");
    }

    private void testSimpleRule(String name) throws IOException {
        Assert.assertFalse(new AnyDiff().left(FilterHelperTest.LEFT).right(FilterHelperTest.RIGHT).isMatch());
        String jsRule = readRule(name);
        try (FilterFactory ruleFactory = new FilterFactory()) {
            ruleFactory.useScript(jsRule);
            List<Diff> result = new AnyDiff()
                    .left(FilterHelperTest.LEFT)
                    .right(FilterHelperTest.RIGHT)
                    .filter(ruleFactory.getFilters())
                    .compare();
            Assert.assertTrue(AnyDiff.isMatch(result));
        }
    }

    private static String readRule(String name) throws IOException {
        URL url = FiltersTest.class.getResource("/filter/" + name + ".js");
        Assert.assertNotNull(url);
        try (InputStream input = url.openStream()) {
            return IOUtils.toString(input, StandardCharsets.UTF_8);
        }
    }

    private static String readSource(String path) throws IOException {
        URL url = FiltersTest.class.getResource("/sample/" + path);
        Assert.assertNotNull(url);
        try (InputStream input = url.openStream()) {
            return IOUtils.toString(input, StandardCharsets.UTF_8);
        }
    }
}
