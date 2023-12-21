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
import com.exadel.etoolbox.anydiff.comparison.TaskParameters;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Pre-processes the HTML content that needs to be compared
 */
@RequiredArgsConstructor
class HtmlPreprocessor extends Preprocessor {

    private final TaskParameters parameters;

    @Override
    public String apply(String value) {
        Document document = Jsoup.parse(value);
        StringBuilder builder = new StringBuilder();
        if (parameters.arrangeAttributes()) {
            document.traverse(new AttributePreprocessor(parameters));
        }
        document.traverse(new PrettyPrinter(builder, parameters));
        return builder.toString();
    }

    /**
     * Implements {@link NodeVisitor} to arrange attributes of the processed HTML content
     */
    @RequiredArgsConstructor
    private static class AttributePreprocessor implements NodeVisitor {

        private final TaskParameters parameters;

        @Override
        public void head(Node node, int depth) {
            Attributes arranged = arrangeAttributes(node.attributes());
            node.clearAttributes();
            node.attributes().addAll(arranged);
        }

        private Attributes arrangeAttributes(Attributes source) {
            Map<String, String> attributes = parameters.arrangeAttributes() ? new TreeMap<>() : new LinkedHashMap<>();
            for (Attribute attribute : source) {
                String key = attribute.getKey();
                String value = attribute.getValue();
                if (!StringUtils.equalsAny(key, "#text", "#data")) {
                    value = value.replaceAll("[\\n\\r]+", StringUtils.SPACE);
                }
                attributes.put(key, value);
            }
            Attributes result = new Attributes();
            attributes.forEach(result::put);
            return result;
        }
    }

    /**
     * Implements {@link NodeVisitor} to format the processed HTML content
     */
    @RequiredArgsConstructor
    private static class PrettyPrinter implements NodeVisitor {

        private static final List<String> VOID_ELEMENTS = Arrays.asList("area", "base", "br", "col", "embed",
                "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr");
        private static final List<String> SELF_CLOSING_ELEMENTS = Arrays.asList("circle", "ellipse", "line", "path",
                "polygon", "polyline", "rect");

        private final StringBuilder builder;
        private final TaskParameters parameters;

        private final Set<Node> consumedNodes = new HashSet<>();

        @Override
        public void head(Node node, int depth) {
            if (!isPrintable(node) || isConsumed(node)) {
                return;
            }
            String thisIndent = StringUtils.repeat(StringUtils.SPACE, (depth - 1) * Constants.DEFAULT_INDENT);
            String nextIndent = StringUtils.repeat(StringUtils.SPACE, depth * Constants.DEFAULT_INDENT);
            if (StringUtils.isNotEmpty(builder)) {
                builder.append(StringUtils.LF);
            }
            if (isOneLineTag(node)) {
                if (parameters.ignoreSpaces()) {
                    builder
                            .append(thisIndent)
                            .append(Constants.TAG_OPEN)
                            .append(node.nodeName())
                            .append(Constants.TAG_CLOSE)
                            .append(((Element) node).html().trim())
                            .append(Constants.TAG_PRE_CLOSE)
                            .append(node.nodeName())
                            .append(Constants.TAG_CLOSE);
                } else {
                    builder.append(thisIndent).append(node);
                }
                consumedNodes.add(node);
            } else if (isPrintableTag(node)) {
                builder.append(thisIndent).append(Constants.TAG_OPEN).append(node.nodeName());
                appendAttributes(node, thisIndent, nextIndent);
                builder.append(SELF_CLOSING_ELEMENTS.contains(node.nodeName()) ? Constants.TAG_AUTO_CLOSE :
                        Constants.TAG_CLOSE);
            } else {
                String nodeText = node instanceof TextNode && !parameters.ignoreSpaces()
                        ? ((TextNode) node).getWholeText().trim()
                        : node.toString();
                if (StringUtils.containsAny(nodeText, StringUtils.LF, StringUtils.CR)) {
                    appendTextBlock(nodeText, thisIndent);
                } else {
                    builder.append(thisIndent).append(nodeText.trim());
                }
            }
        }

        private void appendAttributes(Node node, String tagIndent, String attributeIndent) {
            boolean hasAttributes = !node.attributes().isEmpty();
            if (!hasAttributes) {
                return;
            }
            boolean hasMultipleAttributes = node.attributes().size() > 1;
            for (Attribute attribute : node.attributes()) {
                if (hasMultipleAttributes) {
                    builder.append(StringUtils.LF).append(attributeIndent);
                } else {
                    builder.append(StringUtils.SPACE);
                }
                builder
                        .append(attribute.getKey())
                        .append(Constants.TAG_ATTR_OPEN)
                        .append(parameters.ignoreSpaces() ? attribute.getValue().trim() : attribute.getValue())
                        .append(Constants.TAG_ATTR_CLOSE);
            }
            if (hasMultipleAttributes) {
                builder.append(StringUtils.LF).append(tagIndent);
            }
        }

        private void appendTextBlock(String text, String indent) {
            List<String> lines = Arrays.asList(StringUtils.split(text, StringUtils.LF + StringUtils.CR));
            int trimmableSpaces = lines
                    .stream()
                    .filter(StringUtils::isNotBlank)
                    .mapToInt(line -> line.chars().filter(chr -> !Character.isWhitespace(chr)).map(line::indexOf).findFirst().orElse(line.length()))
                    .min()
                    .orElse(0);
            boolean newLine = false;
            for (String line : lines) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                builder.append(newLine ? StringUtils.LF : StringUtils.EMPTY)
                        .append(indent)
                        .append(StringUtils.stripEnd(line, StringUtils.SPACE).substring(trimmableSpaces));
                newLine = true;
            }
        }

        @Override
        public void tail(Node node, int depth) {
            if (!isPrintableTag(node)
                    || VOID_ELEMENTS.contains(node.nodeName())
                    || SELF_CLOSING_ELEMENTS.contains(node.nodeName())
                    || isConsumed(node)) {
                consumedNodes.remove(node);
                return;
            }
            String indent = StringUtils.repeat(StringUtils.SPACE, (depth - 1) * Constants.DEFAULT_INDENT);
            builder
                    .append(StringUtils.LF)
                    .append(indent)
                    .append(Constants.TAG_PRE_CLOSE)
                    .append(node.nodeName())
                    .append(Constants.TAG_CLOSE);
        }

        private boolean isConsumed(Node node) {
            if (consumedNodes.isEmpty()) {
                return false;
            }
            for (Node current = node; current != null; current = current.parent()) {
                if (consumedNodes.contains(current)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isPrintable(Node node) {
            if ("#document".equals(node.nodeName())) {
                return false;
            }
            if (StringUtils.equalsAny(node.nodeName(), "#text", "#data")) {
                return StringUtils.isNotBlank(node.toString());
            }
            return true;
        }

        private static boolean isPrintableTag(Node node) {
            return node.nodeName().charAt(0) != '#';
        }

        private static boolean isOneLineTag(Node node) {
            if (!isPrintableTag(node)) {
                return false;
            }
            if (!node.childNodes().isEmpty() && node.childNodes().stream().anyMatch(PrettyPrinter::isPrintableTag)) {
                return false;
            }
            String content = node.toString();
            if (StringUtils.containsAny(content, StringUtils.LF, StringUtils.CR)) {
                return false;
            }
            return node.attributes().isEmpty();
        }
    }
}
