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
package com.exadel.etoolbox.anydiff.comparison;

import com.exadel.etoolbox.anydiff.Constants;
import com.exadel.etoolbox.anydiff.OutputType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a marker used to highlight the differences between two text strings
 * <u>Note</u>: This class is not a part of public API and is subject to change. You should not use it directly
 */
@RequiredArgsConstructor
public enum Marker {

    /* ------
       Values
       ------ */

    CONTEXT {
        @Override
        public String toString() {
            return "{{eq}}";
        }

        @Override
        public String toConsole() {
            return "\u001B[37m";
        }

        @Override
        public String toLogFile(Marker previous) {
            return Constants.EQUALS;
        }

        @Override
        String toHtml(Marker previous) {
            return formatHtmlClass("eq");
        }
    },

    DELETE {
        @Override
        public String toString() {
            return "{{del}}";
        }

        @Override
        public String toConsole() {
            return "\u001B[30;48;5;207m";
        }

        @Override
        public String toLogFile(Marker previous) {
            return "~";
        }

        @Override
        String toHtml(Marker previous) {
            return formatHtmlClass("del");
        }
    },

    INSERT {
        @Override
        public String toString() {
            return "{{ins}}";
        }

        @Override
        public String toConsole() {
            return "\u001B[30;48;5;119m";
        }

        @Override
        public String toLogFile(Marker previous) {
            return "+";
        }

        @Override
        String toHtml(Marker previous) {
            return formatHtmlClass("ins");
        }
    },

    PLACEHOLDER {
        @Override
        public String toString() {
            return "{{_}}";
        }

        @Override
        public String toConsole() {
            return StringUtils.EMPTY;
        }

        @Override
        String toLogFile(Marker previous) {
            return StringUtils.EMPTY;
        }

        @Override
        String toHtml(Marker previous) {
            return StringUtils.EMPTY;
        }
    },

    RESET {
        @Override
        public String toString() {
            return "{{/}}";
        }

        @Override
        public String toConsole() {
            return "\u001B[0m";
        }

        @Override
        public String toLogFile(Marker previous) {
            return previous == INSERT || previous == DELETE ? previous.toLogFile(null) : StringUtils.EMPTY;
        }

        @Override
        String toHtml(Marker previous) {
            return previous != null ? "</span>" : StringUtils.EMPTY;
        }
    };

    /* -----------------
       Utility constants
       ----------------- */

    static final String ELLIPSIS = "{{.}}";

    public static final String NEW_LINE = "{{n}}";

    static final String[] TOKENS = new String[]{
            CONTEXT.toString(),
            DELETE.toString(),
            INSERT.toString(),
            PLACEHOLDER.toString(),
            RESET.toString()
    };

    /* -------
       Methods
       ------- */

    /**
     * Creates a marked string out of the provided value
     * @param value An arbitrary object. A non-null value is expected
     * @return A string with the in-line marker applied
     */
    public String wrap(Object value) {
        return this + (value != null ? String.valueOf(value) : StringUtils.EMPTY) + Marker.RESET;
    }

    /**
     * Outputs the marker according to the specified target (a console, a log file, an HTML page)
     * @param target   An {@link OutputType} value
     * @param previous A {@code Marker} value that precedes the current one (to distinguish between the "opening" and
     *                 "closing" part of a marker when needed)
     * @return A string value
     */
    String to(OutputType target, Marker previous) {
        if (target == OutputType.CONSOLE) {
            return toConsole();
        } else if (target == OutputType.LOG) {
            return toLogFile(previous);
        }
        return toHtml(previous);
    }

    /**
     * Outputs the marker to the console
     * @return A string value
     */
    abstract String toConsole();

    /**
     * Outputs the marker to a log file
     * @param previous A {@code Marker} value that precedes the current one (to distinguish between the "opening" and
     *                 "closing" part of a marker when needed)
     * @return A string value
     */
    abstract String toLogFile(Marker previous);

    /**
     * Outputs the marker to HTML markup
     * @param previous A {@code Marker} value that precedes the current one (to distinguish between the "opening" and
     *                 "closing" part of a marker when needed)
     * @return A string value
     */
    abstract String toHtml(Marker previous);

    private static String formatHtmlClass(String className) {
        return "<span class=\"" + className + "\">";
    }

    /* -------------
       Factory logic
       ------------- */

    /**
     * Retrieves a {@code Marker} instance matching the specified string value
     * @param value A string value
     * @return A {@code Marker} instance or null if the value does not match any of the known markers
     */
    static Marker fromString(String value) {
        for (Marker marker : Marker.values()) {
            if (StringUtils.equals(value, marker.toString())) {
                return marker;
            }
        }
        return null;
    }
}
