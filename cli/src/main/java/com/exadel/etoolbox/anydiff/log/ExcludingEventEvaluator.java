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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EventEvaluatorBase;
import com.exadel.etoolbox.anydiff.Constants;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Implements a Logback event evaluator that allows filtering out log entries by the event level threshold and a
 * specially assigned marker
 */
@SuppressWarnings("unused")
public class ExcludingEventEvaluator extends EventEvaluatorBase<ILoggingEvent> {

    private String exclude;
    private Level localThreshold;
    private Level foreignThreshold;

    /**
     * Sets the name of the marker that, if present in the log entry, will cause the entry to be excluded from the log
     * @param value String value. A non-empty string is expected
     */
    public void setExclude(String value) {
        exclude = value;
    }

    /**
     * Sets the threshold level for log entries originating from the code of the current project
     * @param value String value. A non-empty string is expected
     */
    public void setLocalCodeThreshold(String value) {
        localThreshold = Level.toLevel(value, Level.TRACE);
    }

    /**
     * Sets the threshold level for log entries originating from the code of the third-party projects
     * @param value String value. A non-empty string is expected
     */
    public void setForeignCodeThreshold(String value) {
        foreignThreshold = Level.toLevel(value, Level.TRACE);
    }

    @Override
    public boolean evaluate(ILoggingEvent event) throws NullPointerException {
        if (!isStarted()) {
            return false;
        }
        return shouldAllowByExcludeFlag(event) && shouldAllowByThreshold(event);
    }

    private boolean shouldAllowByExcludeFlag(ILoggingEvent event) {
        return event.getMarkerList() == null
                || event.getMarkerList().stream().noneMatch(marker -> marker.getName().equals(exclude));
    }

    private boolean shouldAllowByThreshold(ILoggingEvent event) {
        if (event.getLevel() == null) {
            return true;
        }
        String caller = ArrayUtils.isNotEmpty(event.getCallerData()) ? event.getCallerData()[0].getClassName() : null;
        Level effectiveLevel = caller != null && caller.startsWith(Constants.ROOT_PACKAGE)
                ? localThreshold
                : foreignThreshold;
        return event.getLevel() == null || event.getLevel().isGreaterOrEqual(effectiveLevel);
    }
}
