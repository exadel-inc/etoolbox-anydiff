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
package com.exadel.etoolbox.anydiff.filter;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a function definition in the user-defined filter created via {@link FilterFactory}
 */
class FunctionDefinition {

    private final Set<String> tokens;

    /**
     * Gets the name of the function parameter
     */
    @Getter
    private final String parameter;

    /**
     * Gets the function body
     */
    @Getter
    private final String body;

    /**
     * Creates a new {@code FunctionDefinition} instance
     * @param name      Function name
     * @param parameter Function parameter name
     * @param body      Function body
     */
    public FunctionDefinition(String name, String parameter, String body) {
        this.tokens = Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(name))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        this.parameter = parameter;
        this.body = body;
    }

    /**
     * Gets whether this function is of the {@code accept} type
     * @return True or false
     * @see Filter
     */
    boolean isAccept() {
        return tokens.contains("accept") || CollectionUtils.containsAll(tokens, Arrays.asList("skip", "log"));
    }

    /**
     * Gets whether this function is of the {@code skip} type
     * @return True or false
     * @see Filter
     */
    boolean isSkip() {
        return tokens.contains("skip") && !isAccept();
    }
}
