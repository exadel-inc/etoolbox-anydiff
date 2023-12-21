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

import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.diff.DiffEntryType;
import com.exadel.etoolbox.anydiff.diff.DiffState;
import com.exadel.etoolbox.anydiff.diff.EntryHolder;
import com.exadel.etoolbox.anydiff.diff.Fragment;
import com.exadel.etoolbox.anydiff.diff.FragmentHolder;
import com.exadel.etoolbox.anydiff.diff.FragmentPair;
import com.exadel.etoolbox.anydiff.filter.Filter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Contains utility methods for filtering {@link Diff} and {@link DiffEntry} objects
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class FilterHelper {

    /**
     * Gets a {@code Predicate} object that can be used to filter {@link Diff} objects
     * @param filters List of {@link Filter} objects
     * @return A non-null {@code Predicate} object
     */
    static Predicate<Diff> getDiffFilter(List<Filter> filters) {
        return diff -> !shouldExclude(diff, filters);
    }

    /**
     * Gets a {@code Predicate} object that can be used to filter {@link DiffEntry} objects
     * @param filters List of {@link Filter} objects
     * @return A non-null {@code Predicate} object
     */
    static Predicate<DiffEntry> getEntryFilter(List<Filter> filters) {
        return entry -> !shouldExclude(entry, filters);
    }

    private static boolean shouldExclude(Diff diff, List<Filter> rules) {
        if (diff.getState() == DiffState.UNCHANGED) {
            return true;
        }
        if (CollectionUtils.isEmpty(rules)) {
            return false;
        }
        for (Filter rule : rules) {
            if (invokeSilently(rule::skipDiff, diff)) {
                return true;
            }
            if (invokeSilently(rule::acceptDiff, diff)) {
                diff.children().forEach(DiffEntry::accept);
                return false;
            }
        }
        return false;
    }

    private static boolean shouldExclude(DiffEntry entry, List<Filter> rules) {
        if (entry.getState() == DiffState.UNCHANGED) {
            return true;
        }
        if (CollectionUtils.isEmpty(rules)) {
            return false;
        }
        DiffEntryType diffEntryType = DiffEntryType.from(entry.getName());
        for (Filter rule : rules) {
            if (diffEntryType == DiffEntryType.BLOCK && invokeSilently(rule::acceptBlock, entry)) {
                entry.accept();
                return false;
            }
            if (diffEntryType == DiffEntryType.BLOCK && invokeSilently(rule::skipBlock, entry)) {
                return true;
            }
            if (diffEntryType == DiffEntryType.LINE && invokeSilently(rule::acceptLine, entry)) {
                entry.accept();
                return false;
            }
            if (diffEntryType == DiffEntryType.LINE && invokeSilently(rule::skipLine, entry)) {
                return true;
            }
            if (diffEntryType == DiffEntryType.FRAGMENT_PAIR && invokeSilently(rule::acceptFragments, (FragmentPair) entry)) {
                entry.accept();
                return false;
            }
            if (diffEntryType == DiffEntryType.FRAGMENT_PAIR && invokeSilently(rule::skipFragments, (FragmentPair) entry)) {
                return true;
            }
        }
        return shouldExcludeByChildren(entry, rules) || shouldExcludeByFragments(entry, rules);
    }

    private static boolean shouldExclude(FragmentHolder holder, Fragment fragment, List<Filter> rules) {
        if (CollectionUtils.isEmpty(rules)) {
            return false;
        }
        for (Filter rule : rules) {
            if (invokeSilently(rule::skipFragment, fragment)) {
                return true;
            }
            if (invokeSilently(rule::acceptFragment, fragment)) {
                holder.accept(fragment);
                break;
            }
        }
        return false;
    }

    private static boolean shouldExcludeByChildren(DiffEntry entry, List<Filter> rules) {
        List<? extends DiffEntry> children = entry instanceof EntryHolder
                ? ((EntryHolder) entry).children()
                : Collections.emptyList();
        List<? extends DiffEntry> removableChildren = children
                .stream()
                .filter(child -> shouldExclude(child, rules))
                .collect(Collectors.toList());
        if (entry instanceof EntryHolder && !removableChildren.isEmpty()) {
            removableChildren.forEach(((EntryHolder) entry)::exclude);
        }
        boolean allChildrenUnchanged = !children.isEmpty()
                && children.stream().allMatch(child -> child.getState() == DiffState.UNCHANGED);
        return (children.isEmpty() && !removableChildren.isEmpty()) || allChildrenUnchanged;
    }

    private static boolean shouldExcludeByFragments(DiffEntry entry, List<Filter> rules) {
        if (DiffEntryType.from(entry.getName()) != DiffEntryType.LINE) {
            return false;
        }
        FragmentHolder fragmentHolder = (FragmentHolder) entry;
        List<Fragment> fragments = fragmentHolder.getFragments();
        int removedFragmentsCount = 0;
        for (Fragment fragment : fragments) {
            if (!shouldExclude(fragmentHolder, fragment, rules)) {
                continue;
            }
            fragmentHolder.exclude(fragment);
            removedFragmentsCount++;
        }
        return removedFragmentsCount == fragments.size() && removedFragmentsCount > 0;
    }

    private static <T> boolean invokeSilently(Predicate<T> predicate, T value) {
        try {
            return predicate.test(value);
        } catch (Exception e) {
            return false;
        }

    }
}
