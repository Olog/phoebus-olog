package edu.msu.nscl.olog;

import static org.junit.Assert.assertTrue;
import static edu.msu.nscl.olog.ResourceManagerTestSuite.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import edu.msu.nscl.olog.Log.LogBuilder;

public class LogManagerIT {

    @Test
    public void findLogs() {
        List<Log> result = LogManager.listRecentLogs();
        assertTrue("Failed to a get a list of recent log entries", result != null & !result.isEmpty());
        assertTrue("Failed to a get a list of recent log entries", result.containsAll(initialLogs));
    }

    /**
     * A basic test to create a simple tag
     */
    @Test
    public void createLogSimple() {
        Property property = initialProperties.get(0);
        property.getAttributes().stream().forEach((attr) ->{
            attr.setValue("test-val");
        });
        
        Log log = LogBuilder.createLog("A simple log entry")
                .level(Level.Info)
                .owner("test-owner")
                .withLogbooks(new HashSet<>(initialLogbooks))
                .withTags(new HashSet<>(initialTags))
                .withProperty(property)
                .build();
        Log createdLog = LogManager.createLog(log);

    }

}
