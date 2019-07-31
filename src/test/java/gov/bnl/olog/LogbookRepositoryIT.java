package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_LOGBOOK_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOGBOOK_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import gov.bnl.olog.Config;
import gov.bnl.olog.LogbookRepository;
import gov.bnl.olog.entity.Logbook;
import gov.bnl.olog.entity.State;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Config.class)
public class LogbookRepositoryIT {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;
    @Autowired
    private LogbookRepository logbookRepository;

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
        Logbook testLogbook = new Logbook("test-logbook-1", testOwner, State.Active);
        logbookRepository.index(testLogbook);
        Optional<Logbook> result = logbookRepository.findById(testLogbook.getName());
        assertThat("Failed to create Logbook " + testLogbook, result.isPresent() && result.get().equals(testLogbook));

        // Manual cleanup since Olog does not delete things
        elasticsearchTemplate.getClient().prepareDelete(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, testLogbook.getName()).get("10s");
    }

    /**
     * Test the deletion of a test logbook
     */
    @Test
    public void deleteLogbook() {
        Logbook testLogbook = new Logbook("test-logbook-2", testOwner, State.Active);
        logbookRepository.index(testLogbook);
        Optional<Logbook> result = logbookRepository.findById(testLogbook.getName());
        assertThat("Failed to create Logbook " + testLogbook, result.isPresent() && result.get().equals(testLogbook));

        logbookRepository.delete(testLogbook);
        result = logbookRepository.findById(testLogbook.getName());
        testLogbook.setState(State.Inactive);
        assertThat("Failed to delete Logbook", result.isPresent() && result.get().equals(testLogbook));

        // Manual cleanup since Olog does not delete things
        elasticsearchTemplate.getClient().prepareDelete(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, testLogbook.getName()).get("10s");
    }

    /**
     * create a set of logbooks
     */
    @Test
    public void createLogbooks() {
        Logbook testLogbook1 = new Logbook("test-logbook-1", testOwner, State.Active);
        Logbook testLogbook2 = new Logbook("test-logbook-2", testOwner, State.Active);
        Logbook testLogbook3 = new Logbook("test-logbook-3", testOwner, State.Active);
        Logbook testLogbook4 = new Logbook("test-logbook-4", testOwner, State.Active);
        List<Logbook> logbooks = Arrays.asList(testLogbook1, testLogbook2, testLogbook3, testLogbook4);
        List<Logbook> result = new ArrayList<Logbook>();
        logbookRepository.saveAll(logbooks).forEach(logbook -> {
            result.add(logbook);
        });
        assertThat("Failed to create all logbooks", result.containsAll(logbooks));

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<Logbook> findAll = new ArrayList<Logbook>();
        logbookRepository.findAll().forEach(logbook -> {
            findAll.add(logbook);
        });
        assertThat("Failed to list all logbooks", findAll.containsAll(logbooks));

        // Manual cleanup since Olog does not delete things
        BulkRequestBuilder bulk = elasticsearchTemplate.getClient().prepareBulk();
        logbooks.forEach(logbook -> {
            bulk.add(elasticsearchTemplate.getClient().prepareDelete(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, logbook.getName()));
        });
        bulk.get("10s");
    }

}
