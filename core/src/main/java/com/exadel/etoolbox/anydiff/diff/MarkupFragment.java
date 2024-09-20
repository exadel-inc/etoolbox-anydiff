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
 * Represents a fragment of string content structured as XML, HTML, or JSON markup
 */
public interface MarkupFragment extends Fragment {

    /**
     * Gets whether the current fragment is situated inside an XML/HTML attribute with the given name
     * @param name Name of the attribute. A non-empty string value is expected
     * @return True or false
     */
    boolean isAttributeValue(String name);

    /**
     * Gets whether the current fragment is situated inside an XML/HTML tag with the given name (that is, in between the
     * {@code <tag...} opener and the closing or self-closing ({@code >} or {@code />}) bracket)
     * @param name Name of the tag. A non-empty string value is expected
     * @return True or false
     */
    boolean isInsideTag(String name);

    /**
     * Gets whether the current fragment is situated inside the content (that is, anywhere among the children) of an
     * XML/HTML tag with the given name
     * @param name Name of the tag. A non-empty string value is expected
     * @return True or false
     */
    boolean isTagContent(String name);

    /**
     * Gets whether the current fragment is situated inside a JSON property with the given name. Note: this method
     * only considers the "immediate" name of the property regardless how deep it is in the JSON structure
     * @param name Name of the property. A non-empty string value is expected
     * @return True or false
     */
    boolean isJsonValue(String name);
}
