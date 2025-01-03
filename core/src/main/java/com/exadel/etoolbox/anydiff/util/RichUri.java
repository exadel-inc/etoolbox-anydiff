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
package com.exadel.etoolbox.anydiff.util;

import com.exadel.etoolbox.anydiff.Constants;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A utility class that represents a URI with additional parsing capabilities
 * <u>Note</u>: This class is not a part of public API and is subject to change. You should not use it directly
 */
@Getter
public class RichUri {

    private static final String AMPERSAND = "&";
    private static final String QUESTION_SIGN = "?";

    private static final String EMPTY = "<empty>";

    private URI uri;
    private String userInfo;
    private Map<String, String> options;

    /**
     * Creates a {@code RichUri} instance based on the specified URI value
     * @param value URI value as a string. A non-blank string is expected
     * @throws URISyntaxException If the specified value is not a valid URI
     * @throws IOException        If an I/O error occurs while parsing the URI value
     */
    public RichUri(String value) throws URISyntaxException, IOException {
        parse(value);
    }

    @Override
    public String toString() {
        return uri != null ? uri.toString() : EMPTY;
    }

    private void parse(String value) throws URISyntaxException, IOException {
        if (StringUtils.isEmpty(value)) {
            throw new URISyntaxException(value, "URI value cannot be null or empty");
        }
        StreamTokenizer streamTokenizer = new StreamTokenizer(new StringReader(value.trim().replace('\\', '/')));
        streamTokenizer.wordChars('-', '-');
        streamTokenizer.wordChars(' ', ' ');
        streamTokenizer.quoteChar('\'');
        streamTokenizer.quoteChar('`');
        streamTokenizer.ordinaryChar('.');
        streamTokenizer.ordinaryChar('/');
        for (char c = '0'; c <= '9'; c++) {
            streamTokenizer.ordinaryChar(c);
        }
        streamTokenizer.wordChars('0', '9');
        List<String> buffer = new ArrayList<>();
        int token = streamTokenizer.nextToken();
        while (token != StreamTokenizer.TT_EOF) {
            if (streamTokenizer.sval != null) {
                buffer.add(streamTokenizer.sval);
            } else {
                buffer.add(String.valueOf((char) token));
            }
            token = streamTokenizer.nextToken();
        }
        parseTokens(buffer.stream().filter(StringUtils::isNotEmpty).collect(Collectors.toList()));
    }

    private void parseTokens(List<String> buffer) throws URISyntaxException {
        String schema = null;
        String fragment = null;
        String query = null;

        if (buffer.size() > 1 && buffer.get(1).equals(Constants.COLON)) {
            schema = buffer.get(0);
            StringBuilder schemaDelimiter = new StringBuilder(Constants.COLON);
            for (int i = 2; i < buffer.size(); i++) {
                if (!Constants.COLON.equals(buffer.get(i)) && !Constants.SLASH.equals(buffer.get(i))) {
                    break;
                } else {
                    schemaDelimiter.append(buffer.get(i));
                }
            }
            schema += schemaDelimiter;
            buffer = buffer.subList(1 + schemaDelimiter.length(), buffer.size());
        }

        int indexOfFragment = buffer.indexOf(Constants.HASH);
        if (indexOfFragment > -1) {
            if (indexOfFragment < buffer.size() - 1) {
                fragment = Constants.HASH + buffer.get(indexOfFragment + 1);
            }
            buffer = buffer.subList(0, buffer.indexOf(Constants.HASH));
        }

        int indexOfQuery = buffer.indexOf(QUESTION_SIGN);
        if (indexOfQuery > -1) {
            if (indexOfQuery < buffer.size() - 1) {
                Map<String, String> parsedQuery = parseQuery(buffer.subList(indexOfQuery + 1, buffer.size()));
                this.options = parsedQuery.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().startsWith(Constants.AT))
                    .collect(Collectors.toMap(
                        entry -> entry.getKey().substring(1),
                        entry -> StringUtils.defaultString(entry.getValue())));
                query = parsedQuery.entrySet()
                    .stream()
                    .filter(entry -> !entry.getKey().startsWith(Constants.AT))
                    .map(entry -> entry.getKey() + Constants.EQUALS + entry.getValue())
                    .collect(Collectors.joining(AMPERSAND));
                if (StringUtils.isNotEmpty(query)) {
                    query = QUESTION_SIGN + query;
                }
            }
            buffer = buffer.subList(0, indexOfQuery);
        }

        int indexOfUserInfoDelimiter = buffer.indexOf(Constants.AT);
        if (indexOfUserInfoDelimiter > -1) {
            if (indexOfUserInfoDelimiter < buffer.size() - 1) {
                userInfo = String.join(StringUtils.EMPTY, buffer.subList(0, indexOfUserInfoDelimiter));
            }
            buffer = buffer.subList(indexOfUserInfoDelimiter + 1, buffer.size());
        }

        String mainPart = String.join(StringUtils.EMPTY, buffer);
        if (mainPart.contains(StringUtils.SPACE)) {
            mainPart = new URI(null, null, mainPart, null).toString();
        }
        this.uri = new URI(
            StringUtils.defaultString(schema)
                + mainPart
                + StringUtils.defaultString(query)
                + StringUtils.defaultString(fragment));
    }

    private static Map<String, String> parseQuery(List<String> buffer) {
        Map<String, String> result = new LinkedHashMap<>();
        boolean nextIsOption = false;
        boolean nextIsValue = false;
        String bufferedKey = null;
        for (String token : buffer) {
            if (Constants.AT.equals(token)) {
                nextIsOption = true;
            } else if (Constants.EQUALS.equals(token)) {
                nextIsValue = true;
            } else if (nextIsValue) {
                if (bufferedKey != null) {
                    result.put(bufferedKey, token);
                }
                bufferedKey = null;
                nextIsValue = false;
            } else if (AMPERSAND.equals(token)) {
                if (bufferedKey != null) {
                    result.put(bufferedKey, null);
                    bufferedKey = null;
                }
            } else if (StringUtils.isNotBlank(token)) {
                bufferedKey = (nextIsOption ? Constants.AT : StringUtils.EMPTY) + token;
                nextIsOption = false;
            }
        }
        if (bufferedKey != null) {
            result.put(bufferedKey, null);
        }
        return result;
    }
}
