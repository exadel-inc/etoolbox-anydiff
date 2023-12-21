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
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.diff.DiffState;
import com.exadel.etoolbox.anydiff.diff.Fragment;
import com.exadel.etoolbox.anydiff.diff.FragmentPair;
import com.exadel.etoolbox.anydiff.diff.MarkupFragment;
import com.exadel.etoolbox.anydiff.filter.Filter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

public class DiffCountTest {

    @Test
    public void shouldCountDifferences() throws URISyntaxException, IOException {
        Pair<String, String> sources = getSources("left/file1.txt", "right/file1.txt");
        String left = sources.getLeft();
        String right = sources.getRight();

        List<Diff> differences = new AnyDiff().left(left).right(right).compare();
        Assert.assertEquals(1, differences.size());
        Diff diff = differences.get(0);

        final int totalCount = 13;
        Assert.assertEquals(totalCount, diff.getCount());
        Assert.assertEquals(4, diff.children().size());

        AbstractBlock block0 = (AbstractBlock) diff.children().get(0);
        Assert.assertEquals(3, block0.getCount());
        AbstractBlock block1 = (AbstractBlock) diff.children().get(1);
        Assert.assertEquals(7, block1.getCount());
        AbstractBlock block2 = (AbstractBlock) diff.children().get(2);
        Assert.assertEquals(1, block2.getCount());
        AbstractBlock block3 = (AbstractBlock) diff.children().get(3);
        Assert.assertEquals(2, block3.getCount());

        Assert.assertEquals(totalCount, diff.getPendingCount());
    }

    @Test
    public void shouldDiscountDifferencesInText() throws URISyntaxException, IOException {
        Pair<String, String> sources = getSources("left/file1.txt", "right/file1.txt");
        String left = sources.getLeft();
        String right = sources.getRight();

        AnyDiff anyDiff = new AnyDiff().left(left).right(right);
        Diff diff = anyDiff.filter(new Filter() {
            @Override
            public boolean acceptFragments(FragmentPair value) {
                return StringUtils.isAllBlank(value.getLeft(), value.getRight());
            }
        }).compare().get(0);

        final int totalCount = 13;
        Assert.assertEquals(totalCount, diff.getCount());
        Assert.assertEquals(12, diff.getPendingCount());

        diff = anyDiff.filter(new Filter() {
            @Override
            public boolean acceptFragments(FragmentPair value) {
                return StringUtils.equalsAny(value.getLeft(), "labore", "dolore");
            }
        }).compare().get(0);
        Assert.assertEquals(totalCount, diff.getCount());
        Assert.assertEquals(10, diff.getPendingCount());

        diff = anyDiff.filter(new Filter() {
            @Override
            public boolean acceptFragment(Fragment value) {
                return StringUtils.equalsAny(value, "commodo", "comodo", "dragon ");
            }
        }).compare().get(0);
        Assert.assertEquals(totalCount, diff.getCount());
        Assert.assertEquals(7, diff.getPendingCount());

        diff = anyDiff.filter(new Filter() {
            @Override
            public boolean acceptLine(DiffEntry value) {
                return value.getLeft().contains("Duis");
            }
        }).compare().get(0);
        Assert.assertEquals(totalCount, diff.getCount());
        Assert.assertEquals(5, diff.getPendingCount());

        diff = anyDiff.filter(new Filter() {
            @Override
            public boolean acceptFragment(Fragment value) {
                return value.length() == 2 && (value.isDelete() || value.isInsert());
            }
        }).compare().get(0);
        Assert.assertEquals(totalCount, diff.getCount());
        Assert.assertEquals(5, diff.getPendingCount());
    }

    @Test
    public void shouldDiscountDifferencesInHtml() throws URISyntaxException, IOException {
        Pair<String, String> sources = getSources("left/html/file2.html", "right/html/file2.html");
        String left = sources.getLeft();
        String right = sources.getRight();

        AnyDiff anyDiff = new AnyDiff().left(left).right(right).contentType(ContentType.HTML).ignoreSpaces(true);
        Diff diff = anyDiff.compare().get(0);

        final int totalCount = 4;
        Assert.assertEquals(totalCount, diff.getCount());
        Assert.assertEquals(totalCount, diff.getPendingCount());

        diff = anyDiff.filter(new Filter() {
            @Override
            public boolean acceptLine(DiffEntry value) {
                return value.getLeft().toLowerCase().contains("lorem");
            }
        }).compare().get(0);
        Assert.assertEquals(totalCount, diff.getCount());
        Assert.assertEquals(1, diff.getPendingCount());

        diff = anyDiff.filter(new Filter() {
            @Override
            public boolean acceptLine(DiffEntry value) {
                return value.getState().isInsertion();
            }
        }).compare().get(0);
        Assert.assertEquals(totalCount, diff.getCount());
        Assert.assertEquals(0, diff.getPendingCount());
    }

    @Test
    public void shouldDiscountDifferencesInXml() throws URISyntaxException, IOException {
        Pair<String, String> sources = getSources("left/xml/file3.xml", "right/xml/file3.xml");
        String left = sources.getLeft();
        String right = sources.getRight();

        AnyDiff anyDiff = new AnyDiff().left(left).right(right).contentType(ContentType.XML).ignoreSpaces(true);
        Diff diff = anyDiff.compare().get(0);

        final int totalCount = 5;
        Assert.assertEquals(totalCount, diff.getCount());
        Assert.assertEquals(totalCount, diff.getPendingCount());

        diff = anyDiff.filter(new Filter() {
            @Override
            public boolean acceptFragment(Fragment value) {
                return value.as(MarkupFragment.class).isAttributeValue("tag");
            }
        }).compare().get(0);
        Assert.assertEquals(totalCount, diff.getCount());
        Assert.assertEquals(4, diff.getPendingCount());

        diff = anyDiff.filter(new Filter() {
            @Override
            public boolean acceptFragment(Fragment value) {
                return value.as(MarkupFragment.class).isInsideTag("title");
            }
        }).compare().get(0);
        Assert.assertEquals(totalCount, diff.getCount());
        Assert.assertEquals(1, diff.getPendingCount());

        diff = anyDiff.filter(new Filter() {
            @Override
            public boolean skipLine(DiffEntry value) {
                return value.getState() == DiffState.CHANGE;
            }
        }).compare().get(0);
        Assert.assertEquals(1, diff.getCount());
        Assert.assertEquals(1, diff.getPendingCount());
    }

    private static Pair<String, String> getSources(String leftPath, String rightPath) throws URISyntaxException, IOException {
        URL resourcesRoot = DiffCountTest.class.getResource("/sample");
        assert resourcesRoot != null;
        String left = IOUtils.toString(
                Paths.get(resourcesRoot.toURI()).resolve(leftPath).toUri(),
                StandardCharsets.UTF_8);
        String right = IOUtils.toString(
                Paths.get(resourcesRoot.toURI()).resolve(rightPath).toUri(),
                StandardCharsets.UTF_8);
        Assert.assertNotNull(left);
        Assert.assertNotNull(right);
        return Pair.of(left, right);
    }
}
