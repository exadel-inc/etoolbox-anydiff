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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.exadel.etoolbox.anydiff.Constants;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * Extends {@link  ch.qos.logback.core.ConsoleAppender} to handle both transient (disappearing) and persistent console
 * messages
 */
public class ConsoleAppender extends ch.qos.logback.core.ConsoleAppender<ILoggingEvent> {

    private int transientMessageLength = 0;

    @Override
    protected void writeOut(ILoggingEvent event) throws IOException {
        if (!(event instanceof LoggingEvent)) {
            super.writeOut(event);
            return;
        }
        if (transientMessageLength > 0) {
            LoggingEvent newEvent = new LoggingEvent();
            newEvent.setMessage("." + StringUtils.repeat('\b', transientMessageLength));
            super.writeOut(newEvent);
            transientMessageLength = 0;
        }
        if (StringUtils.startsWith(event.getMessage(), Constants.DOT)) {
            transientMessageLength = event.getFormattedMessage().length() - 1;
        }
        super.writeOut(event);
    }
}
