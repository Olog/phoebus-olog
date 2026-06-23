package org.phoebus.olog.entity;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LogBuilderTest {
    @Test
    public void testBuilder(){
        Log testLog = new Log();

        testLog.setDescription(null);
        testLog.setDescription("desc");
        testLog.setSource(null);
        testLog.setSource("src");


        Log.LogBuilder testLogBuilder = new Log.LogBuilder(testLog);

        testLogBuilder.description(null);
        testLogBuilder.description("desc");
        testLogBuilder.source(null);
        testLogBuilder.source("src");
        testLogBuilder.title(null);
    }

    @Test
    public void testNullTitle(){
        Log testLog = new Log();
        assertThrows(NullPointerException.class, () -> new Log.LogBuilder(testLog));
    }
}
