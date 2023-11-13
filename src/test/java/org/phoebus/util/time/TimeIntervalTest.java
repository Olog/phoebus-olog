/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 *
 * @author carcassi
 */
class TimeIntervalTest {

    @Test
    void interval1() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0));
        assertThat(interval.getStart(), equalTo(Instant.ofEpochSecond(0, 0)));
        assertThat(interval.getEnd(), equalTo(Instant.ofEpochSecond(3600, 0)));
    }

    @Test
    void interval2() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(3600, 0), Instant.ofEpochSecond(7200, 0));
        assertThat(interval.getStart(), equalTo(Instant.ofEpochSecond(3600, 0)));
        assertThat(interval.getEnd(), equalTo(Instant.ofEpochSecond(7200, 0)));
    }

    @Test
    void interval3() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), null);
        assertThat(interval.getStart(), equalTo(Instant.ofEpochSecond(0, 0)));
        assertThat(interval.getEnd(), nullValue());
    }

    @Test
    void interval4() {
        TimeInterval interval = TimeInterval.between(null, Instant.ofEpochSecond(0, 0));
        assertThat(interval.getStart(), nullValue());
        assertThat(interval.getEnd(), equalTo(Instant.ofEpochSecond(0, 0)));
    }

    @Test
    void equals1() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0));
        assertThat(interval, equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0))));
    }

    @Test
    void equals2() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 1), Instant.ofEpochSecond(3600, 0));
        assertThat(interval, not(equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0)))));
    }

    @Test
    void equals3() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 1));
        assertThat(interval, not(equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 0)))));
    }

    @Test
    void equals4() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), null);
        assertThat(interval, equalTo(TimeInterval.between(Instant.ofEpochSecond(0, 0), null)));
    }

    @Test
    void equals5() {
        TimeInterval interval = TimeInterval.between(null, Instant.ofEpochSecond(0, 0));
        assertThat(interval, equalTo(TimeInterval.between(null, Instant.ofEpochSecond(0, 0))));
    }

    @Test
    void contains1() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), Instant.ofEpochSecond(3600, 1));
        assertThat(interval.contains(Instant.ofEpochSecond(3,0)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(0,110)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(3600,0)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(-1,110)), is(false));
        assertThat(interval.contains(Instant.ofEpochSecond(3600,2)), is(false));
    }

    @Test
    void contains2() {
        TimeInterval interval = TimeInterval.between(Instant.ofEpochSecond(0, 0), null);
        assertThat(interval.contains(Instant.ofEpochSecond(-3600,2)), is(false));
        assertThat(interval.contains(Instant.ofEpochSecond(-1,110)), is(false));
        assertThat(interval.contains(Instant.ofEpochSecond(0,110)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(3,0)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(3600,0)), is(true));
    }

    @Test
    void contains3() {
        TimeInterval interval = TimeInterval.between(null, Instant.ofEpochSecond(0, 0));
        assertThat(interval.contains(Instant.ofEpochSecond(-3600,2)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(-1,110)), is(true));
        assertThat(interval.contains(Instant.ofEpochSecond(0,110)), is(false));
        assertThat(interval.contains(Instant.ofEpochSecond(3,0)), is(false));
        assertThat(interval.contains(Instant.ofEpochSecond(3600,0)), is(false));
    }
}