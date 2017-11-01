package edu.msu.nscl.olog;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.LongStream;

import org.junit.Test;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Test the creation of a unique integer id
 * 
 * @author Kunal Shroff
 *
 */
public class LogSequenceIT {

    /**
     * 
     */
    @Test
    public void checkMultithreadedIdCreation() {
        long initialId = SequenceGenerator.getID();
        Set<Long> expectedIds = Sets.newHashSet(LongStream.rangeClosed(initialId + 1, initialId + 5000).iterator());
        ExecutorService executor = Executors.newFixedThreadPool(50);
        Collection<Future<Long>> tasks = new ArrayList<Future<Long>>(5000);
        for (Long l : expectedIds) {
            tasks.add(executor.submit(() -> {
                return SequenceGenerator.getID();
            }));
        }
        assertTrue("failed to create all the requests", tasks.size() == 5000);
        Set<Long> resultIds = new HashSet<Long>(5000);
        for (Future<Long> result : tasks) {
            try {
                resultIds.add(result.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
        assertTrue("failed to create all the requests", resultIds.size() == 5000);
        assertTrue("failed to create all the requests", resultIds.containsAll(expectedIds));
    }

}
