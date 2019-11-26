package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_LOGBOOK_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOGBOOK_TYPE;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_TYPE;
import static gov.bnl.olog.OlogResourceDescriptors.ES_PROPERTY_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_PROPERTY_TYPE;
import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_TYPE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.client.gridfs.model.GridFSFile;

import gov.bnl.olog.entity.Attachment;
import gov.bnl.olog.entity.Attribute;
import gov.bnl.olog.entity.Log;
import gov.bnl.olog.entity.Logbook;
import gov.bnl.olog.entity.Property;
import gov.bnl.olog.entity.State;
import gov.bnl.olog.entity.Tag;
import junitx.framework.FileAssert;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations="classpath:test_application.properties")
public class LogRepositoryIT
{
    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient client;

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


    private static final String testOwner = "test-owner";
    private static Logbook testLogbook = new Logbook("test-logbook-1", testOwner, State.Active);
    private static Tag testTag = new Tag("test-tag-1", State.Active);

    private static Attribute attribute1 = new Attribute("test-attribute-1");
    private static Attribute attribute2 = new Attribute("test-attribute-2");
    private static Set<Attribute> attributes = new HashSet<Attribute>(List.of(attribute1, attribute2));
    private static Property testProperty = new Property("test-property-1", testOwner, State.Active, attributes);


    /**
     * Test the creation of a simple test log
     * @throws IOException 
     */
    @Test
    public void createLog() throws IOException
    {
        try
        {
        logbookRepository.save(testLogbook);
        tagRepository.save(testTag);
        propertyRepository.save(testProperty);

        // create a log entry with a logbook only
        Log log1 = Log.LogBuilder.createLog("This is a test entry").owner(testOwner).withLogbook(testLogbook).build();
        Log createdLog1 = logRepository.save(log1);

        assertTrue("Failed to create a log entry with a valid id", createdLog1.getId() != null);
        assertTrue(createdLog1.getLogbooks().contains(testLogbook));

        Log log2 = Log.LogBuilder.createLog("This is a test entry").owner(testOwner).withTag(testTag)
                .withLogbook(testLogbook).build();
        Log createdLog2 = logRepository.save(log2);
        assertTrue(createdLog2.getLogbooks().contains(testLogbook));
        assertTrue(createdLog2.getTags().contains(testTag));

        Log log3 = Log.LogBuilder.createLog("This is a test entry").owner(testOwner).withTag(testTag)
                .withLogbook(testLogbook).withProperty(testProperty).build();
        Log createdLog3 = logRepository.save(log3);
        assertTrue(createdLog3.getLogbooks().contains(testLogbook));
        assertTrue(createdLog3.getTags().contains(testTag));
        assertTrue(createdLog3.getProperties().contains(testProperty));

        client.delete(new DeleteRequest(ES_LOG_INDEX, ES_LOG_TYPE, createdLog1.getId().toString()), RequestOptions.DEFAULT);
        client.delete(new DeleteRequest(ES_LOG_INDEX, ES_LOG_TYPE, createdLog2.getId().toString()), RequestOptions.DEFAULT);
        client.delete(new DeleteRequest(ES_LOG_INDEX, ES_LOG_TYPE, createdLog3.getId().toString()), RequestOptions.DEFAULT);

        } finally
        {
            // Manual cleanup since Olog does not delete things
            client.delete(new DeleteRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, testLogbook.getName()), RequestOptions.DEFAULT);

            // Manual cleanup since Olog does not delete things
            client.delete(new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, testTag.getName()), RequestOptions.DEFAULT);

            // Manual cleanup since Olog does not delete things
            client.delete(new DeleteRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, testProperty.getName()), RequestOptions.DEFAULT);
        }

    }

    /**
     * Test the creation of a simple test log with attachments
     * @throws IOException 
     */
    @Test
    public void createLogWithAttachment() throws IOException
    {
        logbookRepository.save(testLogbook);
        tagRepository.save(testTag);
        propertyRepository.save(testProperty);

        try
        {
            File testFile = new File("src/test/resources/Tulips.jpg");

            MockMultipartFile mock = new MockMultipartFile(testFile.getName(), new FileInputStream(testFile));
            Attachment testAttachment = new Attachment(mock, "Tulips.jpg", "");

            Log log = Log.LogBuilder.createLog("This is a test entry")
                    .owner(testOwner)
                    .withTag(testTag)
                    .withLogbook(testLogbook)
                    .withProperty(testProperty)
                    .withAttachment(testAttachment)
                    .build();
            Log createdLog = logRepository.save(log);

            createdLog.getAttachments().forEach(a -> {
                String id = a.getId();
                gridOperation.find(new Query(Criteria.where("_id").is(id))).forEach(new Consumer<GridFSFile>()
                {

                    @Override
                    public void accept(GridFSFile t)
                    {
                        try
                        {
                            File createdFile = new File("test_" + createdLog.getId()+ "_" + a.getFilename());
                            InputStream st = gridOperation.getResource(t).getInputStream();
                            Files.copy(st, createdFile.toPath());
                            FileAssert.assertBinaryEquals("failed to create log entry with attachment", testFile, createdFile);
                            Files.delete(createdFile.toPath());
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        } finally
                        {
                            gridOperation.delete(new Query(Criteria.where("_id").is(id)));
                        }
                    }

                });
            });

            assertTrue(createdLog.getLogbooks().contains(testLogbook));
            assertTrue(createdLog.getTags().contains(testTag));
            assertTrue(createdLog.getProperties().contains(testProperty));

            client.delete(new DeleteRequest(ES_LOG_INDEX, ES_LOG_TYPE, createdLog.getId().toString()), RequestOptions.DEFAULT);
            // Manual cleanup since Olog does not delete things
            client.delete(new DeleteRequest(ES_LOG_INDEX, ES_LOG_TYPE, createdLog.getId().toString()),
            RequestOptions.DEFAULT);
        } finally
        {
            // Manual cleanup since Olog does not delete things
            client.delete(new DeleteRequest(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, testLogbook.getName()),
            RequestOptions.DEFAULT);

            // Manual cleanup since Olog does not del`ete things
            client.delete(new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, testTag.getName()),
            RequestOptions.DEFAULT);

            // Manual cleanup since Olog does not delete things
            client.delete(new DeleteRequest(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, testProperty.getName()),
            RequestOptions.DEFAULT);
        }
    }

    /**
     * Test the creation of a multiple test log entries
     * @throws IOException 
     */
    @Test
    public void createLogs() throws IOException
    {
        logbookRepository.save(testLogbook);
        tagRepository.save(testTag);
        propertyRepository.save(testProperty);

        // create a log entry with a logbook only
        Log log1 = Log.LogBuilder.createLog("This is a test entry").owner(testOwner)
                                 .withLogbook(testLogbook).build();
        Log log2 = Log.LogBuilder.createLog("This is a test entry").owner(testOwner)
                                 .withLogbook(testLogbook).withTag(testTag).build();
        Log log3 = Log.LogBuilder.createLog("This is a test entry").owner(testOwner)
                                 .withLogbook(testLogbook).withTag(testTag).withProperty(testProperty).build();

        List<Log> createdLogs = new ArrayList<Log>(); 
        logRepository.saveAll(List.of(log1, log2, log3)).forEach(log -> createdLogs.add(log));

        assertTrue("Failed to create logs ", containsLogs(createdLogs, List.of(log1, log2, log3)));
        createdLogs.forEach(cleanupLog -> {
            try
            {
                client.delete(new DeleteRequest(ES_LOG_INDEX, ES_LOG_TYPE, cleanupLog.getId().toString()),
                                                RequestOptions.DEFAULT);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }
    
    @Test
    public void checkLogExists() throws IOException
    {
        // check for non existing log entry 
        assertFalse("Failed to check non existance of log entry 123456789" , logRepository.existsById("123456789"));

        // check for an existing log entry
        Log log = Log.LogBuilder.createLog("This is a test entry").owner(testOwner).withLogbook(testLogbook).build();
        Log createdLog = logRepository.save(log);

        assertTrue("Failed to create a log entry with a valid id", createdLog.getId() != null);
        assertTrue("Failed to check existance of log entry " + createdLog.getId(), logRepository.existsById(String.valueOf(createdLog.getId())));

        client.delete(new DeleteRequest(ES_LOG_INDEX, ES_LOG_TYPE, createdLog.getId().toString()), RequestOptions.DEFAULT);
    }
    
    

    @Test
    public void findLogsById() throws IOException
    {
        // check for an existing log entry
        Log log = Log.LogBuilder.createLog("This is a test entry").owner(testOwner).withLogbook(testLogbook).build();
        Log createdLog = logRepository.save(log);

        assertTrue("Failed to create a log entry with a valid id", createdLog.getId() != null);
        assertTrue("Failed to check existance of log entry " + createdLog.getId(), logRepository.existsById(String.valueOf(createdLog.getId())));

        client.delete(new DeleteRequest(ES_LOG_INDEX, ES_LOG_TYPE, createdLog.getId().toString()), RequestOptions.DEFAULT);
    }

    @Test
    public void findLogsByNonExistingId() throws IOException
    {
        // check for non existing log entry 
        assertFalse("Failed to check non existance of log entry 123456789" , logRepository.existsById("123456789"));
    }

    @Test
    public void findLogsByIds() throws IOException
    {
        // check for an existing log entry
        Log log1 = Log.LogBuilder.createLog("This is a test entry").owner(testOwner).withLogbook(testLogbook).build();
        Log log2 = Log.LogBuilder.createLog("This is a test entry").owner(testOwner).withLogbook(testLogbook).build();
        List<Log> createdLogs = StreamSupport.stream(logRepository.saveAll(List.of(log1, log2)).spliterator(), false).collect(Collectors.toList());
        assertTrue("Failed to find logs by ids:",
                containsLogs(logRepository.findAllById(createdLogs.stream().map(log -> {
                                                                                    return String.valueOf(log.getId());
                                                                                }).collect(Collectors.toList()))
                        , createdLogs));

        createdLogs.forEach(cleanupLog -> {
            try
            {
                client.delete(new DeleteRequest(ES_LOG_INDEX, ES_LOG_TYPE, cleanupLog.getId().toString()),
                                                RequestOptions.DEFAULT);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }

    private boolean containsLogs(Iterable<Log> foundLogs, Iterable<Log> expectedLogs)
    {
        return containsLogs(() -> StreamSupport.stream(foundLogs.spliterator(), false) ,
                            () -> StreamSupport.stream(expectedLogs.spliterator(), false));
    }

    /**
     * Checks if the expected logs are present in the found logs. The id's field is
     * ignored since the expected logs are usually user created objects and the id
     * is assigned by the service
     * 
     * @return
     */
    private boolean containsLogs(List<Log> foundLogs, List<Log> expectedLogs)
    {
        return containsLogs(() -> foundLogs.stream(), () -> expectedLogs.stream());
    }

    private boolean containsLogs(Supplier<Stream<Log>> foundLogs, Supplier<Stream<Log>> expectedLogs)
    {
        return expectedLogs.get().allMatch(expectedLog -> {
            return foundLogs.get().anyMatch(foundLog -> {
                return foundLog.getId() != null &&
                       foundLog.getDescription().equals(expectedLog.getDescription()) &&
                       foundLog.getLogbooks().equals(expectedLog.getLogbooks()) &&
                       foundLog.getTags().equals(expectedLog.getTags()) &&
                       foundLog.getProperties().equals(expectedLog.getProperties()) &&
                       foundLog.getOwner().equals(expectedLog.getOwner()) &&
                       foundLog.getSource().equals(expectedLog.getSource());
            });
        });
    }
}
