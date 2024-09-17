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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pre-processes the XML content that needs to be compared
 */
@RequiredArgsConstructor
@Slf4j
class XmlPreprocessor extends Preprocessor {

    private static final Map<String, Boolean> SECURITY_FEATURES;
    static {
        Map<String, Boolean> securityFeatures = new HashMap<>();
        securityFeatures.put(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        securityFeatures.put("http://xml.org/sax/features/external-general-entities", false);
        securityFeatures.put("http://xml.org/sax/features/external-parameter-entities", false);
        securityFeatures.put("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        SECURITY_FEATURES = securityFeatures;
    }

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;
    static {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, StringUtils.EMPTY);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, StringUtils.EMPTY);
        for (Map.Entry<String, Boolean> feature : SECURITY_FEATURES.entrySet()) {
            try {
                factory.setFeature(feature.getKey(), feature.getValue());
            } catch (ParserConfigurationException e) {
                log.error("Error setting security feature {}", feature.getKey(), e);
            }
        }
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DOCUMENT_BUILDER_FACTORY = factory;
    }

    private final TaskParameters parameters;

    @Override
    public String apply(String value) {
        Document document = getDocument(value);
        if (document == null) {
            return value;
        }
        StringBuilder builder = new StringBuilder();
        append(builder, document.getDocumentElement(), 0);
        return builder.toString();
    }

    private void append(StringBuilder builder, Node node, int level) {

        String thisIndent = StringUtils.repeat(StringUtils.SPACE, level * Constants.DEFAULT_INDENT);
        String nextIndent = StringUtils.repeat(StringUtils.SPACE, (level + 1) * Constants.DEFAULT_INDENT);

        boolean hasAttributes = node.getAttributes() != null && node.getAttributes().getLength() > 0;
        boolean hasMultipleAttributes = hasAttributes && node.getAttributes().getLength() > 1;

        builder.append(thisIndent).append(Constants.TAG_OPEN).append(node.getNodeName());

        if (hasAttributes) {
            Map<String, String> arrangedAttributes = arrangeAttributes(node.getAttributes());
            if (hasMultipleAttributes) {
                arrangedAttributes.forEach((key, value) ->  builder
                        .append(StringUtils.LF)
                        .append(nextIndent)
                        .append(key)
                        .append(Constants.TAG_ATTR_OPEN)
                        .append(parameters.ignoreSpaces() ? StringUtils.trim(value) : value)
                        .append(Constants.TAG_ATTR_CLOSE));
            } else {
                String key = arrangedAttributes.keySet().iterator().next();
                String value = arrangedAttributes.values().iterator().next();
                builder
                        .append(StringUtils.SPACE)
                        .append(key)
                        .append(Constants.TAG_ATTR_OPEN)
                        .append(parameters.ignoreSpaces() ? StringUtils.trim(value) : value)
                        .append(Constants.TAG_ATTR_CLOSE);
            }
        }
        if (hasMultipleAttributes) {
            builder.append(StringUtils.LF).append(thisIndent);
        }
        if (!hasChildren(node)) {
            builder.append(Constants.TAG_AUTO_CLOSE);
            return;
        }
        builder.append(Constants.TAG_CLOSE);
        NodeList children = node.getChildNodes();
        if (children.getLength() == 1
                && children.item(0).getNodeType() == Node.TEXT_NODE
                && !StringUtils.containsAny(children.item(0).getTextContent(), StringUtils.CR, StringUtils.LF)) {
            String text = children.item(0).getNodeValue();
            builder.append(parameters.ignoreSpaces() ? text.trim() : text);
        } else {
            for (int i = 0, length = children.getLength(); i < length; i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.TEXT_NODE && StringUtils.isBlank(child.getTextContent())) {
                    continue;
                }
                builder.append(StringUtils.LF);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    append(builder, child.getTextContent(), level + 1);
                } else {
                    append(builder, child, level + 1);
                }
            }
            builder.append(StringUtils.LF).append(thisIndent);
        }
        builder.append(Constants.TAG_PRE_CLOSE).append(node.getNodeName()).append(Constants.TAG_CLOSE);
    }

    private void append(StringBuilder builder, String text, int level) {
        String thisIndent = StringUtils.repeat(StringUtils.SPACE, level * Constants.DEFAULT_INDENT);
        builder.append(thisIndent).append(text);
    }

    private boolean hasChildren(Node node) {
        NodeList childNodes = node.getChildNodes();
        if (childNodes.getLength() == 0) {
            return false;
        } else if (childNodes.getLength() > 1) {
            return true;
        }
        Node firstChild = childNodes.item(0);
        return firstChild.getNodeType() != Node.TEXT_NODE
                || StringUtils.isNotBlank(firstChild.getTextContent());
    }

    private Map<String, String> arrangeAttributes(NamedNodeMap source) {
        Map<String, String> result = parameters.arrangeAttributes()
                ? new TreeMap<>(new AttributeSorter())
                : new LinkedHashMap<>();
        for (int i = 0, length = source.getLength(); i < length; i++) {
            Node attribute = source.item(i);
            result.put(attribute.getNodeName(), attribute.getNodeValue());
        }
        return result;
    }

    private Document getDocument(String source) {
        try {
            DocumentBuilder builder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(source));
            return builder.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Error parsing XML for {}", getContentId(), e);
        }
        return null;
    }

    /**
     * Sorts XML attributes in the consistent reproducible order
     */
    private static class AttributeSorter implements Comparator<String> {
        private static final List<String> PRIVILEGED_NAMES = Arrays.asList(
                "xmlns:",
                "jcr:primaryType",
                "sling:resourceType",
                "sling:resourceSuperType",
                "jcr:title",
                "jcr:description");

        @Override
        public int compare(String first, String second) {
            int privilegedRankingFirst = getPrivilegedRanking(first);
            int privilegedRankingSecond = getPrivilegedRanking(second);
            if (privilegedRankingFirst >= 0 && privilegedRankingSecond >= 0) {
                int internalComparison = Integer.compare(privilegedRankingFirst, privilegedRankingSecond);
                return internalComparison != 0 ? internalComparison : first.compareTo(second);
            } else if (privilegedRankingFirst >= 0) {
                return -1;
            } else if (privilegedRankingSecond >= 0) {
                return 1;
            }
            if (first.contains(Constants.COLON) && !second.contains(Constants.COLON)) {
                return -1;
            } else if (second.contains(Constants.COLON) && !first.contains(Constants.COLON)) {
                return 1;
            }
            return first.compareTo(second);
        }

        private static int getPrivilegedRanking(String value) {
            return PRIVILEGED_NAMES
                    .stream()
                    .filter(value::startsWith)
                    .map(PRIVILEGED_NAMES::indexOf)
                    .findFirst()
                    .orElse(-1);
        }
    }
}
