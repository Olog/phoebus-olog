package gov.bnl.olog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import gov.bnl.olog.entity.Logbook;
import gov.bnl.olog.entity.State;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations="classpath:test_application.properties")
public class LogbookRepositoryIT {

    // Read the elatic index and type from the application.properties
    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    private String ES_LOGBOOK_INDEX;
    @Value("${elasticsearch.logbook.type:olog_logbook}")
    private String ES_LOGBOOK_TYPE;

    @Autowired
    private LogbookRepository logbookRepository;

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
     * @throws IOException 
     */
    @Test
    public void createLogbook() throws IOException
    {
        try
        {
            logbookRepository.save(testLogbook1);
            Optional<Logbook> result = logbookRepository.findById(testLogbook1.getName());
            assertThat("Failed to create Logbook " + testLogbook1, result.isPresent() && result.get().equals(testLogbook1));
        } finally
        {
            cleanupLogbook(Arrays.asList(testLogbook1));
        }
    }

    /**
     * create a set of logbooks
     * @throws IOException 
     */
    @Test
    public void createLogbooks() throws IOException
    {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        try
        {
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
        } finally
        {
            cleanupLogbook(logbooks);
        }
    }


    /**
     * Test the deletion of a logbook
     * @throws IOException 
     */
    @Test
    public void deleteLogbook() throws IOException
    {
        try
        {
            logbookRepository.save(testLogbook2);
            Optional<Logbook> result = logbookRepository.findById(testLogbook2.getName());
            assertThat("Failed to create Logbook " + testLogbook2,
                    result.isPresent() && result.get().equals(testLogbook2));

            logbookRepository.delete(testLogbook2);
            result = logbookRepository.findById(testLogbook2.getName());
            Logbook expectedLogbook = new Logbook("test-logbook-2", testOwner, State.Inactive);
            assertThat("Failed to delete Logbook", result.isPresent() && result.get().equals(expectedLogbook));
        } finally
        {
            cleanupLogbook(Arrays.asList(testLogbook2));
        }
    }

    /**
     * Delete a set of logbooks
     * @throws IOException 
     */
    @Test
    public void deteleLogbooks() throws IOException
    {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        try
        {
            List<Logbook> result = new ArrayList<Logbook>();
            logbookRepository.saveAll(logbooks).forEach(logbook -> {
                result.add(logbook);
            });

            logbookRepository.deleteAll(logbooks);
            List<Logbook> inactiveTags = new ArrayList<Logbook>();
            logbookRepository.findAllById(logbooks.stream().map(Logbook::getName).collect(Collectors.toList())).forEach(logbook -> {
                if (logbook.getState().equals(State.Inactive))
                {
                    inactiveTags.add(logbook);
                }
            });
            assertThat("Failed to delete multiple tags ", inactiveTags.containsAll(logbooks));
        } finally
        {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    /**
     * Find all logbooks that are still Active
     * @throws IOException 
     */
    @Test
    public void findAllLogbooks() throws IOException
    {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        try
        {
            logbookRepository.saveAll(logbooks);
            List<Logbook> findAll = new ArrayList<Logbook>();
            logbookRepository.findAll().forEach(logbook -> {
                findAll.add(logbook);
            });
            assertThat("Failed to list all logbooks", findAll.containsAll(logbooks));
        } finally
        {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    /**
     * Find a logbook with the given Id, the logbook is found irrespective of its State
     * @throws IOException 
     */
    @Test
    public void findLogbooksById() throws IOException
    {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2);
        try
        {
            logbookRepository.saveAll(logbooks);
            assertTrue("Failed to find by index logbook: " + testLogbook1,
                    testLogbook1.equals(logbookRepository.findById(testLogbook1.getName()).get()));
            assertTrue("Failed to find by index logbook: " + testLogbook2,
                    testLogbook2.equals(logbookRepository.findById(testLogbook2.getName()).get()));
        } finally
        {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    /**
     * Find logbooks with the given Ids, the logbooks are found irrespective of its State
     * @throws IOException 
     */
    @Test
    public void findAllLogbooksByIds() throws IOException
    {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        try
        {
            logbookRepository.saveAll(logbooks);

            List<Logbook> findAllById = new ArrayList<Logbook>();
            logbookRepository.findAllById(Arrays.asList("test-logbook-1", "test-logbook-2"))
                                        .forEach(logbook -> {
                                            findAllById.add(logbook);
                                        });
            assertTrue("Failed to search by id test-logbook-1 and test-logbook-2 ",
                    findAllById.size() == 2 && findAllById.contains(testLogbook1) && findAllById.contains(testLogbook2));
        } finally
        {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    @Test
    public void checkLogbookExist() throws IOException
    {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2);
        try
        {
            logbookRepository.saveAll(logbooks);

            assertTrue("Failed to check if exists logbook: " + testLogbook1, logbookRepository.existsById(testLogbook1.getName()));
            assertTrue("Failed to check if exists logbook: " + testLogbook2, logbookRepository.existsById(testLogbook2.getName()));

            assertFalse("Failed to check if exists logbook: non-existant-logbook",
                    logbookRepository.existsById("non-existant-logbook"));
        } finally
        {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    @Test
    public void checkLogbooksExist() throws IOException
    {
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2);
        try
        {
            logbookRepository.saveAll(logbooks);

            assertTrue("Failed to check if logbooks : " + testLogbook1 + ", " + testLogbook2 +" exist",
                    logbookRepository.existsByIds(Arrays.asList(testLogbook1.getName(), testLogbook2.getName())));
            
            assertFalse("When any one of the requested ids does not exist the expected result is false",
                    logbookRepository.existsByIds(Arrays.asList(testLogbook1.getName(), testLogbook3.getName())));

        } finally
        {
            // Manual cleanup
            cleanupLogbook(logbooks);
        }
    }

    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient client;

    /**
     * Cleanup up the logbooks
     * @param logbooks
     * @throws IOException 
     */
    void cleanupLogbook(List<Logbook> logbooks) throws IOException
    {
        // Manual cleanup since Olog does not delete things
        BulkRequest bulk = new BulkRequest();
        logbooks.forEach(logbook -> {
            bulk.add(new DeleteRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbook.getName()));
        });
        client.bulk(bulk, RequestOptions.DEFAULT);
    }

}
