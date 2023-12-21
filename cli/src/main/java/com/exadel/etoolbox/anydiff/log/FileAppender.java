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
package com.exadel.etoolbox.anydiff.log;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extends {@link  ch.qos.logback.core.FileAppender} to provide a custom log file cleanup logic
 * @param <E>
 */
public class FileAppender<E> extends ch.qos.logback.core.FileAppender<E> {

    private static final int DEFAULT_MAX_HISTORY = -1;

    private int maxHistory = DEFAULT_MAX_HISTORY;

    /**
     * Gets the maximum number of log files to keep in the log folder
     * @param maxHistory Maximum number of log files
     */
    @SuppressWarnings("unused")
    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    @Override
    public void start() {
        super.start();
        this.context.getScheduledExecutorService().submit(this::cleanUp);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void cleanUp() {
        File folder = new File(getFile()).getParentFile();
        if (!folder.isDirectory() || !folder.exists()) {
            return;
        }
        File[] fileArray = folder.listFiles();
        if (fileArray == null) {
            return;
        }
        List<File> fileList = Arrays.stream(fileArray).filter(File::isFile).collect(Collectors.toList());
        Arrays.stream(fileArray).filter(f -> f.length() == 0).forEach(f -> {
            f.delete();
            fileList.remove(f);
        });
        if (maxHistory <= 0 || fileList.size() <= maxHistory) {
            return;
        }
        Arrays.stream(fileArray)
                .filter(f -> StringUtils.endsWithAny(f.getPath(), ".txt", ".log"))
                .sorted((first, second) -> Long.compare(first.lastModified(), second.lastModified()) * -1)
                .skip(maxHistory)
                .forEach(File::delete);
    }
}
