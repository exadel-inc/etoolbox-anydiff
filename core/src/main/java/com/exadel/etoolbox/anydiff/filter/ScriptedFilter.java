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

import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffEntry;
import com.exadel.etoolbox.anydiff.diff.DiffEntryType;
import com.exadel.etoolbox.anydiff.diff.Fragment;
import com.exadel.etoolbox.anydiff.diff.FragmentPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Implements {@link Filter} by parsing and executing a user-provided JavaScript function
 */
@RequiredArgsConstructor
@Slf4j
class ScriptedFilter implements Filter {

    private static final String PROPERTY_VALUE = "value";
    private static final String SELF_EXECUTING_FUNCTION = "(%s)(value);";

    private final Context context;
    private final Scriptable scope;
    private final FunctionDefinition function;

    @Override
    public boolean acceptDiff(Diff value) {
        return StringUtils.equalsIgnoreCase(function.getParameter(), "diff")
                && function.isAccept()
                && execute(value);
    }

    @Override
    public boolean skipDiff(Diff value) {
        return StringUtils.equalsIgnoreCase(function.getParameter(), "diff")
                && function.isSkip()
                && execute(value);
    }

    @Override
    public boolean acceptBlock(DiffEntry value) {
        return DiffEntryType.from(function.getParameter()) == DiffEntryType.BLOCK
                && function.isAccept()
                && execute(value);
    }

    @Override
    public boolean skipBlock(DiffEntry value) {
        return DiffEntryType.from(function.getParameter()) == DiffEntryType.BLOCK
                && function.isSkip()
                && execute(value);
    }

    @Override
    public boolean acceptLine(DiffEntry value) {
        return DiffEntryType.from(function.getParameter()) == DiffEntryType.LINE
                && function.isAccept()
                && execute(value);
    }

    @Override
    public boolean skipLine(DiffEntry value) {
        return DiffEntryType.from(function.getParameter()) == DiffEntryType.LINE
                && function.isSkip()
                && execute(value);
    }

    @Override
    public boolean acceptFragments(FragmentPair value) {
        return DiffEntryType.from(function.getParameter()) == DiffEntryType.FRAGMENT_PAIR
                && function.isAccept()
                && execute(value);
    }

    @Override
    public boolean skipFragments(FragmentPair value) {
        return DiffEntryType.from(function.getParameter()) == DiffEntryType.FRAGMENT_PAIR
                && function.isSkip()
                && execute(value);
    }

    @Override
    public boolean acceptFragment(Fragment value) {
        return StringUtils.equalsIgnoreCase(function.getParameter(), "fragment")
                && function.isAccept()
                && execute(value);
    }

    @Override
    public boolean skipFragment(Fragment value) {
        return StringUtils.equalsIgnoreCase(function.getParameter(), "fragment")
                && function.isSkip()
                && execute(value);
    }

    private boolean execute(Object value) {
        Scriptable scriptable = new ScriptableWrapper(value, scope);
        ScriptableObject.putProperty(scope, PROPERTY_VALUE, scriptable);
        try {
            Object result = context.evaluateString(
                    scope,
                    String.format(SELF_EXECUTING_FUNCTION, function.getBody()),
                    StringUtils.EMPTY,
                    0,
                    null);
            return Context.toBoolean(result);
        } catch (EvaluatorException | EcmaError e) {
            log.error("Error while executing script", e);
        } finally {
            ScriptableObject.deleteProperty(scope, PROPERTY_VALUE);
        }
        return false;
    }

}
