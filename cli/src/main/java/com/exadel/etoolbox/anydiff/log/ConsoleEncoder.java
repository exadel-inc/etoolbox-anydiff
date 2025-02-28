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

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.exadel.etoolbox.anydiff.Constants;
import org.apache.commons.lang3.StringUtils;

/**
 * Extends {@link PatternLayoutEncoder} to handle both transient and persistent console messages
 */
public class ConsoleEncoder extends PatternLayoutEncoder {

    @Override
    public byte[] encode(ILoggingEvent event) {
        if (StringUtils.startsWith(event.getMessage(), Constants.DOT)) {
            return event.getFormattedMessage().substring(Constants.DOT.length()).getBytes();
        }
        return super.encode(event);
    }
}
