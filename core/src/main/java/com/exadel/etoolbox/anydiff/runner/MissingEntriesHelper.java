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
package com.exadel.etoolbox.anydiff.runner;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A helper class that is used to determine the missing entries detect moved entries between the two directories or
 * archive files
 */
class MissingEntriesHelper {

    private final List<String> leftEntries;
    private final List<String> rightEntries;
    private final Function<String, Long> leftCrcSupplier;
    private final Function<String, Long> rightCrcSupplier;

    private final List<String> missingLeft = new ArrayList<>();
    private final List<String> missingRight = new ArrayList<>();
    private final Map<String, String> moved = new HashMap<>();

    /**
     * Creates a new instance of {@code MissingEntriesHelper} class
     * @param leftEntries      List of entries that represent the left side of the comparison
     * @param rightEntries     List of entries that represent the right side of the comparison
     * @param leftCrcSupplier  A {@code Function} that can be used to compute CRC values for the left-side entries
     * @param rightCrcSupplier A {@code Function} that can be used to compute CRC values for the right-side entries
     */
    MissingEntriesHelper(
            List<String> leftEntries,
            List<String> rightEntries,
            Function<String, Long> leftCrcSupplier,
            Function<String, Long> rightCrcSupplier) {

        this.leftEntries = leftEntries;
        this.rightEntries = rightEntries;
        this.leftCrcSupplier = leftCrcSupplier;
        this.rightCrcSupplier = rightCrcSupplier;
        prepare();
    }

    /**
     * Checks if the specified entry is missing on the left side of the comparison
     * @param value Entry identifier. A non-null string is expected
     * @return True or false
     */
    boolean isMissingLeft(String value) {
        return missingLeft.contains(value);
    }

    /**
     * Checks if the specified entry is missing on the right side of the comparison
     * @param value Entry identifier. A non-null string is expected
     * @return True or false
     */
    boolean isMissingRight(String value) {
        return missingRight.contains(value);
    }

    /**
     * Gets the path of the entry that was moved from the left side to the right side of the comparison
     * @param value Entry identifier. A non-null string is expected
     * @return A string representing the path of the moved entry, or {@code null} if the entry was not moved
     */
    String getMoved(String value) {
        return moved.get(value);
    }

    private void prepare() {
        List<FileMetadata> missingInLeft = new ArrayList<>();
        List<FileMetadata> missingInRight = new ArrayList<>();

        for (String entry : leftEntries) {
            if (!rightEntries.contains(entry)) {
                long crc = leftCrcSupplier.apply(entry);
                missingInRight.add(FileMetadata.builder().path(entry).crc(crc).pathAgnostic(true).build());
            }
        }
        for (String entry : rightEntries) {
            if (!leftEntries.contains(entry)) {
                long crc = rightCrcSupplier.apply(entry);
                missingInLeft.add(FileMetadata.builder().path(entry).crc(crc).pathAgnostic(true).build());
            }
        }
        for (FileMetadata metadata : CollectionUtils.intersection(missingInLeft, missingInRight)) {
            String leftPath = missingInRight.remove(missingInRight.indexOf(metadata)).getPath();
            String rightPath = missingInLeft.remove(missingInLeft.indexOf(metadata)).getPath();
            moved.put(leftPath, rightPath);
        }
        missingInLeft.forEach(metadata -> this.missingLeft.add(metadata.getPath()));
        missingInRight.forEach(metadata -> this.missingRight.add(metadata.getPath()));
    }
}
