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

import com.exadel.etoolbox.anydiff.ContentType;
import com.exadel.etoolbox.anydiff.diff.Diff;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class DiffBlockXPathTest {

    @Test
    public void shouldReportHtmlPath() throws IOException {
        try (
                InputStream leftInput = getClass().getResourceAsStream("/sample/left/html/file2.html");
                InputStream rightInput = getClass().getResourceAsStream("/sample/right/html/file2.html")
        ) {
            Assert.assertNotNull(leftInput);
            Assert.assertNotNull(rightInput);
            String left = IOUtils.toString(leftInput, StandardCharsets.UTF_8);
            String right = IOUtils.toString(rightInput, StandardCharsets.UTF_8);
            right = right.replace("<head>", "<head><meta value=\"Additional 1\"/>");
            right = right.replace("<body class=\"body\">", "<body class=\"body\"><p>Additional 2</p>");
            right = right.replace("</body>", "<p>Additional 3</p></body>");

            Diff diff = DiffTask
                    .builder()
                    .leftContent(left)
                    .rightContent(right)
                    .contentType(ContentType.HTML)
                    .build()
                    .run();

            Assert.assertEquals(5, diff.children().size());
            Assert.assertEquals("/html/head/meta", ((BlockImpl) diff.children().get(0)).getPath());
            Assert.assertEquals("/html/body/p", ((BlockImpl) diff.children().get(1)).getPath());
            Assert.assertEquals("/html/body/h1", ((BlockImpl) diff.children().get(2)).getPath());
            Assert.assertEquals("/html/body/h1", ((BlockImpl) diff.children().get(3)).getPath());
            Assert.assertEquals("/html/body/h1", ((BlockImpl) diff.children().get(4)).getPath());
        }
    }

    @Test
    public void shouldReportXmlPath() throws IOException {
        try (
                InputStream leftInput = getClass().getResourceAsStream("/sample/left/xml/file3.xml");
                InputStream rightInput = getClass().getResourceAsStream("/sample/right/xml/file3.xml")
        ) {
            Assert.assertNotNull(leftInput);
            Assert.assertNotNull(rightInput);
            String left = IOUtils.toString(leftInput, StandardCharsets.UTF_8);
            String right = IOUtils.toString(rightInput, StandardCharsets.UTF_8);
            right = StringUtils.replace(right, "</items>", "<more>Addition</more></items>");
            Diff diff = DiffTask
                    .builder()
                    .leftContent(left)
                    .rightContent(right)
                    .contentType(ContentType.XML)
                    .build()
                    .run();
            Assert.assertEquals("/catalog/items/item", ((BlockImpl) diff.children().get(0)).getPath());
            Assert.assertEquals("/catalog/items/item[2]/title", ((BlockImpl) diff.children().get(1)).getPath());
            Assert.assertEquals("/catalog/items/item[2]/ranking", ((BlockImpl) diff.children().get(2)).getPath());
            Assert.assertEquals("/catalog/items/more", ((BlockImpl) diff.children().get(3)).getPath());
        }
    }

    @Test
    public void shouldReportManifestPath() throws IOException {
        try (
                InputStream leftInput = getClass().getResourceAsStream("/sample/left/mf/MANIFEST.MF");
                InputStream rightInput = getClass().getResourceAsStream("/sample/right/mf/MANIFEST.MF")
        ) {
            Assert.assertNotNull(leftInput);
            Assert.assertNotNull(rightInput);
            String left = IOUtils.toString(leftInput, StandardCharsets.UTF_8);
            String right = IOUtils.toString(rightInput, StandardCharsets.UTF_8);
            Diff diff = DiffTask
                    .builder()
                    .leftContent(left)
                    .rightContent(right)
                    .contentType(ContentType.MANIFEST)
                    .build()
                    .run();
            Assert.assertEquals(2, diff.children().size());
            Assert.assertEquals("Import-Package", ((BlockImpl) diff.children().get(0)).getPath());
            Assert.assertEquals("Private-Package", ((BlockImpl) diff.children().get(1)).getPath());
        }
    }
}
