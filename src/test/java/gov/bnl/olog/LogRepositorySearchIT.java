/**
 * 
 */
package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_LOGBOOK_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOGBOOK_TYPE;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_TYPE;
import static gov.bnl.olog.OlogResourceDescriptors.ES_PROPERTY_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_PROPERTY_TYPE;
import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_TYPE;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import gov.bnl.olog.entity.Attribute;
import gov.bnl.olog.entity.Log;
import gov.bnl.olog.entity.Logbook;
import gov.bnl.olog.entity.Property;
import gov.bnl.olog.entity.State;
import gov.bnl.olog.entity.Tag;

/**
 * @author kunal
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(listeners = {LogRepositorySearchIT.class})
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
public class LogRepositorySearchIT  implements TestExecutionListener
{
    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient client;

    private static LogRepository logRepository;

    private static final String testOwner1 = "testOwner1";
    private static final String testOwner2 = "testOwner2";

    private static Logbook testLogbook1 = new Logbook("testLogbook1", testOwner1, State.Active);
    private static Logbook testLogbook2 = new Logbook("testLogbook2", testOwner1, State.Active);
    
    private static Tag testTag1 = new Tag("testTag1", State.Active);
    private static Tag testTag2= new Tag("testTag2", State.Active);

    private static Attribute attribute1 = new Attribute("testAttribute1");
    private static Attribute attribute2 = new Attribute("testAttribute2");

    private static Property testProperty1 = new Property("testProperty1",
                                                         testOwner1,
                                                         State.Active,
                                                         new HashSet<Attribute>(List.of(attribute1, attribute2)));
    

    private static Property testProperty2 = new Property("testProperty2",
                                                         testOwner1,
                                                         State.Active,
                                                         new HashSet<Attribute>(List.of(attribute2)));

    /**
     * Search for a particular keyword
     */
    @Test
    public void searchByKeyword()
    {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.put("desc", List.of("quick"));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on a single keyword.",
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));

        searchParameters.put("desc", List.of("jumped"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on a single keyword.",
                foundLogs.size() == 1 &&  foundLogs.contains(createdLog2));
    }

    /**
     * Search for a particular keyword with wildcards
     */
    @Test
    public void searchByKeywordWithWildcards()
    {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.put("desc", List.of("jump*"));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on a single keyword.",
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));
    }
    
    /**
     * Search for a particular set of keywords, the search is not sensitive to the order of the words
     */
    @Test
    public void searchByKeywords()
    {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.put("desc", List.of("brown quick"));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on a set of keywords",
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));
        searchParameters.put("desc", List.of("brown", "quick"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on a set of keywords",
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));
    }

    /**
     * 
     */
    @Test
    public void searchByKeywordsOrdered()
    {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.put("desc", List.of("\"brown quick\""));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on exact ordered match of key words",
                   foundLogs.size() == 0);
        
        searchParameters.put("desc", List.of("\"quick brown\""));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on exact ordered match of key words",
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));
    }

    /**
     * Search for a log entries based on owner
     */
    @Test
    public void searchByOwner()
    {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.put("owner", List.of(testOwner1));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on owner.",
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1));

        searchParameters.put("owner", List.of(testOwner1, testOwner2));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on owner.",
                foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));
    }

    /**
     * Search for log entries based on the tag/s attached to it
     */
    @Test
    public void searchByTags()
    {
        // simple search based on the tag name
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.put("tags", List.of(testTag1.getName()));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on a tag name : "+ testTag1.getName(),
                foundLogs.size() == 1 &&  foundLogs.contains(createdLog1));
        
        // search based on a tag name with wildcards
        searchParameters.put("tags", List.of("testTag*"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on tag names with wildcards : testTag*",
                foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));
    }

    /**
     * Search for log entries based on the logbook/s attached to it
     */
    @Test
    public void searchByLogbooks()
    {
        // simple search based on the tag name
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.put("logbooks", List.of(testLogbook1.getName()));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook name " + testLogbook1.getName(),
                foundLogs.size() == 1 &&  foundLogs.contains(createdLog1));
        
        // search based on a tag name with wildcards
        searchParameters.put("logbooks", List.of("testLogbook*"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));
    }

    @Test
    public void searchByPropertyName()
    {

        // simple search based on the property name
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.put("properties", List.of(testProperty1.getName()));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on property name " + testProperty1.getName(),
                foundLogs.size() == 1 && foundLogs.contains(createdLog1));
        searchParameters.put("properties", List.of(testProperty2.getName()));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on property name " + testProperty2.getName(),
                foundLogs.size() == 1 && foundLogs.contains(createdLog2));
        
        // search based on a tag name with wildcards
        searchParameters.put("properties", List.of("testProperty*"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));
    }

    @Test
    public void searchByTime() 
    {
        assertTrue(false);
    }
    
    @Test
    public void searchByEventTime()
    {
        assertTrue(false);
    }

    private String description1 = "The quick brown fox jumps over the lazy dog";
    private String source1 = "The quick brown *fox* jumps over the lazy *dog*";
    private String description2 = "The quick brown foxes jumped over the lazy dogs";
    private String source2 = "The quick brown *foxes* jumped over the lazy *dogs*";

    private static Log createdLog1;
    private static Log createdLog2;

    /**
     * Before running the search tests create the set of log entries to be used for searching
     */
    @Override
    public void beforeTestClass(TestContext testContext)
    {
        logRepository = (LogRepository) testContext.getApplicationContext().getBean("logRepository");
        TagRepository tagRepository = (TagRepository) testContext.getApplicationContext().getBean("tagRepository");
        LogbookRepository logbookRepository = (LogbookRepository) testContext.getApplicationContext().getBean("logbookRepository");
        PropertyRepository propertyRepository = (PropertyRepository) testContext.getApplicationContext().getBean("propertyRepository");

        tagRepository.saveAll(List.of(testTag1, testTag2));
        logbookRepository.saveAll(List.of(testLogbook1, testLogbook2));
        propertyRepository.saveAll(List.of(testProperty1, testProperty2));

        testProperty1 = new Property("testProperty1",
                testOwner1,
                State.Active,
                new HashSet<Attribute>(List.of(new Attribute("testProperty1", "log1"), new Attribute("testProperty2", "log1"))));
        // create a log entry with a logbook only
        Log log1 = Log.LogBuilder.createLog()
                                 .owner(testOwner1)
                                 .appendDescription(description1)
                                 .source(source1)
                                 .withLogbook(testLogbook1)
                                 .withTag(testTag1)
                                 .withProperty(testProperty1)
                                 .build();
        createdLog1 = logRepository.save(log1);

        testProperty2 = new Property("testProperty2",
                testOwner1,
                State.Active,
                new HashSet<Attribute>(List.of(new Attribute("testProperty1", "log2"))));
        Log log2 = Log.LogBuilder.createLog()
                                 .owner(testOwner2)
                                 .description(description2)
                                 .source(source2)
                                 .withLogbook(testLogbook2)
                                 .withTag(testTag2)
                                 .withProperty(testProperty2)
                                 .build();
        createdLog2 = logRepository.save(log2);
    }

    /**
     * cleanup all the tags, logbooks, properties and log entries created for testing
     */
    @Override
    public void afterTestClass(TestContext testContext) throws IOException
    {
        RestHighLevelClient client = (RestHighLevelClient) testContext.getApplicationContext().getBean("indexClient");

        client.delete(new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, testTag1.getName()), RequestOptions.DEFAULT);
        client.delete(new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, testTag2.getName()), RequestOptions.DEFAULT);
        client.delete(new DeleteRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, testLogbook1.getName()), RequestOptions.DEFAULT);
        client.delete(new DeleteRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, testLogbook2.getName()), RequestOptions.DEFAULT);
        client.delete(new DeleteRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, testProperty1.getName()), RequestOptions.DEFAULT);
        client.delete(new DeleteRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, testProperty2.getName()), RequestOptions.DEFAULT);

        client.delete(new DeleteRequest(ES_LOG_INDEX, ES_LOG_TYPE, createdLog1.getId().toString()), RequestOptions.DEFAULT);
        client.delete(new DeleteRequest(ES_LOG_INDEX, ES_LOG_TYPE, createdLog2.getId().toString()), RequestOptions.DEFAULT);

    }
}
