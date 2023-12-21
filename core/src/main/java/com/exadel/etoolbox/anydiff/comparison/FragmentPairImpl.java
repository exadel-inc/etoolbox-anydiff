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
import com.exadel.etoolbox.anydiff.diff.Fragment;
import com.exadel.etoolbox.anydiff.diff.FragmentPair;
import lombok.RequiredArgsConstructor;

/**
 * Implements {@link DiffEntry} to represent a pair of fragments that contain a difference
 * @see DiffEntry
 * @see FragmentPair
 */
@RequiredArgsConstructor
class FragmentPairImpl implements DiffEntry, FragmentPair {

    private final LineImpl line;
    private final Fragment left;
    private final Fragment right;

    /* ---------
       Accessors
       --------- */

    @Override
    public Diff getDiff() {
        return line.getDiff();
    }

    @Override
    public DiffState getState() {
        return DiffState.CHANGE;
    }

    /**
     * Gets whether fragments of the current pair have not been "silenced" (accepted) via a
     * {@link com.exadel.etoolbox.anydiff.filter.Filter}
     * @return True or false
     */
    boolean isPending() {
        return (left == null || left.isPending()) && (right == null || right.isPending());
    }

    /* -------
       Content
       ------- */

    @Override
    public String getLeft(boolean includeContext) {
        return getLeft();
    }

    @Override
    public String getLeft() {
        return left.toString();
    }

    @Override
    public String getRight(boolean includeContext) {
        return getRight();
    }

    @Override
    public String getRight() {
        return right.toString();
    }

    @Override
    public Fragment getLeftFragment() {
        return left;
    }

    @Override
    public Fragment getRightFragment() {
        return right;
    }

    /* ----------
       Operations
       ---------- */

    @Override
    public void accept() {
        line.accept(left);
        line.accept(right);
    }
}
