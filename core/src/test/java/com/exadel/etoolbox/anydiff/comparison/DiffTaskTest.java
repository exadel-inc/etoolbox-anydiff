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

import com.exadel.etoolbox.anydiff.ContentType;
import com.exadel.etoolbox.anydiff.OutputType;
import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.diff.DiffState;
import com.exadel.etoolbox.anydiff.diff.EntryHolder;
import com.exadel.etoolbox.anydiff.diff.PrintableEntry;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DiffTaskTest {

    @Test
    public void shouldReturnEmptyDiff() {
        Diff diff = DiffTask
                .builder()
                .leftContent("Lorem ipsum")
                .rightContent("Lorem ipsum")
                .build()
                .run();
        Assert.assertEquals(DiffState.UNCHANGED, diff.getState());
        Assert.assertTrue(diff.children().isEmpty());
    }

    @Test
    public void shouldReturnSimpleDiff() {
        Diff diff = DiffTask
                .builder()
                .leftContent("Lorem ipsum dolor sit amet")
                .rightContent("Lorem consectetur dolor sit amet")
                .build()
                .run();
        Assert.assertEquals(DiffState.CHANGE, diff.getState());

        Assert.assertEquals(1, diff.children().size());
        DiffEntry block = diff.children().get(0);
        Assert.assertTrue(block instanceof BlockImpl);

        List<? extends DiffEntry> lines = block.as(EntryHolder.class).children();
        Assert.assertEquals(1, lines.size());
        DiffEntry line = lines.get(0);
        Assert.assertTrue(line instanceof LineImpl);

        List<? extends DiffEntry> fragmentPairs = line.as(EntryHolder.class).children();
        Assert.assertEquals(1, fragmentPairs.size());
        DiffEntry fragmentPair = fragmentPairs.get(0);
        Assert.assertTrue(fragmentPair instanceof FragmentPairImpl);

        diff.children().forEach(e -> System.out.println(((PrintableEntry) e).toString(OutputType.CONSOLE)));
    }

    @Test
    public void shouldReturnBlockDiffFromTxt() throws IOException {
        try (
                InputStream leftInput = getClass().getResourceAsStream("/sample/left/file1.txt");
                InputStream rightInput = getClass().getResourceAsStream("/sample/right/file1.txt")
        ) {
            Assert.assertNotNull(leftInput);
            Assert.assertNotNull(rightInput);
            String left = IOUtils.toString(leftInput, StandardCharsets.UTF_8);
            String right = IOUtils.toString(rightInput, StandardCharsets.UTF_8);
            Diff diff = DiffTask
                    .builder()
                    .leftContent(left)
                    .rightContent(right)
                    .build()
                    .run();
            Assert.assertEquals(DiffState.CHANGE, diff.getState());
            Assert.assertEquals(4, diff.children().size());
            Assert.assertTrue(diff.children().get(0).getState().isChange());
            Assert.assertTrue(diff.children().get(1).getState().isChange());
            Assert.assertTrue(diff.children().get(2).getState().isDeletion());
            Assert.assertTrue(diff.children().get(3).getState().isChange());

            diff.children().forEach(entry -> System.out.println(((PrintableEntry) entry).toString(OutputType.CONSOLE)));
        }
    }

    @Test
    public void shouldReturnBlockDiffFromHtml() throws IOException {
        try (
                InputStream leftInput = getClass().getResourceAsStream("/sample/left/html/file2.html");
                InputStream rightInput = getClass().getResourceAsStream("/sample/right/html/file2.html")
        ) {
            Assert.assertNotNull(leftInput);
            Assert.assertNotNull(rightInput);
            String left = IOUtils.toString(leftInput, StandardCharsets.UTF_8);
            String right = IOUtils.toString(rightInput, StandardCharsets.UTF_8);
            Diff diff = DiffTask
                    .builder()
                    .leftContent(left)
                    .rightContent(right)
                    .contentType(ContentType.HTML)
                    .taskParameters(TaskParameters.builder().ignoreSpaces(true).build())
                    .build()
                    .run();
            Assert.assertEquals(DiffState.CHANGE, diff.getState());
            Assert.assertEquals(2, diff.children().size());

            Assert.assertTrue(diff.children().get(0).getState().isChange());
            Assert.assertTrue(diff.children().get(1).getState().isChange());

            diff.children().forEach(entry -> System.out.println(((PrintableEntry) entry).toString(OutputType.CONSOLE)));
        }
    }

    @Test
    public void shouldReturnBlockDiffFromXml() throws IOException {
        try (
                InputStream leftInput = getClass().getResourceAsStream("/sample/left/xml/file3.xml");
                InputStream rightInput = getClass().getResourceAsStream("/sample/right/xml/file3.xml")
        ) {
            Assert.assertNotNull(leftInput);
            Assert.assertNotNull(rightInput);
            String left = IOUtils.toString(leftInput, StandardCharsets.UTF_8);
            String right = IOUtils.toString(rightInput, StandardCharsets.UTF_8);
            Diff diff = DiffTask
                    .builder()
                    .leftContent(left)
                    .rightContent(right)
                    .contentType(ContentType.XML)
                    .build()
                    .run();
            Assert.assertEquals(DiffState.CHANGE, diff.getState());
            Assert.assertEquals(3, diff.children().size());
            Assert.assertTrue(diff.children().get(0).getState().isChange());
            Assert.assertTrue(diff.children().get(1).getState().isChange());
            Assert.assertTrue(diff.children().get(2).getState().isDeletion());

            diff.children().forEach(entry -> System.out.println(((PrintableEntry) entry).toString(OutputType.CONSOLE)));
        }
    }
}