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

import com.exadel.etoolbox.anydiff.AnyDiff;
import com.exadel.etoolbox.anydiff.OutputType;
import com.exadel.etoolbox.anydiff.diff.Fragment;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a string that can contain specially marked fragments
 */
class MarkedString {

    private final List<Chunk> chunks;
    private final boolean ignoreSpaces;
    private final boolean normalize;

    /**
     * Sets the reference to the previous {@code MarkedString} in the chain
     */
    @Setter
    private MarkedString previous;

    /**
     * Sets the reference to the next {@code MarkedString} in the chain
     */
    @Setter
    private MarkedString next;

    /* --------------
       Initialization
       -------------- */

    /**
     * Creates a {@code MarkedString} instance from the specified string
     * @param content String value
     */
    MarkedString(String content) {
        this(content, false, false);
    }

    /**
     * Creates a {@code MarkedString} instance from the specified text content. The whole string is marked with the
     * given {@link Marker}
     * @param content String value
     * @param marker  {@link Marker} reference
     */
    MarkedString(String content, Marker marker) {
        ignoreSpaces = false;
        normalize = false;
        if (StringUtils.isEmpty(content)) {
            chunks = Collections.emptyList();
            return;
        }
        chunks = new ArrayList<>();
        chunks.add(new Chunk(content, marker));
    }

    /**
     * Creates a {@code MarkedString} instance from the specified string
     * @param content      String value
     * @param ignoreSpaces If set to {@code true}, the dangling spaces are not respected when operations are performed
     *                     over in-string {@code Marker}s
     */
    MarkedString(String content, boolean ignoreSpaces) {
        this(content, ignoreSpaces, false);
    }

    /**
     * Creates a {@code MarkedString} instance from the specified string
     * @param content      String value
     * @param ignoreSpaces If set to {@code true}, the dangling spaces are not respected when operations are performed
     *                     over in-string {@code Marker}s
     * @param normalize    If set to {@code true}m the dangling spaces can be trimmed to compactify the output
     */
    MarkedString(String content, boolean ignoreSpaces, boolean normalize) {
        this.ignoreSpaces = ignoreSpaces;
        this.normalize = normalize;
        if (content == null) {
            chunks = Collections.emptyList();
            return;
        }
        chunks = new ArrayList<>();
        StringBuilder source = new StringBuilder(content);
        Pair<Integer, String> entry = StringUtil.getNearestSubstring(source, Marker.TOKENS);
        Marker lastOpenMarker = null;
        while (entry != null) {
            int position = entry.getKey();
            String token = entry.getValue();
            Marker currentMarker = Marker.fromString(token);
            if (currentMarker == null) {
                break;
            }
            if (lastOpenMarker != null && currentMarker == Marker.RESET) {
                chunks.add(new Chunk(source.substring(0, position), lastOpenMarker));
                lastOpenMarker = null;
            } else {
                if (position > 0) {
                    chunks.add(new Chunk(source.substring(0, position), null));
                }
                lastOpenMarker = currentMarker;
            }
            source.replace(0, position + currentMarker.toString().length(), StringUtils.EMPTY);
            entry = StringUtil.getNearestSubstring(source, Marker.TOKENS);
        }
        if (StringUtils.isNotEmpty(source)) {
            chunks.add(
                    new Chunk(
                            source.toString(),
                            lastOpenMarker != null && lastOpenMarker != Marker.RESET ? lastOpenMarker : null)
            );
        }
    }

    /* ----------
       Properties
       ---------- */

    /**
     * Gets the number of leading spaces not locked by any {@code Marker}
     * @return Integer value
     */
    int getIndent() {
        int indentableChunksCount = 0;
        for (Chunk chunk : chunks) {
            if (chunk.getMarker() != null && !normalize) {
                break;
            }
            indentableChunksCount += 1;
        }
        return StringUtil.getIndent(chunks.subList(0, indentableChunksCount));
    }

    /**
     * Gets whether the current {@code MarkedString} instance contains any {@code Marker}s that pose a disparity in the
     * comparison
     * @return True or false
     */
    boolean hasChanges() {
        return !chunks.isEmpty()
                && chunks.stream().anyMatch(chunk -> chunk.getMarker() != null && chunk.getMarker() != Marker.CONTEXT);
    }

    /* ---------
       Fragments
       --------- */

    /**
     * Gets a list of {@link Fragment} objects representing character sequences in the current string with a marker
     * @return A non-null list of {@code Fragment} objects. Can be empty
     */
    List<Fragment> getFragments() {
        return getFragments(false);
    }

    /**
     * Gets a list of {@link Fragment} objects representing character sequences in the current string with a marker
     * @param includeContext If set to {@code true}, the {@code Marker.CONTEXT} fragments are included in the result
     * @return A non-null list of {@code Fragment} objects. Can be empty
     */
    @SuppressWarnings("SameParameterValue")
    List<Fragment> getFragments(boolean includeContext) {
        return getFragments(false, includeContext);
    }

    /**
     * Gets a list of {@link Fragment} objects representing character sequences in the current string with a marker. The
     * fragments respect the XML/HTML markup contained in the string
     * @return A non-null list of {@code Fragment} objects. Can be empty
     */
    List<Fragment> getMarkupFragments() {
        return getFragments(true, false);
    }

    /**
     * Gets a list of {@link Fragment} objects representing character sequences in the current string with a marker. The
     * fragments respect the XML/HTML markup contained in the string
     * @param includeContext If set to {@code true}, the {@code Marker.CONTEXT} fragments are included in the result
     * @return A non-null list of {@code Fragment} objects. Can be empty
     */
    @SuppressWarnings("SameParameterValue")
    List<Fragment> getMarkupFragments(boolean includeContext) {
        return getFragments(true, includeContext);
    }

    private List<Fragment> getFragments(boolean markupAware, boolean includeContext) {
        StringBuilder textBuilder = new StringBuilder();
        List<Fragment> result = new ArrayList<>();
        int offset = 0;
        int lineOffset = 0;
        for (ChunkIterator it = new ChunkIterator(this); it.hasNext(); ) {
            Chunk chunk = it.next();
            textBuilder.append(chunk.getText());
            boolean isMarked = (includeContext && chunk.getMarker() != null)
                    || chunk.getMarker() == Marker.DELETE
                    || chunk.getMarker() == Marker.INSERT;
            if (isMarked && chunks.contains(chunk)) {
                // Must use {@code chunk.getText().length()} here instead of {@code chunk.length()} because the
                // latter reports +1 character for the "newline" type of chunk and is used for column formatting
                // while the former is used for offsetting
                FragmentImpl fragment = markupAware
                        ? new MarkupFragmentImpl(offset, offset + chunk.getText().length())
                        : new FragmentImpl(offset, offset + chunk.getText().length());
                fragment.setLineOffset(lineOffset);
                fragment.setDelete(chunk.isPending() && chunk.getMarker() == Marker.DELETE);
                fragment.setInsert(chunk.isPending() && chunk.getMarker() == Marker.INSERT);
                result.add(fragment);
            }
            offset += chunk.length();
            lineOffset += chunk.length();
            if (it.isLineEnd()) {
                textBuilder.append(StringUtils.LF);
                offset++;
                lineOffset = 0;
            }
        }
        String fullText = textBuilder.toString();
        result.forEach(fragment -> {
            FragmentImpl fragmentImpl = (FragmentImpl) fragment;
            fragmentImpl.setSource(fullText);
            if (ignoreSpaces) {
                fragmentImpl.trim();
            }
        });
        return result;
    }

    /* -------------------
       String manipulation
       ------------------- */

    /**
     * Accepts the current {@code MarkedString} instance, that is, "silences" all the character sequences with markers
     * so that they are not reported as a pending difference by {@link AnyDiff}
     */
    void accept() {
        chunks
                .stream()
                .filter(c -> c.getMarker() == Marker.INSERT || c.getMarker() == Marker.DELETE)
                .forEach(Chunk::accept);
    }

    /**
     * Accepts the specified {@code Fragment} instance, that is, "silences" it so that it is not reported as a pending
     * difference by {@link AnyDiff}
     * @param fragment {@code Fragment} instance. A non-null value is expected
     */
    void accept(Fragment fragment) {
        if (fragment == null) {
            return;
        }
        String fragmentString = fragment.toString();
        chunks
                .stream()
                .filter(c -> {
                    boolean isMatch = ignoreSpaces ? c.getText().trim().equals(fragmentString) :
                            c.getText().equals(fragmentString);
                    return isMatch && (c.getMarker() == Marker.INSERT || c.getMarker() == Marker.DELETE);
                })
                .findFirst()
                .ifPresent(Chunk::accept);
    }

    /**
     * Assigns a {@link Marker} to the whole content of the current {@code MarkedString} instance
     * @param marker {@code Marker} instance
     * @return Current instance
     */
    @SuppressWarnings("SameParameterValue")
    MarkedString mark(Marker marker) {
        if (chunks.isEmpty()) {
            return this;
        }
        String fullText = toString();
        chunks.clear();
        chunks.add(new Chunk(fullText, marker));
        return this;
    }

    /**
     * Assigns a {@link Marker} to the specified {@link Fragment} inside the current {@code MarkedString} instance
     * @param fragment {@code Fragment} instance. A non-null value is expected
     * @param marker   {@code Marker} instance
     */
    void mark(String fragment, Marker marker) {
        if (chunks.isEmpty()) {
            return;
        }
        LinkedList<Chunk> containerChunks = chunks
                .stream()
                .filter(c -> c.getText().contains(fragment))
                .collect(Collectors.toCollection(LinkedList::new));
        Iterator<Chunk> chunkIterator = containerChunks.descendingIterator();
        while (chunkIterator.hasNext()) {
            Chunk splittable = chunkIterator.next();
            int position = chunks.indexOf(splittable);
            if (splittable.getMarker() == marker) {
                return;
            } else if (splittable.getText().equals(fragment)) {
                splittable.setMarker(marker);
                return;
            }
            List<Chunk> splits = splittable.split(fragment);
            splits.stream().filter(c -> c.getText().equals(fragment)).forEach(c -> c.setMarker(marker));
            chunks.remove(position);
            chunks.addAll(position, splits);
        }
        compactify();
    }

    /**
     * Assigns the given {@link Marker} to the character sequences in the current {@code MarkedString} that have been
     * previously parked with {@link Marker#PLACEHOLDER}
     * @param newMarker {@code Marker} instance
     */
    MarkedString markPlaceholders(Marker newMarker) {
        if (chunks.isEmpty()) {
            return this;
        }
        chunks.stream().filter(chunk -> chunk.getMarker() == Marker.PLACEHOLDER).forEach(chunk -> chunk.setMarker(newMarker));
        return this;
    }

    /**
     * Clears all the {@code Marker}s from the current {@code MarkedString} instance
     * @return Current instance
     */
    MarkedString unmark() {
        String fullText = toString();
        chunks.clear();
        chunks.add(new Chunk(fullText, null));
        return this;
    }

    /**
     * Clears the {@code Marker} from the specified {@link Fragment} inside the current {@code MarkedString} instance
     * @param fragment {@code Fragment} instance. A non-null value is expected
     */
    void unmark(Fragment fragment) {
        if (fragment == null) {
            return;
        }
        String fragmentString = fragment.toString();
        chunks
                .stream()
                .filter(c -> ignoreSpaces
                        ? c.getText().trim().equals(fragmentString)
                        : c.getText().equals(fragmentString))
                .findFirst()
                .ifPresent(c -> c.setMarker(null));
        compactify();
    }

    /**
     * Removes the specified amount of characters from the left side of the current {@code MarkedString} instance.
     * Normally used to trim the indentation
     * @param count Number of characters to remove
     */
    void cutLeft(int count) {
        int toCut = count;
        while (toCut > 0) {
            if (chunks.isEmpty()) {
                return;
            }
            if (chunks.get(0).length() <= toCut) {
                toCut -= chunks.remove(0).length();
            } else {
                chunks.get(0).cutLeft(toCut);
                break;
            }
        }
    }

    /* ----------------
       Chunks utilities
       ---------------- */

    /**
     * Optimizes the composition of the current {@code MarkedString} instance by merging adjacent chunks with the same
     * marker
     */
    private void compactify() {
        if (chunks.isEmpty()) {
            return;
        }
        int cursor = 1;
        while (cursor < chunks.size()) {
            Chunk previous = chunks.get(cursor - 1);
            Chunk current = chunks.get(cursor);
            if (previous.getMarker() == current.getMarker()) {
                previous.append(current.getText());
                chunks.remove(cursor);
            } else {
                cursor++;
            }
        }
    }

    /* ------
       Output
       ------ */

    @Override
    public String toString() {
        return chunks.stream().map(Chunk::getText).collect(Collectors.joining());
    }

    /**
     * Retrieves the HTML representation of the current {@code MarkedString} instance
     * @return String value
     */
    String toHtml() {
        return chunks.stream().map(c -> c.toString(OutputType.HTML)).collect(Collectors.joining());
    }

    /**
     * Retrieves the text representation of the current {@code MarkedString} instance in the specified output format.
     * Lengthy strings are split into multiple lines with the specified maximum length
     * @param target {@link OutputType} value that defines the format
     * @param columnWidth Maximum number of characters in a column when displayed as a two-column table
     * @return A non-null list of strings. Can be empty
     */
    List<String> toText(OutputType target, int columnWidth) {
        if (CollectionUtils.isEmpty(chunks)) {
            return new ArrayList<>(Collections.singletonList(StringUtils.repeat(StringUtils.SPACE, columnWidth)));
        }
        return target == OutputType.LOG ? toLog(columnWidth) : toConsole(columnWidth);
    }

    private List<String> toLog(int columnWidth) {
        List<String> result = new ArrayList<>(StringUtil.splitByLength(
                chunks.stream().map(c -> c.toString(OutputType.LOG)).collect(Collectors.joining()),
                columnWidth));
        int lastPosition = result.size() - 1;
        String lastString = result.get(lastPosition);
        if (lastString.length() < columnWidth) {
            result.set(lastPosition, StringUtils.rightPad(lastString, columnWidth));
        }
        return result;
    }

    private List<String> toConsole(int columnWidth) {
        List<String> result = new ArrayList<>();
        Deque<Chunk> chunkQueue = new LinkedList<>(chunks);
        StringBuilder currentLine = new StringBuilder();
        int filledInLine = 0;
        while (!chunkQueue.isEmpty()) {
            Chunk currentChunk = chunkQueue.remove();
            if (currentChunk.length() > (columnWidth - filledInLine)) {
                Pair<Chunk, Chunk> pair = currentChunk.split(columnWidth - filledInLine);
                chunkQueue.addFirst(pair.getRight());
                chunkQueue.addFirst(pair.getLeft());
            } else {
                currentLine.append(currentChunk.toString(OutputType.CONSOLE));
                filledInLine += currentChunk.length();
                if (filledInLine >= columnWidth) {
                    result.add(currentLine.toString());
                    currentLine.setLength(0);
                    filledInLine = 0;
                }
            }
        }
        if (currentLine.length() > 0) {
            currentLine.append(StringUtils.repeat(StringUtils.SPACE, (columnWidth - filledInLine)));
            result.add(currentLine.toString());
        }
        return result;
    }

    /* ---------------
       Utility methods
       --------------- */

    /**
     * Gets whether the specified {@code MarkedString} instance is {@code null} or empty
     * @param value {@code MarkedString} value
     * @return True or false
     */
    static boolean isEmpty(MarkedString value) {
        return value == null || value.chunks.isEmpty();
    }

    /* ---------------
       Utility classes
       --------------- */

    /**
     * Represents a character sequence with a {@link Marker} assigned to it
     */
    private static class Chunk implements CharSequence {
        @Getter
        @Setter
        private Marker marker;

        private boolean isNewLine;

        private boolean silenced;

        @Getter
        private String text;

        Chunk(String text, Marker marker) {
            if (Marker.NEW_LINE.equals(text)) {
                this.text = StringUtils.EMPTY;
                this.isNewLine = true;
            } else {
                this.text = text;
            }
            this.marker = marker;
        }

        @Override
        public int length() {
            return isNewLine ? 1 : text.length();
        }

        @Override
        public char charAt(int position) {
            if (position < 0 || position >= text.length()) {
                return 0;
            }
            return text.charAt(position);
        }

        @Override
        public CharSequence subSequence(int begin, int end) {
            return text.subSequence(begin, end);
        }

        void accept() {
            silenced = true;
        }

        boolean isPending() {
            return !silenced && marker != null && marker != Marker.CONTEXT;
        }

        void append(String text) {
            this.text += text;
        }

        void cutLeft(int length) {
            if (length >= length()) {
                text = StringUtils.EMPTY;
            } else {
                text = text.substring(length);
            }
        }

        Pair<Chunk, Chunk> split(int position) {
            if (position >= length()) {
                return Pair.of(this, null);
            }
            return Pair.of(
                    new Chunk(text.substring(0, position), marker),
                    new Chunk(text.substring(position), marker));
        }

        List<Chunk> split(String separator) {
            if (StringUtils.isEmpty(separator) || !text.contains(separator)) {
                return Collections.singletonList(this);
            }
            List<Chunk> result = new ArrayList<>();
            String[] splits = StringUtils.splitByWholeSeparatorPreserveAllTokens(text, separator);
            for (int i = 0; i < splits.length; i++) {
                String split = splits[i];
                if (!StringUtils.isEmpty(split)) {
                    result.add(new Chunk(split, marker));
                }
                if (i < splits.length - 1) {
                    result.add(new Chunk(separator, marker));
                }
            }
            return result;
        }

        String toString(OutputType target) {
            String before = marker != null ? marker.to(target, null) : StringUtils.EMPTY;
            String main = text;
            if ((target == OutputType.CONSOLE || target == OutputType.HTML) && isNewLine) {
                main = StringUtils.SPACE;
            }
            if (target == OutputType.HTML) {
                main = StringUtil.escape(main);
            }
            String after = marker != null ? Marker.RESET.to(target, marker) : StringUtils.EMPTY;
            return before + main + after;
        }
    }

    /**
     * Represents an iterator over the {@link Chunk} objects in the current {@code MarkedString} instance
     */
    private static class ChunkIterator implements Iterator<Chunk> {
        private MarkedString nextSource;
        private Chunk nextChunk;

        public ChunkIterator(MarkedString source) {
            MarkedString firstSource = source;
            while (firstSource.previous != null) {
                firstSource = firstSource.previous;
            }
            nextSource = toNonEmptySource(firstSource);
            nextChunk = nextSource != null ? nextSource.chunks.get(0) : null;
        }

        @Override
        public boolean hasNext() {
            return nextChunk != null;
        }

        @Override
        public Chunk next() {
            Chunk result = nextChunk;
            int index = nextSource.chunks.indexOf(nextChunk);
            if (index < nextSource.chunks.size() - 1) {
                nextChunk = nextSource.chunks.get(index + 1);
            } else {
                nextSource = toNonEmptySource(nextSource.next);
                nextChunk = nextSource != null ? nextSource.chunks.get(0) : null;
            }
            return result;
        }

        boolean isLineEnd() {
            return hasNext() && nextSource.chunks.indexOf(nextChunk) == 0;
        }

        private static MarkedString toNonEmptySource(MarkedString source) {
            if (source == null) {
                return null;
            }
            if (CollectionUtils.isNotEmpty(source.chunks)) {
                return source;
            }
            return toNonEmptySource(source.next);
        }
    }
}
