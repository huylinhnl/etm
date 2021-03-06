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

package com.jecstar.etm.server.core.logging;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Log wrapper that wraps the <a
 * href="http://java.sun.com/j2se/1.4.2/docs/guide/util/logging/index.html">Java
 * Util Logging</a> logging system.
 *
 * @author Mark Holster
 */
public class JULLogWrapper extends AbstractDelegateLogWrapper {

    /**
     * The JUL logger.
     */
    private final Logger logger;

    /**
     * JUL doesn't support a FATAL level. We have to keep track of the current
     * level in the LogWrapper itself.
     */
    private LogLevel currentLogLevel;

    /**
     * Constructs a new <code>JULLogWrapper</code> instance.
     *
     * @param loggerName The name of the logger.
     */
    public JULLogWrapper(String loggerName) {
        super(loggerName);
        this.logger = Logger.getLogger(loggerName);
        Level level = this.logger.getLevel();
        if (Level.FINE.equals(level)) {
            this.currentLogLevel = LogLevel.DEBUG;
        } else if (Level.INFO.equals(level)) {
            this.currentLogLevel = LogLevel.INFO;
        } else if (Level.WARNING.equals(level)) {
            this.currentLogLevel = LogLevel.WARNING;
        } else if (Level.SEVERE.equals(level)) {
            this.currentLogLevel = LogLevel.ERROR;
        }
    }

    @Override
    public boolean isDebugLevelEnabled() {
        return this.logger.isLoggable(Level.FINE);
    }

    @Override
    public boolean isErrorLevelEnabled() {
        return this.logger.isLoggable(Level.SEVERE) && (LogLevel.DEBUG.equals(this.currentLogLevel) || LogLevel.INFO.equals(this.currentLogLevel) || LogLevel.WARNING.equals(this.currentLogLevel) || LogLevel.ERROR.equals(this.currentLogLevel) || Level.ALL.equals(this.logger.getLevel()) || Level.CONFIG.equals(this.logger.getLevel()) || Level.FINER.equals(this.logger.getLevel()) || Level.FINEST.equals(this.logger.getLevel()));
    }

    @Override
    public boolean isFatalLevelEnabled() {
        return this.logger.isLoggable(Level.SEVERE) && (LogLevel.DEBUG.equals(this.currentLogLevel) || LogLevel.INFO.equals(this.currentLogLevel) || LogLevel.WARNING.equals(this.currentLogLevel) || LogLevel.ERROR.equals(this.currentLogLevel) || LogLevel.FATAL.equals(this.currentLogLevel) || Level.ALL.equals(this.logger.getLevel()) || Level.CONFIG.equals(this.logger.getLevel()) || Level.FINER.equals(this.logger.getLevel()) || Level.FINEST.equals(this.logger.getLevel()));
    }

    @Override
    public boolean isInfoLevelEnabled() {
        return this.logger.isLoggable(Level.INFO);
    }

    @Override
    public boolean isWarningLevelEnabled() {
        return this.logger.isLoggable(Level.WARNING);
    }

    @Override
    protected void logDelegatedDebugMessage(String message, Throwable throwable) {
        String[] names = getCallingClassAndMethodNames();
        this.logger.logp(Level.FINE, names[0], names[1], message, throwable);
    }

    @Override
    protected void logDelegatedErrorMessage(String message, Throwable throwable) {
        String[] names = getCallingClassAndMethodNames();
        this.logger.logp(Level.SEVERE, names[0], names[1], message, throwable);
    }

    @Override
    protected void logDelegatedFatalMessage(String message, Throwable throwable) {
        String[] names = getCallingClassAndMethodNames();
        this.logger.logp(Level.SEVERE, names[0], names[1], message, throwable);
    }

    @Override
    protected void logDelegatedInfoMessage(String message, Throwable throwable) {
        String[] names = getCallingClassAndMethodNames();
        this.logger.logp(Level.INFO, names[0], names[1], message, throwable);
    }

    @Override
    protected void logDelegatedWarningMessage(String message, Throwable throwable) {
        String[] names = getCallingClassAndMethodNames();
        this.logger.logp(Level.WARNING, names[0], names[1], message, throwable);
    }

    private String[] getCallingClassAndMethodNames() {
        StackTraceElement stackTraceElement = getCallingStackTraceElement();
        if (stackTraceElement == null) {
            return new String[]{"unknown", "unknown"};
        }
        return new String[]{stackTraceElement.getClassName(), stackTraceElement.getMethodName()};
    }

    private StackTraceElement getCallingStackTraceElement() {
        Throwable throwable = new Throwable();
        throwable.fillInStackTrace();
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        boolean loggingClassNameFound = false;
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            if (stackTraceElement.getClassName().equals(getLoggingClassName())) {
                loggingClassNameFound = true;
                continue;
            }
            if (loggingClassNameFound) {
                return stackTraceElement;
            }
        }
        return null;
    }

    @Override
    public boolean isManaged() {
        return true;
    }

    @Override
    public void setToDebugLevel() {
        this.logger.setLevel(Level.FINE);
        this.currentLogLevel = LogLevel.DEBUG;
    }

    @Override
    public void setToErrorLevel() {
        this.logger.setLevel(Level.SEVERE);
        this.currentLogLevel = LogLevel.ERROR;
    }

    @Override
    public void setToFatalLevel() {
        this.logger.setLevel(Level.SEVERE);
        this.currentLogLevel = LogLevel.FATAL;
    }

    @Override
    public void setToInfoLevel() {
        this.logger.setLevel(Level.INFO);
        this.currentLogLevel = LogLevel.INFO;
    }

    @Override
    public void setToWarningLevel() {
        this.logger.setLevel(Level.WARNING);
        this.currentLogLevel = LogLevel.WARNING;
    }

    /**
     * Gives all currently known logger names.
     *
     * @return All currently known logger names.
     */
    public static List<String> getCurrentLoggerNames() {
        Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
        return Collections.list(loggerNames);
    }
}
