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

import com.exadel.etoolbox.anydiff.diff.Diff;
import com.exadel.etoolbox.anydiff.diff.DiffState;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class DiffRunnerTest {

    private MockedStatic<HttpClientFactory> httpClientFactory;
    private Response httpResponse;

    @Before
    public void init() {
        httpResponse = new Response.Builder()
                .request(new okhttp3.Request.Builder().url("https://acme.com/1.html").build())
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("test", okhttp3.MediaType.parse("text/plain")))
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                chain.proceed(chain.request());
                return httpResponse;
            }).build();

        httpClientFactory = Mockito.mockStatic(HttpClientFactory.class);
        Mockito.when(HttpClientFactory.getInstance()).thenReturn(new HttpClientFactory() {
            @Override
            public OkHttpClient newClient(boolean useProxy, String proxyHost) {
                return client;
            }
        });
    }

    @After
    public void destroy() {
        httpClientFactory.close();
        httpResponse.close();
    }

    @Test
    public void shouldCompareStrings() throws URISyntaxException {
        URL resourcesRoot = getClass().getResource("/sample");
        Assert.assertNotNull(resourcesRoot);

        String leftPath = Paths.get(resourcesRoot.toURI()).resolve("left/html/files.list").toAbsolutePath().toString();
        String rightPath = Paths.get(resourcesRoot.toURI()).resolve("right/html/files.list").toAbsolutePath().toString();
        DiffRunner runner = DiffRunner.forValues(leftPath, null, rightPath, null);
        Assert.assertTrue(runner instanceof SimpleRunner);
    }

    @Test
    public void shouldCompareStringArrays() {
        String[] left = new String[] {"Lorem", "ipsum"};
        String[] right = new String[] {"Dolor", "sit", "amet"};
        DiffRunner runner = DiffRunner.forValues(left, null, right, null);
        Assert.assertTrue(runner instanceof StringListRunner);
    }

    @Test
    public void shouldCompareArchives() {
        URL leftUrl = getClass().getResource("/sample/left/left.zip");
        URL rightUrl = getClass().getResource("/sample/right/right.zip");
        Assert.assertNotNull(leftUrl);
        Assert.assertNotNull(rightUrl);

        String leftPath = leftUrl.getPath().substring(1);
        String rightPath = rightUrl.getPath().substring(1);
        DiffRunner runner = DiffRunner.forValues(leftPath, null, rightPath, null);
        Assert.assertTrue(runner instanceof ArchiveRunner);

        List<Diff> differences = runner.run();
        Assert.assertEquals(5, differences.size());
        Assert.assertEquals(3, differences.stream().filter(diff -> diff.getState() == DiffState.CHANGE).count());
    }

    @Test
    public void shouldCompareDirectories() {

        URL leftUrl = getClass().getResource("/sample/left");
        URL rightUrl = getClass().getResource("/sample/right");
        Assert.assertNotNull(leftUrl);
        Assert.assertNotNull(rightUrl);

        String leftPath = leftUrl.getPath().substring(1);
        String rightPath = rightUrl.getPath().substring(1);
        DiffRunner runner = DiffRunner.forValues(leftPath, null, rightPath, null);
        Assert.assertTrue(runner instanceof DirectoryRunner);

        List<Diff> differences = runner.run();
        Assert.assertEquals(10, differences.size());
        Assert.assertEquals(6, differences.stream().filter(diff -> diff.getState() == DiffState.CHANGE).count());
    }

    @Test
    public void shouldCompareListings() {
        URL leftUrl = getClass().getResource("/sample/left/files.list");
        URL rightUrl = getClass().getResource("/sample/right/files.lst");
        Assert.assertNotNull(leftUrl);
        Assert.assertNotNull(rightUrl);

        String leftPath = leftUrl.getPath().substring(1);
        String rightPath = rightUrl.getPath().substring(1);
        DiffRunner runner = DiffRunner.forValues(leftPath, null, rightPath, null);
        Assert.assertTrue(runner instanceof FileListingRunner);

        List<Diff> differences = runner.run();
        Assert.assertEquals(2, differences.size());
    }

    @Test
    public void shouldCompareHttpEndpoints() {
        String left = "https://acme.com/1.html";
        String right = "https://acme.com/2.html";
        DiffRunner runner = DiffRunner.forValues(left, null, right, null);
        Assert.assertTrue(runner instanceof HttpRunner);

        List<Diff> differences = runner.run();
        Assert.assertEquals(1, differences.size());
    }
}
