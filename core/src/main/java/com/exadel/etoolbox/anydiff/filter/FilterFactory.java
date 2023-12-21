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

import com.exadel.etoolbox.anydiff.Constants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionNode;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates and manages {@link Filter} instances built from the provided user scripts in JavaScript syntax
 */
@Slf4j
public class FilterFactory implements Closeable {

    private static final ContextFactory CONTEXT_FACTORY = new ContextFactory();

    private static final ClassShutter CLASS_SHUTTER = className ->
            StringUtils.startsWith(className, Constants.ROOT_PACKAGE);

    /**
     * Gets the list of {@link Filter} instances created from the provided user scripts
     */
    @Getter
    private final List<Filter> filters;

    private Context context;
    private Scriptable scope;

    /**
     * Creates a new {@code FilterFactory} instance
     */
    public FilterFactory() {
        filters = new ArrayList<>();
        try {
            this.context = CONTEXT_FACTORY.enterContext();
            this.context.setLanguageVersion(Context.VERSION_ES6);
            this.context.setClassShutter(CLASS_SHUTTER);
            this.context.getWrapFactory().setJavaPrimitiveWrap(false);
            initScope();
        } catch (IllegalStateException e) {
            log.error("Error while initializing scripting context", e);
        }
    }

    private void initScope() {
        scope = this.context.initStandardObjects();
        ScriptableObject.putProperty(scope, "console", Context.javaToJS(new VirtualConsole(), scope));
    }

    /**
     * Creates and stores a new {@link Filter} instance from the provided user script
     * @param value String value representing the user script
     */
    public void useScript(String value) {
        List<FunctionDefinition> functions = extractFunctions(value);
        for (FunctionDefinition function : functions) {
            if (!function.isSkip() && !function.isAccept()) {
                continue;
            }
            filters.add(new ScriptedFilter(context, scope, function));
        }
    }

    @Override
    public void close() {
        if (context != null) {
            context.close();
        }
    }

    private static List<FunctionDefinition> extractFunctions(String value) {
        AstRoot scriptAst;
        try {
            scriptAst = new Parser().parse(value, StringUtils.EMPTY, 0);
        } catch (EvaluatorException e) {
            log.error("Error while parsing script", e);
            return Collections.emptyList();
        }
        List<FunctionDefinition> result = new ArrayList<>();
        for (Node child = scriptAst.getFirstChild(); child != null; child = child.getNext()) {
            if (!(child instanceof FunctionNode)) {
                continue;
            }
            FunctionNode functionNode = (FunctionNode) child;
            List<AstNode> params = functionNode.getParams();
            if (CollectionUtils.isEmpty(params)) {
                continue;
            }
            String name = functionNode.getFunctionName().getIdentifier();
            String parameter = params.get(0).toSource();
            String body = functionNode.toSource();
            result.add(new FunctionDefinition(name, parameter, body));
        }
        return result;
    }

    /**
     * Proxies the console output for the user code in JavaScript syntax
     */
    public static class VirtualConsole {

        /**
         * Logs the provided string values
         * @param value One or more string values to log as a single line
         */
        public void log(String... value) {
            if (ArrayUtils.isEmpty(value)) {
                return;
            }
            log.info(String.join(StringUtils.SPACE, value));
        }
    }
}
