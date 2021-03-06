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

/**
 * Interface for all wrapper classes that wrap a logging system.
 *
 * @author Mark Holster
 */
public interface LogWrapper {

    /**
     * The available log levels.
     *
     * @author Mark Holster
     */
    enum LogLevel {
        /**
         * The debug level.
         */
        DEBUG,
        /**
         * The info level.
         */
        INFO,
        /**
         * The warning level.
         */
        WARNING,
        /**
         * The error level.
         */
        ERROR,
        /**
         * The fatal level.
         */
        FATAL
    }

    ;

    /**
     * Set the class that is wrapping the logging system. This name is used to determine the place of the
     * logging statement in the code. If you're wrapping the logging system, you should call this method
     * with your wrapping class as argument.
     *
     * @param clazz The class that is wrapping the logging system.
     */
    void setLogWrapperClass(Class<?> clazz);

    /**
     * Gives the name of the logger.
     *
     * @return The name of the logger.
     */
    String getName();

    /**
     * Log a message at debug level.
     *
     * @param message The message to log.
     */
    void logDebugMessage(String message);

    /**
     * Log a stacktrace of a throwable and a corresponding message at debug level.
     *
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    void logDebugMessage(String message, Throwable throwable);

    /**
     * Determines whether the debug logging level is enabled.
     *
     * @return <code>true</code> when the debug logging level (or higher) is enabled, <code>false</code>
     * otherwise.
     */
    boolean isDebugLevelEnabled();

    /**
     * Log a message at info level.
     *
     * @param message The message to log.
     */
    void logInfoMessage(String message);

    /**
     * Log a stacktrace of a throwable and a corresponding message at info level.
     *
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    void logInfoMessage(String message, Throwable throwable);

    /**
     * Determines whether the info logging level is enabled.
     *
     * @return <code>true</code> when the info logging level (or higher) is enabled, <code>false</code>
     * otherwise.
     */
    boolean isInfoLevelEnabled();

    /**
     * Log a message at warning level.
     *
     * @param message The message to log.
     */
    void logWarningMessage(String message);

    /**
     * Log a stacktrace of a throwable and a corresponding message at warning level.
     *
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    void logWarningMessage(String message, Throwable throwable);

    /**
     * Determines whether the warning logging level is enabled.
     *
     * @return <code>true</code> when the warning logging level (or higher) is enabled, <code>false</code>
     * otherwise.
     */
    boolean isWarningLevelEnabled();

    /**
     * Log a message at error level.
     *
     * @param message The message to log.
     */
    void logErrorMessage(String message);

    /**
     * Log a stacktrace of a throwable and a corresponding message at error level.
     *
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    void logErrorMessage(String message, Throwable throwable);

    /**
     * Determines whether the error logging level is enabled.
     *
     * @return <code>true</code> when the error logging level (or higher) is enabled, <code>false</code>
     * otherwise.
     */
    boolean isErrorLevelEnabled();

    /**
     * Log a message at fatal level.
     *
     * @param message The message to log.
     */
    void logFatalMessage(String message);

    /**
     * Log a stacktrace of a throwable and a corresponding message at fatal level.
     *
     * @param message   The message to log.
     * @param throwable The throwable to log.
     */
    void logFatalMessage(String message, Throwable throwable);

    /**
     * Determines whether the fatal logging level is enabled.
     *
     * @return <code>true</code> when the fatal logging level is enabled, <code>false</code> otherwise.
     */
    boolean isFatalLevelEnabled();

    /**
     * Boolean indicating this logger is managed. On the contrary to unmanaged loggers, managed loggers can
     * set their log levels programmatically.
     *
     * @return <code>true</code> when this is a managed logger, <code>false</code> otherwise.
     */
    boolean isManaged();

    /**
     * Set the logging level to a given level. Works only on managed loggers. Unmanaged logger should ignore
     * this method call.
     *
     * @param logLevel The log level to set.
     */
    void setToLevel(LogLevel logLevel);

    /**
     * Set the logging level to debug. Works only on managed loggers. Unmanaged logger should ignore this
     * method call.
     */
    void setToDebugLevel();

    /**
     * Set the logging level to info. Works only on managed loggers. Unmanaged logger should ignore this
     * method call.
     */
    void setToInfoLevel();

    /**
     * Set the logging level to warning. Works only on managed loggers. Unmanaged logger should ignore this
     * method call.
     */
    void setToWarningLevel();

    /**
     * Set the logging level to error. Works only on managed loggers. Unmanaged logger should ignore this
     * method call.
     */
    void setToErrorLevel();

    /**
     * Set the logging level to fatal. Works only on managed loggers. Unmanaged logger should ignore this
     * method call.
     */
    void setToFatalLevel();

}
