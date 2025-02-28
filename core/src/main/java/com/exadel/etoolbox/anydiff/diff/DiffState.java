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
package com.exadel.etoolbox.anydiff.diff;

/**
 * Enumerates the "kind" of difference between two strings. E.g., "change", "insertion", "deletion", etc.
 */
public enum DiffState {
    UNCHANGED {
        @Override
        public boolean isChange() {
            return false;
        }

        @Override
        public boolean isInsertion() {
            return false;
        }

        @Override
        public boolean isDeletion() {
            return false;
        }
    },
    CHANGE {
        @Override
        public boolean isChange() {
            return true;
        }

        @Override
        public boolean isInsertion() {
            return false;
        }

        @Override
        public boolean isDeletion() {
            return false;
        }
    },
    LEFT_MISSING {
        @Override
        public boolean isChange() {
            return false;
        }

        @Override
        public boolean isInsertion() {
            return true;
        }

        @Override
        public boolean isDeletion() {
            return false;
        }
    },
    RIGHT_MISSING {
        @Override
        public boolean isChange() {
            return false;
        }

        @Override
        public boolean isInsertion() {
            return false;
        }

        @Override
        public boolean isDeletion() {
            return true;
        }
    },
    ERROR {
        @Override
        public boolean isChange() {
            return false;
        }

        @Override
        public boolean isInsertion() {
            return false;
        }

        @Override
        public boolean isDeletion() {
            return false;
        }
    };

    /**
     * A shortcut method getting whether the difference is a change
     * @return True or false
     */
    public abstract boolean isChange();

    /**
     * A shortcut method getting whether the difference is an insertion
     * @return True or false
     */
    public abstract boolean isInsertion();

    /**
     * A shortcut method getting whether the difference is a deletion
     * @return True or false
     */
    public abstract boolean isDeletion();
}
