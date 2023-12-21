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

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Decorates a {@link Scriptable} object to provide a stub implementation of methods that are not defined in the
 * underlying object to provide exception-free run of user-defined filters
 */
class ScriptableWrapper implements Scriptable {

    private static final Callable METHOD_STUB = (context, scope, thisObj, args) -> null;

    private Scriptable prototype;

    /**
     * Creates a new {@code ScriptableWrapper} instance
     * @param value A Java object that must be made callable from the user script
     * @param scope A {@link Scriptable} object that represents the scope of the user script
     */
    ScriptableWrapper(Object value, Scriptable scope) {
        this.prototype = (Scriptable) Context.javaToJS(value, scope);
    }

    @Override
    public String getClassName() {
        return prototype.getClassName();
    }

    @Override
    public Object get(String name, Scriptable start) {
        if (!prototype.has(name, start)) {
            return METHOD_STUB;
        }
        return prototype.get(name, start);
    }

    @Override
    public Object get(int index, Scriptable start) {
        if (!prototype.has(index, start)) {
            return METHOD_STUB;
        }
        return prototype.get(index, start);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return prototype.has(name, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return prototype.has(index, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        prototype.put(name, start, value);
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        prototype.put(index, start, value);
    }

    @Override
    public void delete(String name) {
        prototype.delete(name);
    }

    @Override
    public void delete(int index) {
        prototype.delete(index);
    }

    @Override
    public Scriptable getPrototype() {
        return prototype;
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        this.prototype = prototype;
    }

    @Override
    public Scriptable getParentScope() {
        return prototype.getParentScope();
    }

    @Override
    public void setParentScope(Scriptable parent) {
        prototype.setParentScope(parent);
    }

    @Override
    public Object[] getIds() {
        return prototype.getIds();
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return prototype.getDefaultValue(hint);
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        return prototype.hasInstance(instance);
    }
}
