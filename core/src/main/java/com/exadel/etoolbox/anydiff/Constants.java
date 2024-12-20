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
package com.exadel.etoolbox.anydiff;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Defines constants used throughout the AnyDiff library
 * <u>Note</u>: This class is not a part of public API and is subject to change. You should not use it directly
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {

    public static final String ROOT_PACKAGE = "com.exadel.etoolbox.anydiff";

    public static final int DEFAULT_COLUMN_WIDTH = 60;
    public static final boolean DEFAULT_ARRANGE_ATTRIBUTES = true;
    public static final boolean DEFAULT_IGNORE_SPACES = true;
    public static final int DEFAULT_INDENT = 2;
    public static final boolean DEFAULT_NORMALIZE = true;

    public static final int MAX_CONTEXT_LENGTH = 8;

    public static final String AT = "@";
    public static final String COLON = ":";
    public static final char COLON_CHAR = COLON.charAt(0);
    public static final String COMMA = ",";
    public static final char COMMA_CHAR = COMMA.charAt(0);
    public static final String DASH = "-";
    public static final char DASH_CHAR = '-';
    public static final String DOT = ".";
    public static final String ELLIPSIS = "...";
    public static final String EQUALS = "=";
    public static final String HASH = "#";
    public static final String PIPE = "|";
    public static final String QUOTE = "\"";
    public static final char QUOTE_CHAR = QUOTE.charAt(0);
    public static final String SLASH = "/";
    public static final char SLASH_CHAR = SLASH.charAt(0);
    public static final char UNDERSCORE_CHAR = '_';

    public static final String ATTR_HREF = "href";
    public static final String ATTR_ID = "id";

    public static final String CLASS_HEADER = "header";
    public static final String CLASS_LEFT = "left";
    public static final String CLASS_PATH = "path";
    public static final String CLASS_RIGHT = "right";

    public static final String SCHEMA_SEPARATOR = "://";

    public static final char BRACKET_OPEN = '[';
    public static final char BRACKET_CLOSE = ']';

    public static final String TAG_AUTO_CLOSE = "/>";
    public static final String TAG_CLOSE = ">";
    public static final char TAG_CLOSE_CHAR = '>';
    public static final String TAG_OPEN = "<";
    public static final char TAG_OPEN_CHAR = '<';
    public static final String TAG_PRE_CLOSE = "</";

    public static final String TAG_ATTR_OPEN = EQUALS + QUOTE;
    public static final String TAG_ATTR_CLOSE = QUOTE;

    public static final String LABEL_LEFT = "Left";
    public static final String LABEL_RIGHT = "Right";
}
