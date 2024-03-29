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
package com.exadel.etoolbox.anydiff.comparison.preprocessor;

import com.exadel.etoolbox.anydiff.Constants;
import com.exadel.etoolbox.anydiff.ContentType;
import com.exadel.etoolbox.anydiff.comparison.TaskParameters;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.function.UnaryOperator;

/**
 * Represents a preprocessor that is applied to the content before comparison.
 * <u>Note</u>: This class is not a part of public API and is subject to change. You should not use it directly
 */
@Getter(value = AccessLevel.PACKAGE)
public abstract class Preprocessor implements UnaryOperator<String> {

    private static final Preprocessor BASIC = new Preprocessor() {
        @Override
        public String apply(String value) {
            return StringUtils.defaultString(value).replace(
                    "\t",
                    StringUtils.repeat(StringUtils.SPACE, Constants.DEFAULT_INDENT));
        }
    };

    private String contentId;

    /**
     * Sets the identifier of the content to be preprocessed
     * @param value String value
     * @return This instance
     */
    public Preprocessor withContentId(String value) {
        contentId = value;
        return this;
    }

    /**
     * Creates a {@code Preprocessor} instance for the specified content type and formatting
     * @param type       {@link ContentType} value
     * @param parameters {@link TaskParameters} object. A non-null value is expected
     * @return {@code Preprocessor} instance
     */
    public static Preprocessor forType(ContentType type, TaskParameters parameters) {
        if (type == null) {
            return BASIC;
        }
        Preprocessor custom = parameters.getPreprocessors().get(type);
        if (custom != null) {
            return custom;
        }
        switch (type) {
            case HTML:
                return parameters.normalize() ? new HtmlPreprocessor(parameters) : BASIC;
            case XML:
                return parameters.normalize() ? new XmlPreprocessor(parameters) : BASIC;
            default:
                return BASIC;
        }
    }
}
