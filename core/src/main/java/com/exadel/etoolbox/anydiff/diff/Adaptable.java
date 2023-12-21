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

import org.apache.commons.lang3.ClassUtils;

/**
 * Represents an object that can be adapted to a different type
 */
public interface Adaptable {

    /**
     * Adapts the current object to the specified type
     * @param type {@code Class} object representing the desired type
     * @param <T> Type parameter
     * @return An object of the specified type, or {@code null} if the adaptation failed
     */
    default <T> T as(Class<T> type) {
        if (ClassUtils.isAssignable(getClass(), type)) {
            return type.cast(this);
        }
        return null;
    }
}
