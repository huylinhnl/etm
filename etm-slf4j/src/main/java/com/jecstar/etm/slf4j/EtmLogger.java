/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.slf4j;

import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.builder.ApplicationBuilder;
import com.jecstar.etm.domain.builder.EndpointBuilder;
import com.jecstar.etm.domain.builder.EndpointHandlerBuilder;
import com.jecstar.etm.domain.builder.LogTelemetryEventBuilder;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Instant;

class EtmLogger extends MarkerIgnoringBase implements LocationAwareLogger {

    private static final long serialVersionUID = 3661857038951845151L;

    private static final String LEVEL_TRACE = "TRACE";
    private static final String LEVEL_DEBUG = "DEBUG";
    private static final String LEVEL_INFO = "INFO";
    private static final String LEVEL_WARNING = "WARNING";
    private static final String LEVEL_ERROR = "ERROR";

    private final static String FQCN = EtmLogger.class.getName();
    private final EtmLogForwarder logForwarder;
    private final Configuration configuration;
    private final int logLevelAsInt;

    EtmLogger(EtmLogForwarder logForwarder, String loggerName, Configuration configuration) {
        this.name = loggerName;
        this.logForwarder = logForwarder;
        this.configuration = configuration;
        String logLevel = this.configuration.getLogLevel(getName());
        this.logLevelAsInt = determineLevelAsInteger(logLevel);
    }

    @Override
    public boolean isTraceEnabled() {
        return LocationAwareLogger.TRACE_INT >= this.logLevelAsInt;
    }

    @Override
    public void trace(String msg) {
        if (!isTraceEnabled()) {
            return;
        }
        log(FQCN, LEVEL_TRACE, msg, null);
    }

    @Override
    public void trace(String format, Object arg) {
        if (!isTraceEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arg);
        log(FQCN, LEVEL_TRACE, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (!isTraceEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        log(FQCN, LEVEL_TRACE, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (!isTraceEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arguments);
        log(FQCN, LEVEL_TRACE, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (!isTraceEnabled()) {
            return;
        }
        log(FQCN, LEVEL_TRACE, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return LocationAwareLogger.DEBUG_INT >= this.logLevelAsInt;
    }

    @Override
    public void debug(String msg) {
        if (!isDebugEnabled()) {
            return;
        }
        log(FQCN, LEVEL_DEBUG, msg, null);
    }

    @Override
    public void debug(String format, Object arg) {
        if (!isDebugEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arg);
        log(FQCN, LEVEL_DEBUG, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (!isDebugEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        log(FQCN, LEVEL_DEBUG, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (!isDebugEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arguments);
        log(FQCN, LEVEL_DEBUG, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (!isDebugEnabled()) {
            return;
        }
        log(FQCN, LEVEL_DEBUG, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return LocationAwareLogger.INFO_INT >= this.logLevelAsInt;
    }

    @Override
    public void info(String msg) {
        if (!isInfoEnabled()) {
            return;
        }
        log(FQCN, LEVEL_INFO, msg, null);
    }

    @Override
    public void info(String format, Object arg) {
        if (!isInfoEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arg);
        log(FQCN, LEVEL_INFO, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (!isInfoEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        log(FQCN, LEVEL_INFO, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void info(String format, Object... arguments) {
        if (!isInfoEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arguments);
        log(FQCN, LEVEL_INFO, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void info(String msg, Throwable t) {
        if (!isInfoEnabled()) {
            return;
        }
        log(FQCN, LEVEL_INFO, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return LocationAwareLogger.WARN_INT >= this.logLevelAsInt;
    }

    @Override
    public void warn(String msg) {
        if (!isWarnEnabled()) {
            return;
        }
        log(FQCN, LEVEL_WARNING, msg, null);
    }

    @Override
    public void warn(String format, Object arg) {
        if (!isWarnEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arg);
        log(FQCN, LEVEL_WARNING, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (!isWarnEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        log(FQCN, LEVEL_WARNING, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (!isWarnEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arguments);
        log(FQCN, LEVEL_WARNING, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (!isWarnEnabled()) {
            return;
        }
        log(FQCN, LEVEL_WARNING, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return LocationAwareLogger.ERROR_INT >= this.logLevelAsInt;
    }

    @Override
    public void error(String msg) {
        if (!isErrorEnabled()) {
            return;
        }
        log(FQCN, LEVEL_ERROR, msg, null);
    }

    @Override
    public void error(String format, Object arg) {
        if (!isErrorEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arg);
        log(FQCN, LEVEL_ERROR, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (!isErrorEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        log(FQCN, LEVEL_ERROR, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void error(String format, Object... arguments) {
        if (!isErrorEnabled()) {
            return;
        }
        FormattingTuple ft = MessageFormatter.format(format, arguments);
        log(FQCN, LEVEL_ERROR, ft.getMessage(), ft.getThrowable());
    }

    @Override
    public void error(String msg, Throwable t) {
        if (!isErrorEnabled()) {
            return;
        }
        log(FQCN, LEVEL_ERROR, msg, t);
    }

    @Override
    public void log(Marker marker, String fqcn, int level, String message, Object[] argArray, Throwable t) {
        String logLevel;
        switch (level) {
            case LocationAwareLogger.TRACE_INT:
                if (!isTraceEnabled()) {
                    return;
                }
                logLevel = LEVEL_TRACE;
                break;
            case LocationAwareLogger.DEBUG_INT:
                if (!isDebugEnabled()) {
                    return;
                }
                logLevel = LEVEL_DEBUG;
                break;
            case LocationAwareLogger.INFO_INT:
                if (!isInfoEnabled()) {
                    return;
                }
                logLevel = LEVEL_INFO;
                break;
            case LocationAwareLogger.WARN_INT:
                if (!isWarnEnabled()) {
                    return;
                }
                logLevel = LEVEL_WARNING;
                break;
            case LocationAwareLogger.ERROR_INT:
                if (!isErrorEnabled()) {
                    return;
                }
                logLevel = LEVEL_ERROR;
                break;
            default:
                throw new IllegalStateException("Level number " + level + " is not recognized.");
        }
        log(fqcn, logLevel, message, t);
    }


    private int determineLevelAsInteger(String logLevel) {
        if (LEVEL_TRACE.equals(logLevel)) {
            return LocationAwareLogger.TRACE_INT;
        } else if (LEVEL_DEBUG.equals(logLevel)) {
            return LocationAwareLogger.DEBUG_INT;
        } else if (LEVEL_INFO.equals(logLevel)) {
            return LocationAwareLogger.INFO_INT;
        } else if (LEVEL_WARNING.equals(logLevel)) {
            return LocationAwareLogger.WARN_INT;
        } else if (LEVEL_ERROR.equals(logLevel)) {
            return LocationAwareLogger.ERROR_INT;
        }
        return Integer.MAX_VALUE;
    }


    private void log(String callerFQCN, String logLevel, String payload, Throwable throwable) {
        LogLocation logLocation = new LogLocation(new Throwable(), callerFQCN);
        String stackTrace = null;
        if (throwable != null) {
            try (Writer errors = new StringWriter(); PrintWriter pw = new PrintWriter(errors)) {
                throwable.printStackTrace(pw);
                stackTrace = errors.toString();
            } catch (IOException e) {
            }

        }
        LogTelemetryEvent logTelemetryEvent = new LogTelemetryEventBuilder()
                .setName(getName())
                .setLogLevel(logLevel)
                .setPayload(payload)
                .setStackTrace(stackTrace)
                .addOrMergeEndpoint(new EndpointBuilder().setName(logLocation.fullInfo)
                        .addEndpointHandler(new EndpointHandlerBuilder()
                                .setType(EndpointHandler.EndpointHandlerType.WRITER)
                                .setHandlingTime(Instant.now())
                                .setApplication(new ApplicationBuilder()
                                        .setName(this.configuration.getApplicationName())
                                        .setVersion(this.configuration.getApplicationVersion())
                                        .setInstance(this.configuration.getApplicationInstance())
                                        .setPrincipal(this.configuration.getPrincipalName())
                                        .setHostAddress(this.configuration.getHostAddress())
                                )))
                .build();
        this.logForwarder.forwardLog(logTelemetryEvent);

    }

}
