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
    private static final String MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String MILLI_PATTERN_WITH_TZ = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * Time stamp format for time stamp down to milliseconds
     */
    @Deprecated
    public static final DateTimeFormatter MILLI_FORMAT = DateTimeFormatter.ofPattern(MILLI_PATTERN).withZone(zone);

    /**
     * Time stamp format for time stamp down to milliseconds, with time zone
     */
    public static final DateTimeFormatter MILLI_FORMAT_WITH_TZ = DateTimeFormatter.ofPattern(MILLI_PATTERN_WITH_TZ).withZone(zone);


    private static final DateTimeFormatter absolute_parsers[] = new DateTimeFormatter[]{
            TimestampFormats.MILLI_FORMAT,
            TimestampFormats.MILLI_FORMAT_WITH_TZ
    };

    /**
     * Try to parse text as absolute date, time
     *
     * @param text Text with date, time
     * @return {@link Instant} or <code>null</code>
     */
    public static Instant parse(final String text) {
        for (DateTimeFormatter format : absolute_parsers) {
            try {
                return Instant.from(format.parse(text));
            } catch (Throwable ex) {
                // Ignore;
            }
        }
        return null;
    }
}
