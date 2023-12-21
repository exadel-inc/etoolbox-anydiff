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

import com.exadel.etoolbox.anydiff.Constants;
import com.exadel.etoolbox.anydiff.comparison.Marker;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.zip.CRC32;

/**
 * Stores file or URL metadata and allows comparing objects by their metadata parameters
 */
@Builder(builderClassName = "Builder")
@Slf4j
class FileMetadata {

    private static final int CRC_COMPUTATION_THRESHOLD = 1024 * 1024 * 20; // 20 Mb

    /**
     * Gets the path or the URL of the file
     */
    @Getter
    private String path;

    private long crc;

    private long size;

    private Date lastModified;

    private boolean pathAgnostic;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
                .append(Marker.CONTEXT.wrap("Path:"))
                .append(StringUtils.SPACE)
                .append(Marker.PLACEHOLDER.wrap(path));
        if (crc != 0) {
            builder
                    .append(StringUtils.LF)
                    .append(Marker.CONTEXT.wrap("CRC:"))
                    .append(StringUtils.SPACE)
                    .append(Marker.PLACEHOLDER.wrap(crc));
        }
        if (size != 0) {
            builder
                    .append(StringUtils.LF)
                    .append(Marker.CONTEXT.wrap("Size:"))
                    .append(StringUtils.SPACE)
                    .append(Marker.PLACEHOLDER.wrap(toKilobytes(size)));
        }
        if (lastModified != null) {
            builder
                    .append(StringUtils.LF)
                    .append(Marker.CONTEXT.wrap("Last modified:"))
                    .append(StringUtils.SPACE)
                    .append(Marker.PLACEHOLDER.wrap(lastModified));

        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        FileMetadata that = (FileMetadata) object;
        String thisPath = this.pathAgnostic ? StringUtils.substringAfterLast(this.path, Constants.DOT) : this.path;
        String thatPath = that.pathAgnostic ? StringUtils.substringAfterLast(that.path, Constants.DOT) : that.path;
        // If hash codes are computed, we check that the hash codes are equal, and so are the path extracts
        // (either full paths or just the extensions if the {@code pathAgnostic} mode is on
        if (crc != 0) {
            return crc == that.crc && StringUtils.equals(thisPath, thatPath);
        }
        return new EqualsBuilder()
                .append(thisPath, thatPath)
                .append(size, that.size)
                .append(lastModified, that.lastModified)
                .isEquals();
    }

    @Override
    public int hashCode() {
        String thisPath = pathAgnostic ? StringUtils.substringAfterLast(this.path, Constants.DOT) : this.path;
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder(17, 37);
        if (crc != 0) {
            return hashCodeBuilder.append(thisPath).append(crc).toHashCode();
        }
        return hashCodeBuilder
                .append(thisPath)
                .append(size)
                .append(lastModified)
                .toHashCode();
    }

    /**
     * Computes CRC32 hash for the file
     * @param value A {@link Path} object representing the file. A non-null value is expected
     * @return A {@code long} value representing the CRC32 hash
     */
    static long computeCrc(Path value) {
        return computeCrc(value, 0L);
    }

    /**
     * Computes CRC32 hash for the file if its size fits in the specified threshold
     * @param value A {@link Path} object representing the file. A non-null value is expected
     * @param size  The file size in bytes that will be checked against the threshold
     * @return A {@code long} value representing the CRC32 hash, or {@code 0} if the size does not fit into the
     * threshold
     */
    static long computeCrc(Path value, long size) {
        if (size < 0 || size > CRC_COMPUTATION_THRESHOLD) {
            return 0;
        }
        try (InputStream inputStream = Files.newInputStream(value)) {
            CRC32 crc32 = new CRC32();
            while (inputStream.available() > 0) {
                int read = inputStream.read();
                crc32.update(read);
            }
            return crc32.getValue();
        } catch (IOException e) {
            log.error("Error hashing file {}", value, e);
        }
        return 0;
    }

    private static String toKilobytes(long bytes) {
        return String.format("%.1f Kb", bytes / 1024.0);
    }
}
