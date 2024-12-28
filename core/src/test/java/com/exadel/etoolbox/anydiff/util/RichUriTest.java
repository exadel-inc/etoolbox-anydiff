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
package com.exadel.etoolbox.anydiff.util;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class RichUriTest {

    @Test
    public void shouldParseUri() {
        List<String> uris = Arrays.asList(
            "http://www.acme.com",
            "https://www.acme.com#fragment",
            "http://www.acme.com/path",
            "https://www.acme.com/path?query=param",
            "https://www.acme.com/path.with-selector.html?query=param",
            "ftp://ftp.acme.com/path",
            "file:///C:/path/to/file.txt",
            "file:///home/user/file.txt",
            "C:\\path\\to\\мой файл.txt",
            "C:\\path\\to\\file.txt",
            "file:///C:/path/to/мой_файл.txt?query=param",
            "urn:isbn:0451450523",
            "urn:ietf:rfc:2648",
            "http://localhost",
            "http://localhost:8080",
            "customscheme://acme",
            "/path/to/resource",
            "../path/to/resource",
            "http://192.168.0.1",
            "http://[ff06:0:0:0:0:0:0:c3]"
        );

        for (String uri : uris) {
            try {
                RichUri richUri = new RichUri(uri);
                Assert.assertNotNull(richUri);
                Assert.assertNotNull(richUri.getUri());
                Assert.assertTrue(MapUtils.isEmpty(richUri.getOptions()));
                Assert.assertEquals(
                    uri.replace('\\', '/').replace(StringUtils.SPACE, "%20"),
                    richUri.toString());
            } catch (Exception e) {
                Assert.fail("Failed to parse URI " + uri + ": " + e.getMessage());
            }
        }
    }

    @Test
    public void shouldParseUriWithUserInfo() throws URISyntaxException, IOException {
        RichUri richUri = new RichUri("https://admin:admin@www.acme.com#fragment");
        Assert.assertNotNull(richUri);
        Assert.assertNotNull(richUri.getUri());
        Assert.assertEquals("admin:admin", richUri.getUserInfo());
        Assert.assertEquals(
            "https://www.acme.com#fragment",
            richUri.toString());

        richUri = new RichUri("mailto:user@acme.com?subject=Hello");
        Assert.assertNotNull(richUri);
        Assert.assertNotNull(richUri.getUri());
        Assert.assertEquals("user", richUri.getUserInfo());
    }

    @Test
    public void shouldParseUriWithOptions() throws URISyntaxException, IOException {
        RichUri richUri = new RichUri("https://www.acme.com/some/path.4aa4-9635.html?foo=bar&@baz=qux&@qu-ux='some / value'");
        Assert.assertNotNull(richUri);
        Assert.assertNotNull(richUri.getUri());
        Assert.assertTrue(MapUtils.isNotEmpty(richUri.getOptions()));
        Assert.assertEquals("qux", richUri.getOptions().get("baz"));
        Assert.assertEquals("some / value", richUri.getOptions().get("qu-ux"));
        Assert.assertEquals(
            "https://www.acme.com/some/path.4aa4-9635.html?foo=bar",
            richUri.toString());
    }
}
