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

/*
 * This file uses parts of the code from the "BareBonesBrowserLaunch" snippet (https://centerkey.com/java/browser)
 * which is in the public domain
 */
package com.exadel.etoolbox.anydiff;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides utility methods to launch a browser from the command line
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
class Browser {
    private static final String[] BROWSERS = {
            "x-www-browser",
            "google-chrome",
            "firefox",
            "opera",
            "epiphany",
            "konqueror",
            "conkeror",
            "midori",
            "kazehakase",
            "mozilla"
    };

    /**
     * Attempts to launch a browser with the specified URL as an argument
     * @param url URL to open
     */
    public static void launch(String url) {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Class.forName("com.apple.eio.FileManager")
                        .getDeclaredMethod("openURL", String.class)
                        .invoke(null, url);
            } else if (osName.startsWith("Windows")) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else {
                for (String browser : BROWSERS)
                    if (Runtime.getRuntime().exec(new String[]{"which", browser}).getInputStream().read() != -1) {
                        Runtime.getRuntime().exec(new String[]{browser, url});
                        break;
                    }
            }
        } catch (Exception e) {
            log.error("Failed to launch browser", e);
        }
    }
}