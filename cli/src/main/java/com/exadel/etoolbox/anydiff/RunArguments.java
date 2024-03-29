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
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsulates the command-line arguments passed to the AnyDiff CLI
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Slf4j
class RunArguments {

    private static final String ARGUMENT_ARRANGE = "arrange";
    private static final String ARGUMENT_FILTERS = "filters";
    private static final String ARGUMENT_HANDLE_ERRORS = "handle-errorpages";
    private static final String ARGUMENT_IGNORE_SPACES = "ignore-spaces";
    private static final String ARGUMENT_LEFT = "left";
    private static final String ARGUMENT_NORMALIZE = "normalize";
    private static final String ARGUMENT_RIGHT = "right";
    private static final String ARGUMENT_SAVE_HTML = "html";
    private static final String ARGUMENT_SHOW_IN_BROWSER = "browse";
    private static final String ARGUMENT_WIDTH = "width";

    /**
     * Gets the path to the filters that need to be applied to the comparison
     */
    private List<String> filters;

    /**
     * Gets whether to arrange tag attributes in markup files
     */
    @Accessors(fluent = true)
    private Boolean arrangeAttributes;

    /**
     * Gets the column width to use for the console and logfile output
     */
    private Integer columnWidth;

    /**
     * Gets whether to handle HTML error code pages as normal pages
     */
    @Accessors(fluent = true)
    private Boolean handleErrorPages;

    /**
     * Gets whether to ignore spaces when comparing content
     */
    @Accessors(fluent = true)
    private Boolean ignoreSpaces;

    /**
     * Gets the content to be used as the left side of the comparison
     */
    private String[] left;

    /**
     * Gets whether to reformat markup content for more accurate comparison
     */
    @Accessors(fluent = true)
    private Boolean normalizeMarkup;

    /**
     * Gets the content to be used as the right side of the comparison
     */
    private String[] right;

    /**
     * Gets whether to save the comparison results as an HTML file
     */
    @Accessors(fluent = true)
    private boolean saveHtml;

    /**
     * Gets whether to display the comparison results in a browser
     */
    @Accessors(fluent = true)
    private boolean showInBrowser;

    private final Options options;

    /**
     * Gets the command-line options
     * @return A non-null {@link Collection} of {@link Option} objects
     */
    Collection<Option> getOptions() {
        return options.getOptions();
    }

    /**
     * Creates a {@code RunArguments} instance from the given command-line arguments
     * @param args Command-line arguments
     * @return {@code RunArguments} instance
     */
    static RunArguments from(String[] args) {
        RunArguments result = new RunArguments(initOptions());
        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = commandLineParser.parse(result.options, args);
        } catch (ParseException e) {
            log.error(e.getMessage());
            return result;
        }

        result.arrangeAttributes = getBooleanOptionValue(commandLine, ARGUMENT_ARRANGE);
        result.columnWidth = getIntegerOptionValue(commandLine, ARGUMENT_WIDTH);
        result.filters = commandLine.getOptionValues(ARGUMENT_FILTERS) != null
                ? Arrays.stream(commandLine.getOptionValues(ARGUMENT_FILTERS)).filter(StringUtils::isNotBlank).collect(Collectors.toList())
                : Collections.emptyList();
        result.handleErrorPages = getBooleanOptionValue(commandLine, ARGUMENT_HANDLE_ERRORS);
        result.ignoreSpaces = getBooleanOptionValue(commandLine, ARGUMENT_IGNORE_SPACES);
        result.left = Arrays
                .stream(commandLine.getOptionValues(ARGUMENT_LEFT))
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
        result.normalizeMarkup = getBooleanOptionValue(commandLine, ARGUMENT_NORMALIZE);
        result.right = Arrays
                .stream(commandLine.getOptionValues(ARGUMENT_RIGHT))
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
        result.saveHtml = commandLine.hasOption(ARGUMENT_SAVE_HTML);
        result.showInBrowser = commandLine.hasOption(ARGUMENT_SHOW_IN_BROWSER);
        if (result.showInBrowser) {
            result.saveHtml = true;
        }
        return result;
    }

    private static Options initOptions() {
        Options options = new Options();

        options.addOption(
            "a",
            ARGUMENT_ARRANGE,
            true,
            "Arrange node attributes in markup content (default: " + Constants.DEFAULT_ARRANGE_ATTRIBUTES + ")");

        Option filters = new Option(
            "f",
            ARGUMENT_FILTERS,
            true,
            "File or folder containing filters. Multiple values are supported");
        filters.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(filters);

        options.addOption(
            "e",
            ARGUMENT_HANDLE_ERRORS,
            false,
            "Handle HTTP error code pages as normal pages with markup");

        options.addOption(
            "i",
            ARGUMENT_IGNORE_SPACES,
            false,
            "Ignore spaces when comparing text content");

        Option left = new Option(
            "l",
            ARGUMENT_LEFT,
            true,
            "Left part of comparison (a file name or a URL). Multiple values are supported");
        left.setArgs(Option.UNLIMITED_VALUES);
        left.setRequired(true);
        options.addOption(left);

        options.addOption(
            "n",
            ARGUMENT_NORMALIZE,
            true,
            "Normalize markup content before comparison (default: " + Constants.DEFAULT_NORMALIZE + ")");

        Option right = new Option(
            "r",
            ARGUMENT_RIGHT,
            true,
            "Right part of comparison (a file name or a URL). Multiple values are supported");
        right.setArgs(Option.UNLIMITED_VALUES);
        right.setRequired(true);
        options.addOption(right);

        options.addOption(
            "h",
            ARGUMENT_SAVE_HTML,
            false,
            "Save comparison results as HTML");

        options.addOption(
            "b",
            ARGUMENT_SHOW_IN_BROWSER,
            false,
            "Display comparison results in the browser. If you set this flag, you enable \"Save HTML\" as well");

        options.addOption(
            "w",
            ARGUMENT_WIDTH,
            true,
            "Output: Column width in console/log (default: " + Constants.DEFAULT_COLUMN_WIDTH + " chars)");

        return options;
    }

    private static Boolean getBooleanOptionValue(CommandLine commandLine, String name) {
        String rawValue = commandLine.getOptionValue(name);
        if (rawValue == null) {
            return commandLine.hasOption(name) ? true : null;
        }
        return Boolean.parseBoolean(rawValue);
    }

    @SuppressWarnings("SameParameterValue")
    private static Integer getIntegerOptionValue(CommandLine commandLine, String name) {
        String rawValue = commandLine.getOptionValue(name);
        if (!StringUtils.isNumeric(rawValue)) {
            return null;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
