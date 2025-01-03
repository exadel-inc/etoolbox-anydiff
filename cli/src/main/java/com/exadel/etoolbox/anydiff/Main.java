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

import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffState;
import com.exadel.etoolbox.anydiff.filter.FilterFactory;
import com.exadel.etoolbox.anydiff.util.ContentUtil;
import com.exadel.etoolbox.anydiff.util.HtmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MarkerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Implements the entry point for the AnyDiff CLI application
 */
@Slf4j
public class Main {

    private static final org.slf4j.Marker CONSOLE_ONLY = MarkerFactory.getMarker("CONSOLE_ONLY");
    private static final org.slf4j.Marker LOGFILE_ONLY = MarkerFactory.getMarker("FILE_ONLY");

    private static final String REPLACED_FILENAME_CHARS = "[.,:/?\"<>|*\\\\]+";

    private static final String EXTENSION_HTML = ".html";
    private static final String EXTENSION_JS = "js";

    /* ----
       Main
       ---- */

    /**
     * Parses the command line arguments, runs the comparison, and outputs the results
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        RunArguments runArguments = RunArguments.from(args);
        if (ArrayUtils.isEmpty(runArguments.getLeft()) || ArrayUtils.isEmpty(runArguments.getRight())) {
            log.info("Use the following arguments to run the program:");
            runArguments.getOptions().forEach(option ->
                    log.info("-{} (--{}) {}", option.getOpt(), option.getLongOpt(), option.getDescription()));
            return;
        }
        System.out.print("EToolbox AnyDiff ");
        AnyDiff anyDiff = new AnyDiff()
                .left(runArguments.getLeft())
                .right(runArguments.getRight());
        if (runArguments.arrangeAttributes() != null) {
            anyDiff.arrangeAttributes(runArguments.arrangeAttributes());
        }
        if (runArguments.normalizeMarkup() != null) {
            anyDiff.normalize(runArguments.normalizeMarkup());
        }
        if (runArguments.handleErrorPages() != null) {
            anyDiff.handleErrorPages(runArguments.handleErrorPages());
        }
        if (runArguments.ignoreSpaces() != null) {
            anyDiff.ignoreSpaces(runArguments.ignoreSpaces());
        }
        if (runArguments.getColumnWidth() != null) {
            anyDiff.columnWidth(runArguments.getColumnWidth());
        }
        List<Diff> differences = getDifferences(anyDiff, runArguments.getFilters());

        int allCount = differences.stream().mapToInt(Diff::getCount).sum();
        int pendingCount = differences.stream().mapToInt(Diff::getPendingCount).sum();
        printHead(allCount, pendingCount);

        boolean isFirst = true;
        for (Diff difference : differences) {
            print(difference, runArguments, differences.size() > 1, isFirst);
            isFirst = false;
        }

        if (!runArguments.saveHtml() || allCount == 0) {
            return;
        }

        String reportPath = saveHtml(runArguments, differences);
        if (runArguments.showInBrowser() && StringUtils.isNotBlank(reportPath)) {
            Browser.launch(reportPath);
        }
        System.exit(pendingCount > 0 ? 1 : 0);
    }

    /* -------
       Filters
       ------- */

    private static List<Diff> getDifferences(AnyDiff comparator, List<String> filters) {
        List<String> filterFiles = getFilterFiles(filters);
        if (CollectionUtils.isEmpty(filterFiles)) {
            return comparator.compare();
        }
        try (FilterFactory factory = new FilterFactory()) {
            for (String filterFile : filterFiles) {
                String filter = readFilter(new File(filterFile));
                if (StringUtils.isBlank(filter)) {
                    continue;
                }
                factory.useScript(filter);
            }
            return comparator.filter(factory.getFilters()).compare();
        }
    }

    private static List<String> getFilterFiles(List<String> sources) {
        if (CollectionUtils.isEmpty(sources)) {
            return Collections.emptyList();
        }
        List<String> files = new ArrayList<>();
        Path current = Paths.get(StringUtils.EMPTY);
        for (String source : sources) {
            File file = current.resolve(source).toFile();
            if (!file.exists()) {
                log.warn("{} does not exist", file.getAbsolutePath());
                continue;
            }
            if (file.isDirectory()) {
                files.addAll(getFilterFiles(file));
            } else {
                if (StringUtils.endsWith(file.getName(), Constants.DOT + EXTENSION_JS)) {
                    files.add(file.getAbsolutePath());
                } else {
                    log.warn("{} is not a JavaScript file", file.getAbsolutePath());
                }
            }
        }
        return files;
    }

    private static List<String> getFilterFiles(File directory) {
        Collection<?> files = FileUtils.listFiles(directory, new String[]{EXTENSION_JS}, true);
        List<String> paths = new ArrayList<>();
        for (Object filesEntry : files) {
            File file = (File) filesEntry;
            if (!file.isFile() || !file.exists()) {
                continue;
            }
            paths.add(file.getAbsolutePath());
        }
        return paths;
    }

    private static String readFilter(File file) {
        try(InputStream inputStream = file.toURI().toURL().openStream()) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading rule from {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    /* ----------
       Log output
       ---------- */

    private static void printHead(int allCount, long pendingCount) {
        if (pendingCount == 0 && allCount == 0) {
            log.info("No differences");
        } else if (pendingCount == 0) {
            log.info("\nNo new differences found");
            String secondLine = String.format("There are %d accepted difference(-s)", allCount);
            log.info(secondLine);
            log.info(StringUtils.repeat(Constants.EQUALS, secondLine.length()));
        } else if (pendingCount == allCount){
            String line = String.format("\nFound %d difference(-s)", allCount);
            log.info(line);
            log.info(StringUtils.repeat(Constants.EQUALS, line.length() - 1));
        } else {
            log.info(String.format("\nFound %d new difference(-s)", pendingCount));
            String secondLine = String.format("There are also %d accepted difference(-s)", allCount - pendingCount);
            log.info(secondLine);
            log.info(StringUtils.repeat(Constants.EQUALS, secondLine.length()));
        }
    }

    private static void print(Diff diff, RunArguments runArguments, boolean isOneOfMany, boolean isFirst) {
        int columnWidth = runArguments.getColumnWidth() != null
                ? runArguments.getColumnWidth()
                : Constants.DEFAULT_COLUMN_WIDTH;

        String leftIdSplit = splitByLength(diff.getLeft() + " (Left)", "Compared ", columnWidth * 2 + 1);
        String rightIdSplit = splitByLength(diff.getRight() + " (Right)", "      to ", columnWidth * 2 + 1);
        log.info("\n{}{}", isFirst ?  StringUtils.EMPTY : StringUtils.LF, leftIdSplit);
        log.info("{}\n", rightIdSplit);

        if (diff.getState() == DiffState.UNCHANGED) {
            if (isOneOfMany) {
                log.info("No differences");
            }
        } else {
            if (isOneOfMany) {
                log.info("{} difference(-s)", diff.getCount());
            }
            log.info(CONSOLE_ONLY, diff.toString(OutputType.CONSOLE));
            log.info(LOGFILE_ONLY, diff.toString(OutputType.LOG));
        }
    }

    private static String splitByLength(String value, String prefix, int limit) {
        int prefixLength = StringUtils.length(prefix);
        int valueLength = StringUtils.length(value);
        if (prefixLength + valueLength <= limit) {
            return prefix + value;
        }
        int unprefixedLimit = limit - StringUtils.length(prefix);
        StringBuilder builder = new StringBuilder(prefix);
        int position = 0;
        boolean isFirstLine = true;
        while (position < valueLength) {
            if (!isFirstLine) {
                builder.append(StringUtils.LF).append(StringUtils.repeat(StringUtils.SPACE, prefixLength));
            }
            int endPosition = Math.min(position + unprefixedLimit, valueLength);
            builder.append(StringUtils.substring(value, position, endPosition));
            position = endPosition;
            isFirstLine = false;
        }
        return builder.toString();
    }

    /* -----------
       HTML output
       ----------- */

    private static String saveHtml(RunArguments runArguments, List<Diff> differences) {
        String leftLabel = ContentUtil.extractLabel(runArguments.getLeft());
        String rightLabel = ContentUtil.extractLabel(runArguments.getRight());

        try {
            String html = HtmlUtil.toHtml(differences);
            String reportPath = saveHtml(html, leftLabel, rightLabel);
            log.info("\nStored diff at {}", reportPath);
            return reportPath;
        } catch (IOException e) {
            log.error("Error storing report", e);
        }
        return null;
    }

    private static String saveHtml(String value, String leftLabel, String rightLabel) throws IOException {
        File htmlDirectory = Paths.get(System.getProperty("user.home"), ".etoolbox-anydiff/html").toFile();
        if (!htmlDirectory.exists() && !htmlDirectory.mkdirs()) {
            log.error("Error creating directory {}", htmlDirectory.getAbsolutePath());
            return null;
        }
        String fileName = createFileName(htmlDirectory, leftLabel, rightLabel);
        File outputFile = Paths.get(htmlDirectory.getAbsolutePath(), fileName).toFile();
        FileUtils.writeStringToFile(outputFile, value, StandardCharsets.UTF_8.name());
        return outputFile.getAbsolutePath();
    }

    private static String createFileName(File directory, String leftLabel, String rightLabel) {
        String leftEscaped = leftLabel.replaceAll(REPLACED_FILENAME_CHARS, Constants.DASH);
        String rightEscaped = rightLabel.replaceAll(REPLACED_FILENAME_CHARS, Constants.DASH);
        String fileNameBase = leftEscaped + "-vs-" + rightEscaped;
        String fileName = fileNameBase;
        int index = 1;
        while (new File(directory, fileName + EXTENSION_HTML).exists()) {
            fileName = fileNameBase + "(" + index++ + ")";
        }
        return fileName + EXTENSION_HTML;
    }
}
