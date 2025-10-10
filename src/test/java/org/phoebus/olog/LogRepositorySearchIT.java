/**
 *
 */
package org.phoebus.olog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Event;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.State;
import org.phoebus.olog.entity.Tag;
import org.phoebus.util.time.TimestampFormats;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author kunal
 *
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = AuthenticationResource.class)
@TestExecutionListeners(listeners = {LogRepositorySearchIT.class})
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
class LogRepositorySearchIT  implements TestExecutionListener {

    private static LogRepository logRepository;

    private static final String testOwner1 = "testOwner1";
    private static final String testOwner2 = "testOwner2";

    private static final Logbook testLogbook1 = new Logbook("testLogbook1", testOwner1, State.Active);
    private static final Logbook testLogbook2 = new Logbook("testLogbook2", testOwner1, State.Active);

    private static final Tag testTag1 = new Tag("testTag1", State.Active);
    private static final Tag testTag2= new Tag("testTag2", State.Active);

    private static final Event event1 = new Event("testEvent1", Instant.now().minusSeconds(3600));
    private static final Event event2 = new Event("testEvent2", Instant.now().minusSeconds(2*3600));

    private static final Attribute testAttribute1 = new Attribute("testAttribute1");
    private static final Attribute testAttribute2 = new Attribute("testAttribute2");

    private static Property testProperty1 = new Property("testProperty1",
                                                         testOwner1,
                                                         State.Active,
            new HashSet<>(List.of(testAttribute1, testAttribute2)));


    private static Property testProperty2 = new Property("testProperty2",
                                                         testOwner1,
                                                         State.Active,
            new HashSet<>(List.of(testAttribute1)));

    /**
     * Search by title
     */
    @Test
    void searchByTitle() {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("title", List.of("title2"));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 1 && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on title.");

        searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("title", List.of("le"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on title.");

        // Search using wildcards in title
        searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("title", List.of("tit*"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on title.");

        // check case insensitive searches
        searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("title", List.of("TITLE2"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 1 && foundLogs.contains(createdLog2),
                "Failed case insensitive search for log entries based on title.");
    }

    /**
     * Search for log entries based on the level
     */
    @Test
    void searchByLevel() {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("level", List.of("level2"));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 1 && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on level.");

        searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("level", List.of("el"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on level.");

        searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("level", List.of("lev*"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on level.");
    }

    /**
     * Search for a particular word
     */
    @Test
    void searchByWord() {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("desc", List.of("quick"));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on a single keyword.");

        searchParameters.put("desc", List.of("jumped"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 1 &&  foundLogs.contains(createdLog2),
                "Failed to search for log entries based on a single keyword.");
    }

    /**
     * Search for a particular word with wildcards
     */
    @Test
    void searchByWordWithWildcards() {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("desc", List.of("jump*"));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on a single keyword.");
    }

    /**
     * Search for a particular set of words, the search is not sensitive to the order of the words
     */
    @Test
    void searchByWords() {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("desc", List.of("brown quick"));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on a set of keywords");
        searchParameters.put("desc", List.of("brown", "quick"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2));
    }

    /**
     *
     */
    @Test
    void searchByOrderedWords() {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("phrase", List.of("brown quick"));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertEquals(0, foundLogs.size(), "Failed to search for log entries based on exact ordered match of key words, expected 0 but found " + foundLogs.size());

        searchParameters.put("phrase", List.of("quick brown"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on exact ordered match of key words");
    }

    /**
     * Search for a log entries based on owner
     */
    @Test
    void searchByOwner() {
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("owner", List.of(testOwner1));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on owner.");

        searchParameters.put("owner", List.of(testOwner1, testOwner2));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on owner.");
    }

    /**
     * Search for log entries based on the tag/s attached to it
     */
    @Test
    void searchByTags() {
        // simple search based on the tag name
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("tags", List.of(testTag1.getName()));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 1 &&  foundLogs.contains(createdLog1),
                "Failed to search for log entries based on a tag name : "+ testTag1.getName());

        // search for log entries based on a list of tag names, the result contains
        // log entries that match at least one of the tag names
        searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("tags", List.of(testTag1.getName(), testTag2.getName()));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on tags name " + testTag1.getName() +" and " + testTag2.getName());

        // search based on a tag name with wildcards
        searchParameters.put("tags", List.of("testTag*"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on tag names with wildcards : testTag*");
    }

    /**
     * Search for log entries based on the logbook/s attached to it
     */
    @Test
    void searchByLogbooks() {
        // simple search based on the logbook name
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("logbooks", List.of(testLogbook1.getName()));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 1 &&  foundLogs.contains(createdLog1),
                "Failed to search for log entries based on logbook name " + testLogbook1.getName());

        // search for log entries based on a list of logbook names, the result contains
        // log entries that match at least one of the logbook names
        searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("logbooks", List.of(testLogbook1.getName(), testLogbook2.getName()));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on logbook name " + testLogbook1.getName() +" and " + testLogbook2.getName());

        // search based on a logbook name with wildcards
        searchParameters.put("logbooks", List.of("testLogbook*"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*");
    }

    @Test
    void searchByPropertyName() {
        // simple search based on the property name
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("properties", List.of(testProperty1.getName()));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on property name " + testProperty1.getName());
        searchParameters.put("properties", List.of(testProperty2.getName()));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 1 && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on property name " + testProperty2.getName());

        // search based on a property name with wildcards
        searchParameters.put("properties", List.of("testProperty*"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*");

        // search for a non existing property
        searchParameters.put("properties", List.of("noProperty"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertEquals(0, foundLogs.size(), "Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*");
    }

    @Test
    void searchByPropertyAttribute() {
        // simple search based on the property attribute
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("properties", List.of(testProperty1.getName() + "." + testAttribute1.getName()));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue( foundLogs.size() == 1 && foundLogs.contains(createdLog1), "Failed to search for log entries based on property atrribute name " + testProperty1.getName() + "."
                + testAttribute1.getName());
        searchParameters.put("properties", List.of(testProperty2.getName() + "." + testAttribute1.getName()));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(foundLogs.size() == 1 && foundLogs.contains(createdLog2), "Failed to search for log entries based on property name " + testProperty2.getName() + "."
                + testAttribute1.getName());

        // search based on a property name with wildcards and an attribute name
        searchParameters.put("properties", List.of("testProperty*.testAttribute1"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*");
        searchParameters.put("properties", List.of("testProperty*.testAttribute2"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*");

        // search based on a property name and attribute name with wildcards
        searchParameters.put("properties", List.of("testProperty*.testAttribute*"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*");

        // search for a non existing property
        searchParameters.put("properties", List.of(testProperty1.getName() + ".noAttribute"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertEquals(0, foundLogs.size(), "Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*");
    }

    @Test
    void searchByPropertyAttributeValue() {
        // simple search based on the property attribute
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("properties", List.of(testProperty1.getName() + "." + testAttribute1.getName() + ".log1"));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on property atrribute name "
                        + testProperty1.getName() + "."
                        + testAttribute1.getName() +".log1");
        searchParameters.put("properties", List.of(testProperty2.getName() + "." + testAttribute1.getName() + ".log2"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue( foundLogs.size() == 1 && foundLogs.contains(createdLog2),"Failed to search for log entries based on property name "
                        + testProperty2.getName() + "."
                        + testAttribute1.getName() + ".log2");

        // search based on a property name with wildcards and an attribute name
        searchParameters.put("properties", List.of("testProperty*.testAttribute1.*"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*");
        searchParameters.put("properties", List.of("testProperty*.testAttribute1.*1"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*");

        // search based on a property name and attribute name with wildcards
        searchParameters.put("properties", List.of("testProperty*.testAttribute*.log*"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*");

        searchParameters.put("properties", List.of("testProperty*.testAttribute1.log*"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 2 && foundLogs.contains(createdLog1) && foundLogs.contains(createdLog2),
                "Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*");

        // search for a non existing property
        searchParameters.put("properties", List.of(testProperty1.getName() + ".testAttribute1.noValue"));
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertEquals(0, foundLogs.size(), "Failed to search for log entries based on logbook names with wildcard cahrs : testLogbook*");
    }

    @Test
    void searchByTimeNoTimeZone() {
        // simple search based on the start and end time
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();

        searchParameters.put("start", List.of(TimestampFormats.MILLI_FORMAT.format(createdLog1.getCreatedDate().minusMillis(1000))));
        searchParameters.put("end",   List.of(TimestampFormats.MILLI_FORMAT.format(createdLog1.getCreatedDate().plusMillis(1000))));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on log entry create time");
    }

    @Test
    void searchByTimeWithTimeZone() {
        // simple search based on the start and end time
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();

        searchParameters.put("start", List.of(TimestampFormats.MILLI_FORMAT_WITH_TZ.format(createdLog1.getCreatedDate().minusMillis(1000))));
        searchParameters.put("end",   List.of(TimestampFormats.MILLI_FORMAT_WITH_TZ.format(createdLog1.getCreatedDate().plusMillis(1000))));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on log entry create time");
    }

    @Test
    void searchByEventTimeNoTimeZone() {
        // simple search based on events that occured between the start and end time
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();

        searchParameters.put("start", List.of(TimestampFormats.MILLI_FORMAT.format(event1.getInstant().minusMillis(1000))));
        searchParameters.put("end",   List.of(TimestampFormats.MILLI_FORMAT.format(event1.getInstant().plusMillis(1000))));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertEquals(0, foundLogs.size(), "Failed to search for log entries based on log event times");

        searchParameters.put("includeEvents", null);
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on log event times. Expected 1 log entry but found " + foundLogs.size());
    }

    @Test
    void searchByEventTimeWithTimeZone() {
        // simple search based on events that occured between the start and end time
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();

        searchParameters.put("start", List.of(TimestampFormats.MILLI_FORMAT_WITH_TZ.format(event1.getInstant().minusMillis(1000))));
        searchParameters.put("end",   List.of(TimestampFormats.MILLI_FORMAT_WITH_TZ.format(event1.getInstant().plusMillis(1000))));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();
        assertEquals(0, foundLogs.size(), "Failed to search for log entries based on log event times");

        searchParameters.put("includeEvents", null);
        foundLogs = logRepository.search(searchParameters).getLogs();
        assertTrue(
                foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on log event times. Expected 1 log entry but found " + foundLogs.size());
    }

    @Test
    void searchByMultipleKeywords() {
        // search for entries that satisfy all the search conditions.
        // Case 1: log entries matches the description, tag, logbook
        // expected result: only one log entry should match

        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("tags", List.of(testTag1.getName()));
        searchParameters.put("logbooks", List.of(testLogbook1.getName()));
        searchParameters.put("desc", List.of("quick"));
        List<Log> foundLogs = logRepository.search(searchParameters).getLogs();

        assertTrue(
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on desc, tag, and logbook");

        // Case 2: there is no log entry that matches this group of desc, logbook, tag.
        // expected result: no match should be returned.
        searchParameters = new LinkedMultiValueMap<>();
        searchParameters.put("tags", List.of(testTag2.getName()));
        searchParameters.put("logbooks", List.of(testLogbook1.getName()));
        searchParameters.put("desc", List.of("quick"));
        foundLogs = logRepository.search(searchParameters).getLogs();

        assertEquals(0, foundLogs.size(), "Failed to search for log entries based on desc, tag, and logbook");

        // Case 3: log entries matches the description, tag, logbook, and time
        // expected result: only one log entry should match
        searchParameters = new LinkedMultiValueMap<>();

        searchParameters.put("start", List.of(TimestampFormats.MILLI_FORMAT_WITH_TZ.format(createdLog1.getCreatedDate().minusMillis(1000))));
        searchParameters.put("end",   List.of(TimestampFormats.MILLI_FORMAT_WITH_TZ.format(createdLog1.getCreatedDate().plusMillis(1000))));
        searchParameters.put("tags", List.of(testTag1.getName()));
        searchParameters.put("logbooks", List.of(testLogbook1.getName()));
        searchParameters.put("desc", List.of("quick"));
        foundLogs = logRepository.search(searchParameters).getLogs();

        assertTrue(
                   foundLogs.size() == 1 && foundLogs.contains(createdLog1),
                "Failed to search for log entries based on desc, tag, logbook, and time");
    }

    private static Log createdLog1;
    private static Log createdLog2;

    /**
     * Before running the search tests create the set of log entries to be used for searching
     * @throws InterruptedException
     */
    @Override
    public void beforeTestClass(TestContext testContext) throws InterruptedException {
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
                new HashSet<>(List.of(new Attribute("testAttribute1", "log1"), new Attribute("testAttribute2", "log1"))));
        String description1 = "The quick brown fox jumps over the lazy dog";
        String source1 = "The quick brown *fox* jumps over the lazy *dog*";
        String title1 = "tit le";
        String level1 = "lev el";
        Log log1 = Log.LogBuilder.createLog()
                                 .owner(testOwner1)
                                 .appendDescription(description1)
                                 .source(source1)
                                 .title(title1)
                                 .level(level1)
                                 .withLogbook(testLogbook1)
                                 .withTag(testTag1)
                                 .withProperty(testProperty1)
                                 .withEvents(List.of(event1))
                                 .build();
        createdLog1 = logRepository.save(log1);

        // ensure that the log entries are created 5s apart to test time based searches
        Thread.sleep(1000);
        testProperty2 = new Property("testProperty2",
                testOwner1,
                State.Active,
                new HashSet<>(List.of(new Attribute("testAttribute1", "log2"))));
        String description2 = "The quick brown foxes jumped over the lazy dogs";
        String source2 = "The quick brown *foxes* jumped over the lazy *dogs*";
        String title2 = "title2";
        String level2 = "level2";
        Log log2 = Log.LogBuilder.createLog()
                                 .owner(testOwner2)
                                 .description(description2)
                                 .title(title2)
                                 .source(source2)
                                 .level(level2)
                                 .withLogbook(testLogbook2)
                                 .withTag(testTag2)
                                 .withProperty(testProperty2)
                                 .withEvents(List.of(event2))
                                 .build();
        createdLog2 = logRepository.save(log2);

        Thread.sleep(1000);
        String description3 = "some random test text for log entry 3";
        String source3 = "some random test text for log entry 3";
        logRepository.save(Log.LogBuilder.createLog().description(description3).source(source3).build());
        Thread.sleep(1000);
        String description4 = "some random test text for log entry 4";
        String source4 = "some random test text for log entry 4";
        logRepository.save(Log.LogBuilder.createLog().description(description4).source(source4).build());
    }
}
