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
import com.exadel.etoolbox.anydiff.diff.DiffState;
import com.exadel.etoolbox.anydiff.diff.EntryHolder;
import com.exadel.etoolbox.anydiff.diff.Fragment;
import com.exadel.etoolbox.anydiff.diff.FragmentHolder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class DiffTest {

    private Diff diff;

    @Before
    public void setUp() {
        LineImpl line1 = new LineImpl(
                new MarkedString("{{eq}}Lorem ipsum{{/}}"),
                new MarkedString("Lorem ipsum"));
        LineImpl line2 = new LineImpl(
                new MarkedString("Dolor {{del}}sit{{/}} am{{del}}it{{/}}"),
                new MarkedString("Dolor {{ins}}set{{/}} am{{ins}}et{{/}}"));
        LineImpl line3 = new LineImpl(
                null,
                new MarkedString("{{ins}}Consectetur adipiscing elit{{/}}"));
        LineImpl line4 = new LineImpl(
                new MarkedString("{{eq}}sed do eiusmod tempor incididunt{{/}}"),
                new MarkedString("{{eq}}sed do eiusmod tempor incididunt{{/}}"));
        LineImpl line5 = new LineImpl(
                new MarkedString("{{del}}ut labore et dolore magna aliqua{{/}}"),
                null);
        BlockImpl block1 = BlockImpl
                .builder()
                .lines(Arrays.asList(line1, line2, line3, line4))
                .build(BlockImpl::new);
        BlockImpl block2 = BlockImpl
                .builder()
                .lines(Arrays.asList(line4, line5))
                .build(BlockImpl::new);
        diff = new DiffImpl("left", "right").withChildren(block1, block2);
    }

    @Test
    public void shouldReportTypeAndLines() {
        DiffEntry block1 = diff.children().get(0);
        Assert.assertEquals(DiffState.CHANGE, block1.getState());
        int linesCount = block1.as(EntryHolder.class).children().size();
        Assert.assertEquals(4, linesCount);
        Assert.assertEquals(3,((AbstractBlock) block1).getPendingCount());

        DiffEntry block2 = diff.children().get(1);
        linesCount = block2.as(EntryHolder.class).children().size();
        Assert.assertEquals(2, linesCount);
        Assert.assertEquals(DiffState.RIGHT_MISSING, block2.getState());
        Assert.assertEquals(1, ((AbstractBlock) block2).getPendingCount());
    }

    @Test
    public void shouldReportFragmentPairs() {
        DiffEntry block = diff.children().get(0);
        DiffEntry line = block.as(EntryHolder.class).children().get(1);
        Assert.assertEquals(2, line.as(EntryHolder.class).children().size());
    }

    @Test
    public void shouldReportFragments() {
        DiffEntry block1 = diff.children().get(0);
        Assert.assertEquals(2, block1.as(FragmentHolder.class).getLeftFragments().size());
        Assert.assertEquals(3, block1.as(FragmentHolder.class).getRightFragments().size());

        DiffEntry block2 = diff.children().get(1);
        Assert.assertEquals(1, block2.as(FragmentHolder.class).getLeftFragments().size());
        Assert.assertEquals(0, block2.as(FragmentHolder.class).getRightFragments().size());
    }

    @Test
    public void shouldSkipBlocks() {
        Assert.assertEquals(DiffState.CHANGE, diff.getState());
        diff.exclude(diff.children().get(0));
        Assert.assertEquals(DiffState.RIGHT_MISSING, diff.getState());
        diff.exclude(diff.children().get(0));
        Assert.assertEquals(DiffState.UNCHANGED, diff.getState());
    }

    @Test
    public void shouldSkipLines() {
        Assert.assertEquals(DiffState.CHANGE, diff.getState());

        DiffEntry block1 = diff.children().get(0);
        Assert.assertEquals(DiffState.CHANGE, block1.getState());
        block1.as(EntryHolder.class).exclude(block1.as(EntryHolder.class).children().get(1));
        Assert.assertEquals(DiffState.LEFT_MISSING, block1.getState());
        block1.as(EntryHolder.class).exclude(block1.as(EntryHolder.class).children().get(2));
        Assert.assertEquals(DiffState.UNCHANGED, block1.getState());

        DiffEntry block2 = diff.children().get(1);
        Assert.assertEquals(DiffState.RIGHT_MISSING, block2.getState());
        block2.as(EntryHolder.class).exclude(block2.as(EntryHolder.class).children().get(1));
        Assert.assertEquals(DiffState.UNCHANGED, block2.getState());

        Assert.assertEquals(DiffState.UNCHANGED, diff.getState());
    }

    @Test
    public void shouldSkipFragmentPairs() {
        Assert.assertEquals(DiffState.CHANGE, diff.getState());

        DiffEntry block = diff.children().get(0);
        Assert.assertEquals(DiffState.CHANGE, block.getState());

        DiffEntry line = block.as(EntryHolder.class).children().get(1);
        Assert.assertEquals(DiffState.CHANGE, line.getState());

        DiffEntry fragmentPair = line.as(EntryHolder.class).children().get(0);
        Assert.assertEquals(DiffState.CHANGE, fragmentPair.getState());
        line.as(EntryHolder.class).exclude(fragmentPair);
        Assert.assertEquals(DiffState.CHANGE, line.getState());

        fragmentPair = line.as(EntryHolder.class).children().get(0);
        Assert.assertEquals(DiffState.CHANGE, fragmentPair.getState());
        line.as(EntryHolder.class).exclude(fragmentPair);
        Assert.assertEquals(DiffState.UNCHANGED, line.getState());
        Assert.assertEquals(DiffState.LEFT_MISSING, block.getState());
    }

    @Test
    public void shouldSkipFragments() {
        DiffEntry block1 = diff.children().get(0);
        Assert.assertEquals(DiffState.CHANGE, block1.getState());
        block1.as(FragmentHolder.class).getFragments().forEach(fragment -> block1.as(FragmentHolder.class).exclude(fragment));
        Assert.assertEquals(DiffState.UNCHANGED, block1.getState());

        DiffEntry block2 = diff.children().get(1);
        Assert.assertEquals(DiffState.RIGHT_MISSING, block2.getState());
        List<Fragment> block2LeftFragments = block2.as(FragmentHolder.class).getLeftFragments();
        Assert.assertEquals(1, block2LeftFragments.size());
        block2LeftFragments.forEach(fragment -> block2.as(FragmentHolder.class).exclude(fragment));
        Assert.assertEquals(DiffState.UNCHANGED, block2.getState());
    }

    @Test
    public void shouldAcceptBlocks() {
        DiffEntry block1 = diff.children().get(0);
        Assert.assertEquals(3, ((AbstractBlock) block1).getPendingCount());
        block1.accept();
        Assert.assertEquals(0, ((AbstractBlock) block1).getPendingCount());

        DiffEntry block2 = diff.children().get(1);
        Assert.assertEquals(1, ((AbstractBlock) block2).getPendingCount());
        block2.accept();
        Assert.assertEquals(0, ((AbstractBlock) block2).getPendingCount());
    }

    @Test
    public void shouldAcceptLines() {
        DiffEntry block1 = diff.children().get(0);
        Assert.assertEquals(3, ((AbstractBlock) block1).getPendingCount());

        DiffEntry line = block1.as(EntryHolder.class).children().get(1);
        line.accept();
        Assert.assertEquals(1, ((AbstractBlock) block1).getPendingCount());

        line = block1.as(EntryHolder.class).children().get(2);
        line.accept();
        Assert.assertEquals(0, ((AbstractBlock) block1).getPendingCount());

        DiffEntry block2 = diff.children().get(1);
        Assert.assertEquals(1, ((AbstractBlock) block2).getPendingCount());

        line = block2.as(EntryHolder.class).children().get(1);
        line.accept();
        Assert.assertEquals(0, ((AbstractBlock) block2).getPendingCount());
    }

    @Test
    public void shouldAcceptFragmentPairs() {
        DiffEntry block1 = diff.children().get(0);
        Assert.assertEquals(DiffState.CHANGE, block1.getState());
        Assert.assertEquals(3, ((AbstractBlock) block1).getPendingCount());

        DiffEntry line = block1.as(EntryHolder.class).children().get(1);
        DiffEntry fragmentPair = line.as(EntryHolder.class).children().get(0);
        fragmentPair.accept();
        Assert.assertEquals(2, ((AbstractBlock) block1).getPendingCount());

        fragmentPair = line.as(EntryHolder.class).children().get(1);
        fragmentPair.accept();
        Assert.assertEquals(1, ((AbstractBlock) block1).getPendingCount());

        line = block1.as(EntryHolder.class).children().get(2);
        Fragment fragment = line.as(FragmentHolder.class).getFragments().get(0);
        line.as(FragmentHolder.class).accept(fragment);
        Assert.assertEquals(0, ((AbstractBlock) block1).getPendingCount());
    }

    @Test
    public void shouldAcceptFragments() {
        DiffEntry block1 = diff.children().get(0);
        Assert.assertEquals(DiffState.CHANGE, block1.getState());
        Assert.assertEquals(3, ((AbstractBlock) block1).getPendingCount());

        for (DiffEntry line : block1.as(EntryHolder.class).children()) {
            FragmentHolder fragmentHolder = line.as(FragmentHolder.class);
            fragmentHolder.getLeftFragments().forEach(fragmentHolder::accept);
            fragmentHolder.getRightFragments().forEach(fragmentHolder::accept);
        }
        Assert.assertEquals(DiffState.CHANGE, block1.getState());
        Assert.assertEquals(0, ((AbstractBlock) block1).getPendingCount());

        DiffEntry block2 = diff.children().get(1);
        Assert.assertEquals(DiffState.RIGHT_MISSING, block2.getState());
        Assert.assertEquals(1, ((AbstractBlock) block2).getPendingCount());
        for (DiffEntry line : block2.as(EntryHolder.class).children()) {
            FragmentHolder fragmentHolder = line.as(FragmentHolder.class);
            fragmentHolder.getLeftFragments().forEach(fragmentHolder::accept);
            fragmentHolder.getRightFragments().forEach(fragmentHolder::accept);
        }
        Assert.assertEquals(0, ((AbstractBlock) block2).getPendingCount());
    }
}