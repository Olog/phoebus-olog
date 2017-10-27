package edu.msu.nscl.olog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

public class LogbookManagerIT {

    @Test
    public void findAllLogbooks() {
        List<Logbook> result = LogbookManager.list();
        assertTrue("Failed to list all logbooks ", result.containsAll(ResourceManagerTestSuite.initialLogbooks));
    }

    @Test
    public void findAllActiveLogbooks() {
        List<Logbook> result = LogbookManager.listActive();
        assertTrue("Failed to list all active logbooks ", result.containsAll(ResourceManagerTestSuite.initialLogbooks.stream()
                .filter(t -> t.getState().equals(State.Active)).collect(Collectors.toList())));
    }

    /**
     * A basic test to create a simple logbook
     */
    @Test
    public void createLogbook() {
        Logbook logbook = new Logbook("create-test-logbook1", "test-owner", State.Active);
        Optional<Logbook> result = LogbookManager.createLogbook(logbook);
        if (result.isPresent()) {
            assertEquals("Failed to created a new tag ", logbook, result.get());
            assertNotNull("Failed to create a valid tag - no id detected", result.get());
        } else {
            fail("Failed to create a valid tag");
        }
    }

    /**
     * A basic test to delete a simple logbook, a delete is not absolute but rather a
     * marking as inactive of active tags
     */
    @Test
    public void deleteLogbook() {
        Logbook logbook = new Logbook("delete-test-logbook1", "test-owner", State.Active);
        LogbookManager.createLogbook(logbook);

        Optional<Logbook> deletedLogbook = LogbookManager.deleteLogbook(logbook);

        assertTrue("Failed to properly delete tag 'delete-test-logbook1' ", !LogbookManager.listActive().contains(logbook));
        assertTrue("Failed to properly delete tag 'delete-test-logbook1' ", LogbookManager.list().contains(logbook));
    }

    /**
     * A basic test to find a single logbook by name
     */
    @Test
    public void findTag() {
        for (Logbook logbook : ResourceManagerTestSuite.initialLogbooks) {
            Optional<Logbook> result = LogbookManager.findLogbook(logbook.getName());
            if (result.isPresent()) {
                assertEquals("Failed to find logbook based on name seach ", logbook, result.get());
            } else {
                fail("Failed to find the logbook " + logbook.getName());
            }
        }
    }

}
