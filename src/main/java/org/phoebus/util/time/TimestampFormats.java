/*******************************************************************************
 * Copyright (c) 2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.util.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Time stamp formats
 *
 * <p>Java 8 introduced {@link Instant} which handles time stamps
 * with up to nanosecond detail, obsoleting custom classes
 * for wrapping control system time stamps.
 *
 * <p>The {@link DateTimeFormatter} is immutable and thread-safe,
 * finally allowing re-use of common time stamp formatters.
 *
 * <p>The formatters defined here are suggested for CS-Studio time stamps
 * because they can show the full detail of control system time stamps in a portable way,
 * independent from locale.
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimestampFormats {

    private static final ZoneId zone = ZoneId.systemDefault();
    public static final String MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String SECONDS_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm:ss";

    private static final String[] TIME_FORMATS = new String[]{
            MILLI_PATTERN,
            SECONDS_PATTERN,
            DATETIME_PATTERN,
            DATE_PATTERN,
            TIME_PATTERN
    };

    /**
     * Try to parse text as absolute date, time
     *
     * @param text Text with date, time
     * @param zoneId The {@link ZoneId} for which the date/time string shall be evaluated.
     * @return {@link Instant} or <code>null</code> if the specified date/time string cannot be parsed.
     */
    public static Instant parse(final String text, ZoneId zoneId) {
        for(String format : TIME_FORMATS){
            try {
                return Instant.from(DateTimeFormatter.ofPattern(format).withZone(zoneId).parse(text));
            } catch (Exception e) {
                // Ignore
            }
        }
        return null;
    }
}
