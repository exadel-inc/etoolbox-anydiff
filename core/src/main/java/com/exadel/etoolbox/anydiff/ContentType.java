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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Enumerates the content types supported by the AnyDiff library
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum ContentType {

    UNDEFINED {
        @Override
        public boolean matchesMime(String value) {
            return false;
        }

        @Override
        boolean matchesExtension(String value) {
            return false;
        }
    },

    XML {
        @Override
        public boolean matchesMime(String value) {
            return StringUtils.containsIgnoreCase(value, "xml");
        }

        @Override
        boolean matchesExtension(String value) {
            return StringUtils.equalsIgnoreCase(value, "xml");
        }
    },

    HTML {
        @Override
        public boolean matchesMime(String value) {
            return StringUtils.containsIgnoreCase(value, "html");
        }

        @Override
        boolean matchesExtension(String value) {
            return StringUtils.equalsAnyIgnoreCase(
                    value,
                    "htl",
                    "html",
                    "htm");
        }
    },

    TEXT {
        @Override
        public boolean matchesMime(String value) {
            return StringUtils.containsIgnoreCase(value, "text");
        }

        @Override
        boolean matchesExtension(String value) {
            return StringUtils.equalsAnyIgnoreCase(
                    value,
                    "css",
                    "csv",
                    "ecma",
                    "info",
                    "java",
                    "jsp",
                    "jspx",
                    "js",
                    "json",
                    "log",
                    "md",
                    "mf",
                    "php",
                    "properties",
                    "ts",
                    "txt");
        }
    };

    /**
     * Checks if the given MIME-type is matched by the current content type
     * @param value MIME-type to check
     * @return True or false
     */
    abstract boolean matchesMime(String value);

    /**
     * Checks if the given file extension is matched by the current content type
     * @param value File extension to check
     * @return True or false
     */
    abstract boolean matchesExtension(String value);

    /**
     * Gets the content type that matches the given MIME type
     * @param value MIME-type
     * @return {@code ContentType} enum value
     */
    public static ContentType fromMimeType(String value) {
        if (StringUtils.isBlank(value)) {
            return UNDEFINED;
        }
        String effectiveType = StringUtils.substringBefore(value, ";");
        for (ContentType contentType : values()) {
            if (contentType.matchesMime(effectiveType)) {
                return contentType;
            }
        }
        return UNDEFINED;
    }

    /**
     * Gets the content type that matches the given file extension
     * @param value File extension
     * @return {@code ContentType} enum value
     */
    public static ContentType fromExtension(String value) {
        if (StringUtils.isBlank(value)) {
            return UNDEFINED;
        }
        for (ContentType contentType : values()) {
            if (contentType.matchesExtension(value)) {
                return contentType;
            }
        }
        return UNDEFINED;
    }
}
