package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = AuthenticationResource.class)
@ContextConfiguration(classes = {LogbookRepository.class, ElasticConfig.class})
@TestPropertySource(locations = "classpath:test_application.properties")
class LogbookRepositoryIT {

    // Read the elastic index and type from the application.properties
    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    private String ES_LOGBOOK_INDEX;

    @Autowired
    private LogbookRepository logbookRepository;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    private final Logbook testLogbook1 = new Logbook("test-logbook-1", testOwner, State.Active);
    private final Logbook testLogbook2 = new Logbook("test-logbook-2", testOwner, State.Active);
    private final Logbook testLogbook3 = new Logbook("test-logbook-3", testOwner, State.Active);
    private final Logbook testLogbook4 = new Logbook("test-logbook-4", testOwner, State.Active);

    private static final String testOwner = "test-owner";

    /**
     * Test the creation of a test logbook
     */
    @Test
    void createLogbook() {
        try {
            logbookRepository.save(testLogbook1);
            Optional<Logbook> result = logbookRepository.findById(testLogbook1.getName());
            assertThat("Failed to create Logbook " + testLogbook1, result.isPresent() && result.get().equals(testLogbook1));
        } finally {
            cleanupLogbook(List.of(testLogbook1));
        }
    }

    /**
     * create a set of logbooks
     */
    @Test
    void createLogbooks() {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        try {
            List<Logbook> result = new ArrayList<>();
            logbookRepository.saveAll(logbooks).forEach(result::add);
            assertThat("Failed to create all logbooks", result.containsAll(logbooks));

            List<Logbook> findAll = new ArrayList<>();
            logbookRepository.findAll().forEach(logbook -> findAll.add(logbook));
            assertThat("Failed to list all logbooks", findAll.containsAll(logbooks));
        } finally {
            cleanupLogbook(logbooks);
        }
    }


    /**
     * Test the deletion of a logbook
     */
    @Test
    void deleteLogbook() {
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
            cleanupLogbook(List.of(testLogbook2));
        }
    }

    /**
     * Delete a set of logbooks
     */
    @Test
    void deteleLogbooks() {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        try {
            List<Logbook> result = new ArrayList<>();
            logbookRepository.saveAll(logbooks).forEach(result::add);

            logbookRepository.deleteAll(logbooks);
            List<Logbook> inactiveTags = new ArrayList<>();
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
    void findAllLogbooks() {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        try {
            logbookRepository.saveAll(logbooks);
            List<Logbook> findAll = new ArrayList<>();
            logbookRepository.findAll().forEach(findAll::add);
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
    void findLogbooksById() {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2);
        try {
            logbookRepository.saveAll(logbooks);
            assertEquals(testLogbook1, logbookRepository.findById(testLogbook1.getName()).get(), "Failed to find by index logbook: " + testLogbook1);
            assertEquals(testLogbook2, logbookRepository.findById(testLogbook2.getName()).get(), "Failed to find by index logbook: " + testLogbook2);
        } finally {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    /**
     * Find logbooks with the given Ids, the logbooks are found irrespective of its State
     */
    @Test
    void findAllLogbooksByIds() {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        try {
            logbookRepository.saveAll(logbooks);

            List<Logbook> findAllById = new ArrayList<>();
            logbookRepository.findAllById(Arrays.asList("test-logbook-1", "test-logbook-2"))
                    .forEach(findAllById::add);
            assertTrue(
                    findAllById.size() == 2 && findAllById.contains(testLogbook1) && findAllById.contains(testLogbook2),
                    "Failed to search by id test-logbook-1 and test-logbook-2 ");
        } finally {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    @Test
    void checkLogbookExist() {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2);
        try {
            logbookRepository.saveAll(logbooks);

            assertTrue(logbookRepository.existsById(testLogbook1.getName()), "Failed to check if exists logbook: " + testLogbook1);
            assertTrue(logbookRepository.existsById(testLogbook2.getName()), "Failed to check if exists logbook: " + testLogbook2);

            assertFalse(
                    logbookRepository.existsById("non-existant-logbook"),
                    "Failed to check if exists logbook: non-existant-logbook");
        } finally {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    @Test
    void checkLogbooksExist() {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2);
        try {
            logbookRepository.saveAll(logbooks);

            assertTrue(
                    logbookRepository.existsByIds(Arrays.asList(testLogbook1.getName(), testLogbook2.getName())),
                    "Failed to check if logbooks : " + testLogbook1 + ", " + testLogbook2 + " exist");

            assertFalse(
                    logbookRepository.existsByIds(Arrays.asList(testLogbook1.getName(), testLogbook3.getName())),
                    "When any one of the requested ids does not exist the expected result is false");

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
