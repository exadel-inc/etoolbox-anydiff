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
import com.exadel.etoolbox.anydiff.comparison.DiffTask;
import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.diff.EntryHolder;
import com.exadel.etoolbox.anydiff.diff.Fragment;
import com.exadel.etoolbox.anydiff.diff.FragmentHolder;
import com.exadel.etoolbox.anydiff.diff.FragmentPair;
import com.exadel.etoolbox.anydiff.diff.MarkupFragment;
import com.exadel.etoolbox.anydiff.filter.Filter;
import lombok.RequiredArgsConstructor;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class FilterHelperTest {

    static final String LEFT = "Lorem ipsum\nDolor sit amet\nConsectetur adipiscing elit";
    static final String RIGHT = "Lorem ipsum\nDolor sat amet\nConsectetur adipiscing elit";

    @Test
    public void shouldNotFilterWhenNoRuleMatches() {
        List<? extends DiffEntry> entries = DiffTask.builder().leftContent(LEFT).rightContent(RIGHT).build().run().children();
        Assert.assertEquals(1, entries.size()); // Number of blocks
        Assert.assertEquals(3, entries.get(0).as(EntryHolder.class).children().size()); // Number of lines in a block

        DiffEntry block = entries.get(0);
        Predicate<DiffEntry> processingFilter = FilterHelper.getEntryFilter(Collections.singletonList(new SkipNone()));
        Assert.assertTrue(processingFilter.test(block));
    }

    @Test
    public void shouldFilterFullDiff() {
        List<Diff> result = new AnyDiff()
                .left(LEFT)
                .right(RIGHT)
                .filter(Collections.singletonList(new SkipAnyDiff()))
                .compare();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldFilterByBlock() {
        List<? extends DiffEntry> entries = DiffTask
                .builder()
                .leftContent(LEFT)
                .rightContent(RIGHT.replace("elit", "ilet"))
                .build()
                .run()
                .children();
        Assert.assertEquals(1, entries.size());

        DiffEntry block = entries.get(0);
        Predicate<DiffEntry> processingFilter = FilterHelper.getEntryFilter(Collections.singletonList(new SkipBlock()));
        Assert.assertFalse(processingFilter.test(block));
    }

    @Test
    public void shouldFilterByLine() {
        List<? extends DiffEntry> entries = DiffTask
                .builder()
                .leftContent(LEFT)
                .rightContent(RIGHT)
                .build()
                .run()
                .children();
        Assert.assertEquals(1, entries.size());

        DiffEntry block = entries.get(0);
        Predicate<DiffEntry> processingFilter = FilterHelper.getEntryFilter(Collections.singletonList(new SkipWhenContainsFragment("sit")));
        Assert.assertFalse(processingFilter.test(block));
    }

    @Test
    public void shouldFilterByFragmentPair() {
        List<? extends DiffEntry> entries = DiffTask
                .builder()
                .leftContent(LEFT)
                .rightContent(RIGHT)
                .build()
                .run()
                .children();
        Assert.assertEquals(1, entries.size());

        DiffEntry block = entries.get(0);
        Predicate<DiffEntry> processingFilter = FilterHelper.getEntryFilter(Arrays.asList(
                new SkipNone(),
                new SkipFragmentPair("sit", "sat")));
        Assert.assertFalse(processingFilter.test(block));
    }

    @Test
    public void shouldFilterMarkupByFragmentPair() {
        List<? extends DiffEntry> entries = DiffTask
                .builder()
                .contentType(ContentType.HTML)
                .leftContent("<div class=\"lorem\">Lorem <span>ipsum</span> dolor sit amet</div> consectetur adipiscing elit")
                .rightContent("<div class=\"lorum\">Lorem <span>dolorum</span> dolor sat amet</div> consectetur adipiscing elet")
                .build()
                .run()
                .children();
        Assert.assertEquals(3, entries.size());

        DiffEntry block0 = entries.get(0);
        Predicate<DiffEntry> processingFilter = FilterHelper.getEntryFilter(Arrays.asList(new SkipNone(), new SkipMarkupFragments()));
        boolean testResult = processingFilter.test(block0);
        List<Fragment> fragments = block0.as(EntryHolder.class).children().get(1).as(FragmentHolder.class).getFragments();
        Assert.assertTrue(testResult);
        Assert.assertTrue(fragments.stream().noneMatch(Fragment::isPending));

        DiffEntry block1 = entries.get(1);
        processingFilter = FilterHelper.getEntryFilter(Arrays.asList(
                new SkipMarkupFragments(),
                new SkipFragmentPair("sit", "sat")
        ));
        testResult = processingFilter.test(block1);
        Assert.assertFalse(testResult);

        DiffEntry block2 = entries.get(2);
        processingFilter = FilterHelper.getEntryFilter(Arrays.asList(
                new SkipMarkupFragments(),
                new SkipFragmentPair("elit", "elet")
        ));
        testResult = processingFilter.test(block2);
        Assert.assertFalse(testResult);
    }

    @Test
    public void shouldFilterByFragment() {
        List<? extends DiffEntry> entries = DiffTask
                .builder()
                .leftContent(LEFT)
                .rightContent(RIGHT)
                .build()
                .run()
                .children();
        Assert.assertEquals(1, entries.size());

        Predicate<DiffEntry> processingFilter = FilterHelper.getEntryFilter(Collections.singletonList(new SkipFragment("sit")));
        Assert.assertTrue(processingFilter.test(entries.get(0)));

        processingFilter = FilterHelper.getEntryFilter(Arrays.asList(new SkipFragment("sit"), new SkipFragment("sat")));
        Assert.assertFalse(processingFilter.test(entries.get(0)));
    }

    /* ----------
       Rule cases
       ---------- */

    private static class SkipNone implements Filter {
    }

    private static class SkipAnyDiff implements Filter {
        @Override
        public boolean skipDiff(Diff value) {
            return true;
        }
    }

    private static class SkipBlock implements Filter {
        @Override
        public boolean skipBlock(DiffEntry value) {
            return value.getLeft().contains("sit") && value.getLeft().contains("elit");
        }
    }

    @RequiredArgsConstructor
    private static class SkipWhenContainsFragment implements Filter {
        private final String fragment;

        @Override
        public boolean skipLine(DiffEntry value) {
            return value instanceof FragmentHolder
                    && ((FragmentHolder) value)
                    .getLeftFragments()
                    .stream()
                    .anyMatch(f -> f.isDelete() && f.toString().equals(fragment));
        }
    }

    @RequiredArgsConstructor
    private static class SkipFragment implements Filter {
        private final String fragment;

        @Override
        public boolean skipFragment(Fragment value) {
            return fragment.equals(value.toString());
        }
    }

    @RequiredArgsConstructor
    private static class SkipFragmentPair implements Filter {
        private final String leftSample;
        private final String rightSample;

        @Override
        public boolean skipFragments(FragmentPair value) {
            return value.getLeftFragment().equals(leftSample) && value.getRightFragment().equals(rightSample);
        }
    }

    private static class SkipMarkupFragments implements Filter {
        @Override
        public boolean acceptFragments(FragmentPair value) {
            return value.getLeftFragment().equals("lorem")
             && value.getRightFragment().toString().equals("lorum");
        }

        @Override
        public boolean skipFragments(FragmentPair value) {
            return value.getLeftFragment().as(MarkupFragment.class).isTagContent("span")
                    && value.getRightFragment().as(MarkupFragment.class).isTagContent("span");
        }
    }
}