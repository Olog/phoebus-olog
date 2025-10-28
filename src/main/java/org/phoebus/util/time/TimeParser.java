/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.ChronoUnit.YEARS;

/**
 * A helper class to parse user defined time strings to absolute or relative
 * time durations.
 *
 * @author shroffk
 */
@SuppressWarnings("nls")
public class TimeParser {
    /** Text for the relative {@link TemporalAmount} of size 0 */
    public static final String NOW = "now";


    // SPACE*  (NUMBER?) SPACE*  (UNIT),
    // with NUMBER being positive floating point
    // Patterns need to be listed longest-first.
    // Otherwise "days" would match just the "d"
    static final Pattern timeQuantityUnitsPattern = Pattern.compile(
            "\\s*([0-9]*\\.?[0-9]*)\\s*(millis|ms|seconds|second|secs|sec|s|minutes|minute|mins|min|hours|hour|h|days|day|d|weeks|week|w|months|month|mon|mo|years|year|y)\\s*",
            Pattern.CASE_INSENSITIVE);

    /** Parse a temporal amount like "1 month 2 days" or "1 day 20 seconds"
     *
     *  <p>Provides either a time-based {@link Duration}
     *  or a calendar based {@link Period}.
     *
     *  <p>A period of "1 months" does not have a well defined
     *  length in time because a months could have 28 to 31 days.
     *  When the user specifies "1 month" we assume that
     *  a time span between the same day in different months
     *  is requested.
     *  As soon as the time span includes a month or year,
     *  a {@link Period} is returned and the smaller units
     *  from hours down are ignored.
     *
     *  <p>For time spans that only include days or less,
     *  a {@link Duration} is used.
     *
     *  @param string Text
     *  @return {@link Duration} or {@link Period}
     */
    public static TemporalAmount parseTemporalAmount(final String string)
    {
        if (NOW.equalsIgnoreCase(string))
            return Duration.ZERO;
        final Matcher timeQuantityUnitsMatcher = timeQuantityUnitsPattern.matcher(string);
        final Map<ChronoUnit, Integer> timeQuantities = new HashMap<>();

        boolean use_period = false;
        while (timeQuantityUnitsMatcher.find())
        {
            final double quantity = "".equals(timeQuantityUnitsMatcher.group(1))
                    ? 1.0
                    : Double.valueOf(timeQuantityUnitsMatcher.group(1));
            final int full = (int) quantity;
            final double fraction = quantity - full;
            final String unit = timeQuantityUnitsMatcher.group(2).toLowerCase();
            // Collect the YEARS, .., DAYS, .., MINUTES, .. as used by Period or Duration.
            // Problem 1: Need to eventually pick either Period or Duration.
            //            -> We go up to Period when WEEKS or larger are involved.
            // Problem 2: They only take full amounts.
            //            -> We place fractional amounts in the next finer unit.
            if (unit.startsWith("y"))
            {
                timeQuantities.put(YEARS, full);
                if (fraction > 0)
                {
                    final int next = (int) (fraction * 12 + 0.5);
                    timeQuantities.compute(MONTHS, (u, prev) -> prev == null ? next : prev + next);
                }
                use_period = true;
            }
            else if (unit.startsWith("mo"))
            {
                timeQuantities.compute(MONTHS, (u, prev) -> prev == null ? full : prev + full);
                if (fraction > 0)
                {
                    final int next = (int) (fraction * 4*7 + 0.5);
                    timeQuantities.compute(DAYS,  (u, prev) -> prev == null ? next : prev + next);
                }
                use_period = true;
            }
            else if (unit.startsWith("w"))
            {
                timeQuantities.compute(WEEKS, (u, prev) -> prev == null ? full : prev + full);
                if (fraction > 0)
                {
                    final int next = (int) (fraction * 7 + 0.5);
                    timeQuantities.compute(DAYS, (u, prev) -> prev == null ? next : prev + next);
                }
                use_period = true;
            }
            else if (unit.startsWith("mi"))
            {
                timeQuantities.compute(MINUTES, (u, prev) -> prev == null ? full : prev + full);
                if (fraction > 0)
                {
                    final int next = (int) (fraction * 60 + 0.5);
                    timeQuantities.compute(SECONDS, (u, prev) -> prev == null ? next : prev + next);
                }
            }
            else if (unit.startsWith("h"))
            {
                timeQuantities.compute(HOURS, (u, prev) -> prev == null ? full : prev + full);
                if (fraction > 0)
                {
                    final int next = (int) (fraction * 60 + 0.5);
                    timeQuantities.compute(MINUTES, (u, prev) -> prev == null ? next : prev + next);
                }
            }
            else if (unit.startsWith("d"))
            {
                timeQuantities.compute(DAYS, (u, prev) -> prev == null ? full : prev + full);
                if (fraction > 0)
                {
                    final int next = (int) (fraction * 24 + 0.5);
                    timeQuantities.compute(HOURS, (u, prev) -> prev == null ? next : prev + next);
                }
            }
            else if (unit.startsWith("s"))
            {
                timeQuantities.compute(SECONDS, (u, prev) -> prev == null ? full : prev + full);
                if (fraction > 0)
                {
                    final int next = (int) (fraction * 1000 + 0.5);
                    timeQuantities.compute(MILLIS, (u, prev) -> prev == null ? next : prev + next);
                }
            }
            else if (unit.equals("ms")) {
                timeQuantities.compute(MILLIS, (u, prev) -> prev == null ? full : prev + full);
                if (fraction > 0)
                {
                    final int next = (int) (fraction * 1000 + 0.5);
                    timeQuantities.compute(MICROS, (u, prev) -> prev == null ? next : prev + next);
                }
            }
        }

        if (use_period)
        {
            Period result = Period.ZERO;
            if (timeQuantities.containsKey(YEARS))
                result = result.plusYears(timeQuantities.get(YEARS));
            if (timeQuantities.containsKey(WEEKS))
                result = result.plusDays(7*timeQuantities.get(WEEKS));
            if (timeQuantities.containsKey(MONTHS))
                result = result.plusMonths(timeQuantities.get(MONTHS));
            if (timeQuantities.containsKey(DAYS))
                result = result.plusDays(timeQuantities.get(DAYS));
            // Ignoring hours, min, .. because they're insignificant compared to weeks
            return result;
        }
        else
        {
            Duration result = Duration.ofSeconds(0);
            for (Entry<ChronoUnit, Integer> entry : timeQuantities.entrySet())
                result = result.plus(entry.getValue(), entry.getKey());
            return result;
        }
    }

    /** Format a temporal amount
     *
     *  @param amount {@link TemporalAmount}
     *  @return Text like "2 days" that {@link #parseTemporalAmount(String)} can parse
     */
    public static String format(final TemporalAmount amount)
    {
        final StringBuilder buf = new StringBuilder();
        if (amount instanceof Period)
        {
            final Period period = (Period) amount;
            if (period.isZero())
                return NOW;

            if (period.getYears() == 1)
                buf.append("1 year ");
            else if (period.getYears() > 1)
                buf.append(period.getYears()).append(" years ");

            if (period.getMonths() == 1)
                buf.append("1 month ");
            else if (period.getMonths() > 0)
                buf.append(period.getMonths()).append(" months ");

            if (period.getDays() == 1)
                buf.append("1 day");
            else if (period.getDays() > 0)
                buf.append(period.getDays()).append(" days");
        }
        else
        {
            long secs = ((Duration) amount).getSeconds();
            if (secs == 0)
                return NOW;

            int p = (int) (secs / (24*60*60));
            if (p > 0)
            {
                if (p == 1)
                    buf.append("1 day ");
                else
                    buf.append(p).append(" days ");
                secs -= p * (24*60*60);
            }

            p = (int) (secs / (60*60));
            if (p > 0)
            {
                if (p == 1)
                    buf.append("1 hour ");
                else
                    buf.append(p).append(" hours ");
                secs -= p * (60*60);
            }

            p = (int) (secs / (60));
            if (p > 0)
            {
                if (p == 1)
                    buf.append("1 minute ");
                else
                    buf.append(p).append(" minutes ");
                secs -= p * (60);
            }

            if (secs > 0)
                if (secs == 1)
                    buf.append("1 second ");
                else
                    buf.append(secs).append(" seconds ");

            final int ms = ((Duration)amount).getNano() / 1000000;
            if (ms > 0)
                buf.append(ms).append(" ms");
        }
        return buf.toString().trim();
    }

    /** Try to parse text as absolute or relative time
     *  @param text The text to parse
     *  @param zoneId The {@link ZoneId} for which the date/time string shall be evaluated.
     *  @return {@link Instant}, {@link TemporalAmount} or <code>null</code>
     */
    public static Object parseInstantOrTemporalAmount(final String text, ZoneId zoneId)
    {
        Object result = TimestampFormats.parse(text, zoneId);
        if (result != null)
            return result;
        return parseTemporalAmount(text);
    }
}
