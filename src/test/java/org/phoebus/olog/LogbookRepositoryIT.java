package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
public class LogbookRepositoryIT {

    // Read the elastic index and type from the application.properties
    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    private String ES_LOGBOOK_INDEX;

    @Autowired
    private LogbookRepository logbookRepository;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    private Logbook testLogbook1 = new Logbook("test-logbook-1", testOwner, State.Active);
    private Logbook testLogbook2 = new Logbook("test-logbook-2", testOwner, State.Active);
    private Logbook testLogbook3 = new Logbook("test-logbook-3", testOwner, State.Active);
    private Logbook testLogbook4 = new Logbook("test-logbook-4", testOwner, State.Active);

    @BeforeClass
    public static void setup() {

    }

    @AfterClass
    public static void cleanup() {

    }

    private static final String testOwner = "test-owner";

    /**
     * Test the creation of a test logbook
     */
    @Test
    public void createLogbook() {
        try {
            logbookRepository.save(testLogbook1);
            Optional<Logbook> result = logbookRepository.findById(testLogbook1.getName());
            assertThat("Failed to create Logbook " + testLogbook1, result.isPresent() && result.get().equals(testLogbook1));
        } finally {
            cleanupLogbook(Arrays.asList(testLogbook1));
        }
    }

    /**
     * create a set of logbooks
     */
    @Test
    public void createLogbooks() {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        try {
            List<Logbook> result = new ArrayList<Logbook>();
            logbookRepository.saveAll(logbooks).forEach(logbook -> {
                result.add(logbook);
            });
            assertThat("Failed to create all logbooks", result.containsAll(logbooks));

            List<Logbook> findAll = new ArrayList<Logbook>();
            logbookRepository.findAll().forEach(logbook -> {
                findAll.add(logbook);
            });
            assertThat("Failed to list all logbooks", findAll.containsAll(logbooks));
        } finally {
            cleanupLogbook(logbooks);
        }
    }


    /**
     * Test the deletion of a logbook
     */
    @Test
    public void deleteLogbook(){
        try {
            logbookRepository.save(testLogbook2);
            Optional<Logbook> result = logbookRepository.findById(testLogbook2.getName());
            assertThat("Failed to create Logbook " + testLogbook2,
                    result.isPresent() && result.get().equals(testLogbook2));

            logbookRepository.delete(testLogbook2);
            result = logbookRepository.findById(testLogbook2.getName());
            Logbook expectedLogbook = new Logbook("test-logbook-2", testOwner, State.Inactive);
            assertThat("Failed to delete Logbook", result.isPresent() && result.get().equals(expectedLogbook));
        } finally {
            cleanupLogbook(Arrays.asList(testLogbook2));
        }
    }

    /**
     * Delete a set of logbooks
     */
    @Test
    public void deteleLogbooks(){
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        try {
            List<Logbook> result = new ArrayList<Logbook>();
            logbookRepository.saveAll(logbooks).forEach(logbook -> {
                result.add(logbook);
            });

            logbookRepository.deleteAll(logbooks);
            List<Logbook> inactiveTags = new ArrayList<Logbook>();
            logbookRepository.findAllById(logbooks.stream().map(Logbook::getName).collect(Collectors.toList())).forEach(logbook -> {
                if (logbook.getState().equals(State.Inactive)) {
                    inactiveTags.add(logbook);
                }
            });
            assertThat("Failed to delete multiple tags ", inactiveTags.containsAll(logbooks));
        } finally {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    /**
     * Find all logbooks that are still Active
     */
    @Test
    public void findAllLogbooks(){
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        try {
            logbookRepository.saveAll(logbooks);
            List<Logbook> findAll = new ArrayList<Logbook>();
            logbookRepository.findAll().forEach(logbook -> {
                findAll.add(logbook);
            });
            assertThat("Failed to list all logbooks", findAll.containsAll(logbooks));
        } finally {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    /**
     * Find a logbook with the given Id, the logbook is found irrespective of its State
     */
    @Test
    public void findLogbooksById(){
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2);
        try {
            logbookRepository.saveAll(logbooks);
            assertTrue("Failed to find by index logbook: " + testLogbook1,
                    testLogbook1.equals(logbookRepository.findById(testLogbook1.getName()).get()));
            assertTrue("Failed to find by index logbook: " + testLogbook2,
                    testLogbook2.equals(logbookRepository.findById(testLogbook2.getName()).get()));
        } finally {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    /**
     * Find logbooks with the given Ids, the logbooks are found irrespective of its State
     */
    @Test
    public void findAllLogbooksByIds(){
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        try {
            logbookRepository.saveAll(logbooks);

            List<Logbook> findAllById = new ArrayList<Logbook>();
            logbookRepository.findAllById(Arrays.asList("test-logbook-1", "test-logbook-2"))
                    .forEach(logbook -> {
                        findAllById.add(logbook);
                    });
            assertTrue("Failed to search by id test-logbook-1 and test-logbook-2 ",
                    findAllById.size() == 2 && findAllById.contains(testLogbook1) && findAllById.contains(testLogbook2));
        } finally {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    @Test
    public void checkLogbookExist(){
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2);
        try {
            logbookRepository.saveAll(logbooks);

            assertTrue("Failed to check if exists logbook: " + testLogbook1, logbookRepository.existsById(testLogbook1.getName()));
            assertTrue("Failed to check if exists logbook: " + testLogbook2, logbookRepository.existsById(testLogbook2.getName()));

            assertFalse("Failed to check if exists logbook: non-existant-logbook",
                    logbookRepository.existsById("non-existant-logbook"));
        } finally {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    @Test
    public void checkLogbooksExist() {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2);
        try {
            logbookRepository.saveAll(logbooks);

            assertTrue("Failed to check if logbooks : " + testLogbook1 + ", " + testLogbook2 + " exist",
                    logbookRepository.existsByIds(Arrays.asList(testLogbook1.getName(), testLogbook2.getName())));

            assertFalse("When any one of the requested ids does not exist the expected result is false",
                    logbookRepository.existsByIds(Arrays.asList(testLogbook1.getName(), testLogbook3.getName())));

        } finally {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }


    /**
     * Cleanup up the logbooks
     *
     * @param logbooks
     */
    void cleanupLogbook(List<Logbook> logbooks) {
        List<BulkOperation> bulkOperations = new ArrayList<>();
        logbooks.forEach(logbook -> bulkOperations.add(DeleteOperation.of(i ->
                i.index(ES_LOGBOOK_INDEX).id(logbook.getName()))._toBulkOperation()));
        BulkRequest bulkRequest =
                BulkRequest.of(r ->
                        r.operations(bulkOperations).refresh(Refresh.True));
        try {
            client.bulk(bulkRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
