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
package com.exadel.etoolbox.anydiff.comparison.postprocessor;

import com.exadel.etoolbox.anydiff.ContentType;
import com.exadel.etoolbox.anydiff.comparison.TaskParameters;
import com.github.difflib.text.DiffRow;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Represents a routine that post-processes the result of a comparison operation.
 * <u>Note</u>: This class is not a part of public API and is subject to change. You should not use it directly
 */
public abstract class Postprocessor implements UnaryOperator<List<DiffRow>> {

    /**
     * Creates a {@code Postprocessor} instance for the specified content type and formatting
     * @param type       {@link ContentType} value
     * @param parameters {@link TaskParameters} object. A non-null value is expected
     * @return {@code Postprocessor} instance
     */
    public static Postprocessor forType(ContentType type, TaskParameters parameters) {
        if ((type == ContentType.XML || type == ContentType.HTML) && parameters.normalize()) {
            return new XmlPostprocessor(parameters);
        }
        return BasicPostprocessor.INSTANCE;
    }
}
