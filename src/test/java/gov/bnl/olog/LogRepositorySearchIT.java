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

import static gov.bnl.olog.LogSearchUtil.MILLI_FORMAT;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
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
import gov.bnl.olog.entity.Event;
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
    
    private static Event event1 = new Event("testEvent1", Instant.now().minusSeconds(3600));
    private static Event event2 = new Event("testEvent2", Instant.now().minusSeconds(2*3600));

    private static Attribute testAttribute1 = new Attribute("testAttribute1");
    private static Attribute testAttribute2 = new Attribute("testAttribute2");

    private static Property testProperty1 = new Property("testProperty1",
                                                         testOwner1,
                                                         State.Active,
                                                         new HashSet<Attribute>(List.of(testAttribute1, testAttribute2)));
    

    private static Property testProperty2 = new Property("testProperty2",
                                                         testOwner1,
                                                         State.Active,
                                                         new HashSet<Attribute>(List.of(testAttribute1)));

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
        searchParameters.put("phrase", List.of("brown quick"));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on exact ordered match of key words, expected 0 but found " + foundLogs.size(),
                   foundLogs.size() == 0);
        
        searchParameters.put("phrase", List.of("quick brown"));
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

        // search based on a property name with wildcards
        searchParameters.put("properties", List.of("testProperty*"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));

        // search for a non existing property
        searchParameters.put("properties", List.of("noProperty"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                   foundLogs.size() == 0);
    }

    @Test
    public void searchByPropertyAttribute()
    {

        // simple search based on the property attribute
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.put("properties", List.of(testProperty1.getName() + "." + testAttribute1.getName()));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on property atrribute name " + testProperty1.getName() + "."
                + testAttribute1.getName(), foundLogs.size() == 1 && foundLogs.contains(createdLog1));
        searchParameters.put("properties", List.of(testProperty2.getName() + "." + testAttribute1.getName()));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on property name " + testProperty2.getName() + "."
                + testAttribute1.getName(), foundLogs.size() == 1 && foundLogs.contains(createdLog2));

        // search based on a property name with wildcards and an attribute name
        searchParameters.put("properties", List.of("testProperty*.testAttribute1"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));
        searchParameters.put("properties", List.of("testProperty*.testAttribute2"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1));
        
        // search based on a property name and attribute name with wildcards
        searchParameters.put("properties", List.of("testProperty*.testAttribute*"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));

        // search for a non existing property
        searchParameters.put("properties", List.of(testProperty1.getName() + ".noAttribute"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                   foundLogs.size() == 0);
    }


    @Test
    public void searchByPropertyAttributeValue()
    {

        // simple search based on the property attribute
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();
        searchParameters.put("properties", List.of(testProperty1.getName() + "." + testAttribute1.getName() + ".log1"));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on property atrribute name "
                                                        + testProperty1.getName() + "."
                                                        + testAttribute1.getName() +".log1",
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1));
        searchParameters.put("properties", List.of(testProperty2.getName() + "." + testAttribute1.getName() + ".log2"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on property name "
                                            + testProperty2.getName() + "."
                                            + testAttribute1.getName() + ".log2"
                   , foundLogs.size() == 1 && foundLogs.contains(createdLog2));

        // search based on a property name with wildcards and an attribute name
        searchParameters.put("properties", List.of("testProperty*.testAttribute1.*"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));
        searchParameters.put("properties", List.of("testProperty*.testAttribute1.*1"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1));
        
        // search based on a property name and attribute name with wildcards
        searchParameters.put("properties", List.of("testProperty*.testAttribute*.log*"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));

        searchParameters.put("properties", List.of("testProperty*.testAttribute1.log*"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));

        // search for a non existing property
        searchParameters.put("properties", List.of(testProperty1.getName() + ".testAttribute1.noValue"));
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*",
                   foundLogs.size() == 0);
    }

    @Test
    public void searchByTime() 
    {
        // simple search based on the start and end time
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();

        searchParameters.put("start", List.of(MILLI_FORMAT.format(createdLog1.getCreatedDate().minusMillis(1000))));
        searchParameters.put("end",   List.of(MILLI_FORMAT.format(createdLog1.getCreatedDate().plusMillis(1000))));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on log entry create time",
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1));
    }

    @Test
    public void searchByEventTime()
    {   
        // simple search based on events that occured between the start and end time
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<String, String>();

        searchParameters.put("start", List.of(MILLI_FORMAT.format(event1.getEvent().minusMillis(1000))));
        searchParameters.put("end",   List.of(MILLI_FORMAT.format(event1.getEvent().plusMillis(1000))));
        List<Log> foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on log event times", foundLogs.size() == 0);

        searchParameters.put("includeEvents", null);
        foundLogs = logRepository.search(searchParameters);
        assertTrue("Failed to search for log entries based on log event times. Expected 1 log entry but found " + foundLogs.size(),
                foundLogs.size() == 1 && foundLogs.contains(createdLog1));
    }

    private String description1 = "The quick brown fox jumps over the lazy dog";
    private String source1 = "The quick brown *fox* jumps over the lazy *dog*";
    private String description2 = "The quick brown foxes jumped over the lazy dogs";
    private String source2 = "The quick brown *foxes* jumped over the lazy *dogs*";

    private static Log createdLog1;
    private static Log createdLog2;

    /**
     * Before running the search tests create the set of log entries to be used for searching
     * @throws InterruptedException 
     */
    @Override
    public void beforeTestClass(TestContext testContext) throws InterruptedException
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
                new HashSet<Attribute>(List.of(new Attribute("testAttribute1", "log1"), new Attribute("testAttribute2", "log1"))));
        Log log1 = Log.LogBuilder.createLog()
                                 .owner(testOwner1)
                                 .appendDescription(description1)
                                 .source(source1)
                                 .withLogbook(testLogbook1)
                                 .withTag(testTag1)
                                 .withProperty(testProperty1)
                                 .withEvents(Arrays.asList(event1))
                                 .build();
        createdLog1 = logRepository.save(log1);

        // ensure that the log entries are created 5s apart to test time based searches
        Thread.sleep(5000);
        testProperty2 = new Property("testProperty2",
                testOwner1,
                State.Active,
                new HashSet<Attribute>(List.of(new Attribute("testAttribute1", "log2"))));
        Log log2 = Log.LogBuilder.createLog()
                                 .owner(testOwner2)
                                 .description(description2)
                                 .source(source2)
                                 .withLogbook(testLogbook2)
                                 .withTag(testTag2)
                                 .withProperty(testProperty2)
                                 .withEvents(Arrays.asList(event2))
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
