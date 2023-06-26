package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;

import junitx.framework.FileAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.olog.entity.Attachment;
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Event;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.State;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = AuthenticationResource.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@SuppressWarnings("unused")
public class LogRepositoryIT {


    @Autowired
    private LogbookRepository logbookRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private GridFsOperations gridOperation;

    @Autowired
    private LogRepository logRepository;

    @SuppressWarnings("unused")
    @Autowired
    private GridFsTemplate gridFsTemplate;
    @SuppressWarnings("unused")
    @Autowired
    private GridFSBucket gridFSBucket;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    // Read the elatic index and type from the application.properties
    @Value("${elasticsearch.tag.index:olog_tags}")
    private String ES_TAG_INDEX;
    @Value("${elasticsearch.tag.type:olog_tag}")
    private String ES_TAG_TYPE;
    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    private String ES_LOGBOOK_INDEX;
    @Value("${elasticsearch.logbook.type:olog_logbook}")
    private String ES_LOGBOOK_TYPE;
    @Value("${elasticsearch.property.index:olog_properties}")
    private String ES_PROPERTY_INDEX;
    @Value("${elasticsearch.property.type:olog_property}")
    private String ES_PROPERTY_TYPE;
    @Value("${elasticsearch.log.index:olog_logs}")
    private String ES_LOG_INDEX;
    @Value("${elasticsearch.log.archive.index:olog_archived_logs}")
    private String ES_LOG_ARCHIVE_INDEX;

    @Value("${elasticsearch.log.type:olog_log}")
    private String ES_LOG_TYPE;

    private static final String TEST_OWNER = "test-owner";
    private static final Logbook TEST_LOGBOOK_1 = new Logbook("test-logbook-1", TEST_OWNER, State.Active);
    private static final Tag TEST_TAG_1 = new Tag("test-tag-1", State.Active);

    private static final Attribute ATTRIBUTE_1 = new Attribute("test-attribute-1");
    private static final Attribute ATTRIBUTE_2 = new Attribute("test-attribute-2");
    private static final Set<Attribute> ATTRIBUTES = new HashSet<>(List.of(ATTRIBUTE_1, ATTRIBUTE_2));
    private static final Property TEST_PROPERTY_1 = new Property("test-property-1", TEST_OWNER, State.Active, ATTRIBUTES);


    /**
     * Test the creation of a simple test log
     *
     * @throws IOException
     */
    @Test
    public void createLog() throws IOException {
        try {
            logbookRepository.save(TEST_LOGBOOK_1);
            tagRepository.save(TEST_TAG_1);
            propertyRepository.save(TEST_PROPERTY_1);

            // create a log entry with a logbook only
            Log log1 = Log.LogBuilder.createLog("This is a test entry").owner(TEST_OWNER).withLogbook(TEST_LOGBOOK_1).build();
            Log createdLog1 = logRepository.save(log1);

            assertNotNull(createdLog1.getId(), "Failed to create a log entry with a valid id");
            assertTrue(createdLog1.getLogbooks().contains(TEST_LOGBOOK_1));
            Log retrievedLog1 = logRepository.findById(String.valueOf(createdLog1.getId())).get();
            assertNotNull(retrievedLog1.getId(), "Failed to create a log entry with a valid id");
            assertTrue(retrievedLog1.getLogbooks().contains(TEST_LOGBOOK_1));

            Log log2 = Log.LogBuilder.createLog("This is a test entry").owner(TEST_OWNER).withTag(TEST_TAG_1)
                    .withLogbook(TEST_LOGBOOK_1).build();
            Log createdLog2 = logRepository.save(log2);
            assertTrue(createdLog2.getLogbooks().contains(TEST_LOGBOOK_1));
            assertTrue(createdLog2.getTags().contains(TEST_TAG_1));

            Log log3 = Log.LogBuilder.createLog("This is a test entry").owner(TEST_OWNER).withTag(TEST_TAG_1)
                    .withLogbook(TEST_LOGBOOK_1).withProperty(TEST_PROPERTY_1).build();
            Log createdLog3 = logRepository.save(log3);
            assertTrue(createdLog3.getLogbooks().contains(TEST_LOGBOOK_1));
            assertTrue(createdLog3.getTags().contains(TEST_TAG_1));
            assertTrue(createdLog3.getProperties().contains(TEST_PROPERTY_1));
            client.delete(DeleteRequest.of(d -> d.index(ES_LOG_INDEX).id(createdLog1.getId().toString()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_LOG_INDEX).id(createdLog2.getId().toString()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_LOG_INDEX).id(createdLog3.getId().toString()).refresh(Refresh.True)));
        } finally {
            client.delete(DeleteRequest.of(d -> d.index(ES_LOGBOOK_INDEX).id( TEST_LOGBOOK_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_TAG_INDEX).id( TEST_TAG_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_PROPERTY_INDEX).id( TEST_PROPERTY_1.getName()).refresh(Refresh.True)));
        }

    }

    /**
     * Test the creation of a test log with events
     *
     * @throws IOException
     */
    @Test
    public void createLogWithEvents() throws IOException {
        try {
            logbookRepository.save(TEST_LOGBOOK_1);
            tagRepository.save(TEST_TAG_1);
            propertyRepository.save(TEST_PROPERTY_1);

            List<Event> testEvents = List.of(new Event("now", Instant.ofEpochMilli(System.currentTimeMillis())));
            // create a log entry with a logbook only
            Log log1 = Log.LogBuilder.createLog("This is a test entry")
                    .owner(TEST_OWNER)
                    .withLogbook(TEST_LOGBOOK_1)
                    .withEvents(testEvents)
                    .build();
            Log createdLog1 = logRepository.save(log1);

            assertNotNull(createdLog1.getId(), "Failed to create a log entry with a valid id");
            assertTrue(createdLog1.getEvents().containsAll(testEvents));
            Log retrievedLog1 = logRepository.findById(String.valueOf(createdLog1.getId())).get();
            assertNotNull(retrievedLog1.getId(), "Failed to create a log entry with a valid id");
            assertTrue(retrievedLog1.getEvents().containsAll(testEvents));
            client.delete(DeleteRequest.of(d -> d.index(ES_LOG_INDEX).id(createdLog1.getId().toString()).refresh(Refresh.True)));
        } finally {
            client.delete(DeleteRequest.of(d -> d.index(ES_LOGBOOK_INDEX).id( TEST_LOGBOOK_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_TAG_INDEX).id( TEST_TAG_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_PROPERTY_INDEX).id( TEST_PROPERTY_1.getName()).refresh(Refresh.True)));
        }
    }

    /**
     * Test the creation of a simple test log with attachments
     *
     * @throws IOException
     */
    @Test
    public void createLogWithAttachment() throws IOException {
        logbookRepository.save(TEST_LOGBOOK_1);
        tagRepository.save(TEST_TAG_1);
        propertyRepository.save(TEST_PROPERTY_1);

        try {
            File testFile = new File("src/test/resources/Tulips.jpg");

            MockMultipartFile mock = new MockMultipartFile(testFile.getName(), new FileInputStream(testFile));
            Attachment testAttachment = new Attachment(mock, "Tulips.jpg", "");

            Log log = Log.LogBuilder.createLog("This is a test entry")
                    .owner(TEST_OWNER)
                    .withTag(TEST_TAG_1)
                    .withLogbook(TEST_LOGBOOK_1)
                    .withProperty(TEST_PROPERTY_1)
                    .withAttachment(testAttachment)
                    .build();
            Log createdLog = logRepository.save(log);

            createdLog.getAttachments().forEach(a -> {
                String id = a.getId();
                GridFSFile gridFsFile = gridFsTemplate.find(new Query(where("_id").is(id))).first();
                try {
                    File createdFile = new File("test_" + createdLog.getId() + "_" + a.getFilename());
                    InputStream st = gridOperation.getResource(gridFsFile).getInputStream();
                    Files.copy(st, createdFile.toPath());
                    FileAssert.assertBinaryEquals("failed to create log entry with attachment", testFile, createdFile);
                    Files.delete(createdFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    gridOperation.delete(new Query(Criteria.where("_id").is(id)));
                }
            });

            client.delete(DeleteRequest.of(d -> d.index(ES_LOG_INDEX).id(createdLog.getId().toString()).refresh(Refresh.True)));
        } finally {
            client.delete(DeleteRequest.of(d -> d.index(ES_LOGBOOK_INDEX).id( TEST_LOGBOOK_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_TAG_INDEX).id( TEST_TAG_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_PROPERTY_INDEX).id( TEST_PROPERTY_1.getName()).refresh(Refresh.True)));
        }
    }

    /**
     * Test the creation of a multiple test log entries
     *
     * @throws IOException
     */
    @Test
    public void createLogs(){
        logbookRepository.save(TEST_LOGBOOK_1);
        tagRepository.save(TEST_TAG_1);
        propertyRepository.save(TEST_PROPERTY_1);

        // create a log entry with a logbook only
        Log log1 = Log.LogBuilder.createLog("This is a test entry").owner(TEST_OWNER)
                .withLogbook(TEST_LOGBOOK_1).build();
        Log log2 = Log.LogBuilder.createLog("This is a test entry").owner(TEST_OWNER)
                .withLogbook(TEST_LOGBOOK_1).withTag(TEST_TAG_1).build();
        Log log3 = Log.LogBuilder.createLog("This is a test entry").owner(TEST_OWNER)
                .withLogbook(TEST_LOGBOOK_1).withTag(TEST_TAG_1).withProperty(TEST_PROPERTY_1).build();

        List<Log> createdLogs = new ArrayList<>();
        logRepository.saveAll(List.of(log1, log2, log3)).forEach(createdLogs::add);

        assertTrue(containsLogs(createdLogs, List.of(log1, log2, log3)), "Failed to create logs ");

        createdLogs.forEach(cleanupLog -> {
            try {
                client.delete(DeleteRequest.of(d -> d.index(ES_LOG_INDEX).id(cleanupLog.getId().toString()).refresh(Refresh.True)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Test the archive of a simple test log
     *
     * @throws IOException
     */
    @Test
    public void archiveLog() throws IOException {
        try {
            logbookRepository.save(TEST_LOGBOOK_1);
            tagRepository.save(TEST_TAG_1);
            propertyRepository.save(TEST_PROPERTY_1);

            Log originalLog = Log.LogBuilder.createLog("This is a test entry")
                                            .owner(TEST_OWNER)
                                            .withTag(TEST_TAG_1)
                                            .withLogbook(TEST_LOGBOOK_1)
                                            .withProperty(TEST_PROPERTY_1).build();
            Log createdOriginalLog = logRepository.save(originalLog);

            Log archivedLog = logRepository.archive(createdOriginalLog);

            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(ES_LOG_ARCHIVE_INDEX).id(archivedLog.getId()+"_v1"));
            BooleanResponse response = client.exists(existsRequest);
            assertTrue(response.value() , "Failed to archive log entries.");

            GetResponse<Log> archivedResponse = client.get(GetRequest.of(g -> g.index(ES_LOG_ARCHIVE_INDEX).id(archivedLog.getId()+"_v1")), Log.class);
            assertTrue(containsLogs(List.of(createdOriginalLog), List.of(archivedResponse.source())), "Archived log is same as the created log");

            client.delete(DeleteRequest.of(d -> d.index(ES_LOG_INDEX).id(createdOriginalLog.getId().toString()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_LOG_ARCHIVE_INDEX).id(archivedLog.getId()+"_v1").refresh(Refresh.True)));
        } finally {
            client.delete(DeleteRequest.of(d -> d.index(ES_LOGBOOK_INDEX).id( TEST_LOGBOOK_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_TAG_INDEX).id( TEST_TAG_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_PROPERTY_INDEX).id( TEST_PROPERTY_1.getName()).refresh(Refresh.True)));
        }

    }

    /**
     * Test the archiving of a simple test log
     *
     * @throws IOException
     */
    @Test
    public void archiveLogs() throws IOException {
        try {
            logbookRepository.save(TEST_LOGBOOK_1);
            tagRepository.save(TEST_TAG_1);
            propertyRepository.save(TEST_PROPERTY_1);

            Log originalLog = Log.LogBuilder.createLog("This is a test entry").owner(TEST_OWNER).withLogbook(TEST_LOGBOOK_1).build();

            Log originalCreatedLog = logRepository.save(originalLog);
            Log archivedLog = logRepository.archive(originalCreatedLog);
            logRepository.update(originalCreatedLog);
            Log updatedLog1 = logRepository.archive(originalCreatedLog);
            logRepository.update(originalCreatedLog);
            Log updatedLog2 = logRepository.archive(originalCreatedLog);

            // Archiving the same log entry multiple times should result in newer "ids"
            // Check that the log entry has been archived before updating
            String archiveLogId0 = originalCreatedLog.getId() +"_v1";
            String archiveLogId1 = originalCreatedLog.getId() +"_v2";
            String archiveLogId2 = originalCreatedLog.getId() +"_v3";

            List<String> expectedArchivedLogs = List.of(archiveLogId0, archiveLogId1, archiveLogId2);
            expectedArchivedLogs.stream().forEach(expectedArchivedLog -> {
                try {
                    assertTrue(client.exists(ExistsRequest.of(e -> e.index(ES_LOG_ARCHIVE_INDEX).id(expectedArchivedLog))).value() , "Failed to archive log entries.");
                    client.delete(DeleteRequest.of(d -> d.index(ES_LOG_ARCHIVE_INDEX).id(expectedArchivedLog).refresh(Refresh.True)));
                } catch (IOException e) {
                    fail("updated log " + expectedArchivedLog + " was not archived" , e);
                }
            });


            client.delete(DeleteRequest.of(d -> d.index(ES_LOG_INDEX).id(originalCreatedLog.getId().toString()).refresh(Refresh.True)));
        } finally {
            client.delete(DeleteRequest.of(d -> d.index(ES_LOGBOOK_INDEX).id( TEST_LOGBOOK_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_TAG_INDEX).id( TEST_TAG_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_PROPERTY_INDEX).id( TEST_PROPERTY_1.getName()).refresh(Refresh.True)));
        }

    }


    /**
     * Test the updating of a simple test log
     *
     * @throws IOException
     */
    @Test
    public void updateLog() throws IOException {
        try {
            logbookRepository.save(TEST_LOGBOOK_1);
            tagRepository.save(TEST_TAG_1);
            propertyRepository.save(TEST_PROPERTY_1);

            Log originalLog = Log.LogBuilder.createLog("This is a test entry")
                    .owner(TEST_OWNER)
                    .withTag(TEST_TAG_1).build();
            Log originalCreatedLog = logRepository.save(originalLog);

            String updatedSource = "This is an updated test entry";
            originalLog.setId(originalCreatedLog.getId());
            originalLog.setCreatedDate(originalCreatedLog.getCreatedDate());
            originalLog.setSource(updatedSource);
            originalLog.setTags(Set.of(TEST_TAG_1));
            originalLog.setProperties(Set.of(TEST_PROPERTY_1));

            Log updatedLog = logRepository.update(originalLog);

            assertTrue(updatedLog.getSource().equals(updatedSource), "Failed to update the source");
            assertTrue(updatedLog.getTags().contains(TEST_TAG_1) && updatedLog.getProperties().contains(TEST_PROPERTY_1), "Failed to update with new Tags and Properties");

            client.delete(DeleteRequest.of(d -> d.index(ES_LOG_INDEX).id(originalCreatedLog.getId().toString()).refresh(Refresh.True)));
        } finally {
            client.delete(DeleteRequest.of(d -> d.index(ES_LOGBOOK_INDEX).id( TEST_LOGBOOK_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_TAG_INDEX).id( TEST_TAG_1.getName()).refresh(Refresh.True)));
            client.delete(DeleteRequest.of(d -> d.index(ES_PROPERTY_INDEX).id( TEST_PROPERTY_1.getName()).refresh(Refresh.True)));
        }

    }

    @Test
    public void checkLogExists() throws IOException {
        // check for non existing log entry 
        assertFalse(logRepository.existsById("123456789"), "Failed to check non existance of log entry 123456789");

        // check for an existing log entry
        Log log = Log.LogBuilder.createLog("This is a test entry").owner(TEST_OWNER).withLogbook(TEST_LOGBOOK_1).build();
        Log createdLog = logRepository.save(log);

        assertNotNull(createdLog.getId(), "Failed to create a log entry with a valid id");
        assertTrue(logRepository.existsById(String.valueOf(createdLog.getId())), "Failed to check existance of log entry " + createdLog.getId());

        client.delete(DeleteRequest.of(d -> d.index(ES_LOG_INDEX).id(createdLog.getId().toString()).refresh(Refresh.True)));
    }


    @Test
    public void findLogsById() throws IOException {
        // check for an existing log entry
        Log log = Log.LogBuilder.createLog("This is a test entry").owner(TEST_OWNER).withLogbook(TEST_LOGBOOK_1).build();
        Log createdLog = logRepository.save(log);

        assertNotNull(createdLog.getId(), "Failed to create a log entry with a valid id");
        assertTrue( logRepository.existsById(String.valueOf(createdLog.getId())), "Failed to check existance of log entry " + createdLog.getId());

        client.delete(DeleteRequest.of(d -> d.index(ES_LOG_INDEX).id(createdLog.getId().toString()).refresh(Refresh.True)));
    }

    @Test
    public void findLogsByNonExistingId() throws IOException {
        // check for non existing log entry 
        assertFalse(logRepository.existsById("123456789"), "Failed to check non existance of log entry 123456789");
    }

    @Test
    public void findLogsByIds() throws IOException {
        // check for an existing log entry
        Log log1 = Log.LogBuilder.createLog("This is a test entry").owner(TEST_OWNER).withLogbook(TEST_LOGBOOK_1).build();
        Log log2 = Log.LogBuilder.createLog("This is a test entry").owner(TEST_OWNER).withLogbook(TEST_LOGBOOK_1).build();
        List<Log> createdLogs = StreamSupport.stream(logRepository.saveAll(List.of(log1, log2)).spliterator(), false).collect(Collectors.toList());
        assertTrue(
                containsLogs(logRepository.findAllById(createdLogs.stream().map(log -> String.valueOf(log.getId())).collect(Collectors.toList()))
                        , createdLogs),
                "Failed to find logs by ids:");


        createdLogs.forEach(cleanupLog -> {
            try {
                client.delete(DeleteRequest.of(d -> d.index(ES_LOG_INDEX).id(cleanupLog.getId().toString()).refresh(Refresh.True)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean containsLogs(Iterable<Log> foundLogs, Iterable<Log> expectedLogs) {
        return containsLogs(() -> StreamSupport.stream(foundLogs.spliterator(), false),
                () -> StreamSupport.stream(expectedLogs.spliterator(), false));
    }

    /**
     * Checks if the expected logs are present in the found logs. The id's field is
     * ignored since the expected logs are usually user created objects and the id
     * is assigned by the service
     *
     * @return
     */
    private boolean containsLogs(List<Log> foundLogs, List<Log> expectedLogs) {
        return containsLogs(foundLogs::stream, expectedLogs::stream);
    }

    private boolean containsLogs(Supplier<Stream<Log>> foundLogs, Supplier<Stream<Log>> expectedLogs) {
        return expectedLogs.get().allMatch(expectedLog -> foundLogs.get().anyMatch(foundLog -> foundLog.getId() != null &&
                foundLog.getDescription().equals(expectedLog.getDescription()) &&
                foundLog.getLogbooks().equals(expectedLog.getLogbooks()) &&
                foundLog.getTags().equals(expectedLog.getTags()) &&
                foundLog.getProperties().equals(expectedLog.getProperties()) &&
                foundLog.getOwner().equals(expectedLog.getOwner()) &&
                foundLog.getSource().equals(expectedLog.getSource())));
    }
}
