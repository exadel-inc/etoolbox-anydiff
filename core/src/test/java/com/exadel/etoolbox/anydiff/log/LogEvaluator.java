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
import ch.qos.logback.core.boolex.EventEvaluatorBase;
import com.exadel.etoolbox.anydiff.Constants;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
public class LogEvaluator extends EventEvaluatorBase<ILoggingEvent> {
    @Override
    public boolean evaluate(ILoggingEvent event) throws NullPointerException {
        return !StringUtils.startsWith(event.getMessage(), Constants.DOT);
    }
}
