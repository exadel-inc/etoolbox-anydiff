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
package com.exadel.etoolbox.anydiff.comparison.preprocessor;

import com.exadel.etoolbox.anydiff.comparison.TaskParameters;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PreprocessorsTest {

    @Test
    public void shouldReformatHtml() throws IOException {
        try (
                InputStream rawInput = getClass().getResourceAsStream("/preprocessor/raw.html");
                InputStream prettyInput = getClass().getResourceAsStream("/preprocessor/pretty.html")
        ) {
            Assert.assertNotNull(rawInput);
            Assert.assertNotNull(prettyInput);
            String rawHtml = IOUtils.toString(rawInput, StandardCharsets.UTF_8);
            String prettyHtml = IOUtils.toString(prettyInput, StandardCharsets.UTF_8);
            String computedPrettyHtml = new HtmlPreprocessor(TaskParameters.builder().ignoreSpaces(true).build()).apply(rawHtml);
            Assert.assertEquals(prettyHtml, computedPrettyHtml);
        }
    }

    @Test
    public void shouldReformatXml() throws IOException {
        try (
                InputStream rawInput = getClass().getResourceAsStream("/preprocessor/raw.xml");
                InputStream prettyInput = getClass().getResourceAsStream("/preprocessor/pretty.xml")
        ) {
            Assert.assertNotNull(rawInput);
            Assert.assertNotNull(prettyInput);
            String rawHtml = IOUtils.toString(rawInput, StandardCharsets.UTF_8);
            String prettyHtml = IOUtils.toString(prettyInput, StandardCharsets.UTF_8);
            String computedPrettyHtml = new XmlPreprocessor(TaskParameters.builder().build()).apply(rawHtml);
            Assert.assertEquals(prettyHtml, computedPrettyHtml);
        }
    }

    @Test
    public void shouldReformatManifest() throws IOException {
        try (
            InputStream rawInput = getClass().getResourceAsStream("/preprocessor/raw.mf");
            InputStream prettyInput = getClass().getResourceAsStream("/preprocessor/pretty.mf")
        ) {
            Assert.assertNotNull(rawInput);
            Assert.assertNotNull(prettyInput);
            String rawMf = IOUtils.toString(rawInput, StandardCharsets.UTF_8);
            String prettyMf = IOUtils.toString(prettyInput, StandardCharsets.UTF_8);
            String computedPrettyMf = new ManifestPreprocessor().apply(rawMf);
            Assert.assertEquals(prettyMf, computedPrettyMf);
        }
    }
}
