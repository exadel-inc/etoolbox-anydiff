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

import com.exadel.etoolbox.anydiff.comparison.DiffTask;
import com.exadel.etoolbox.anydiff.comparison.Marker;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * A simple DTO entity that is used to pass the information about a moved file to a
 * {@link DiffTask} object
 */
@RequiredArgsConstructor
@Getter
class FileMoveInfo {

    /**
     * Gets the path to the file that has been moved
     */
    private final String path;

    @Override
    public String toString() {
        return Marker.CONTEXT.wrap("Moved to:") + StringUtils.SPACE + Marker.PLACEHOLDER.wrap(path);
    }
}
