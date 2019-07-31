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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.client.gridfs.model.GridFSFile;

import gov.bnl.olog.Config;
import gov.bnl.olog.LogRepository;
import gov.bnl.olog.LogbookRepository;
import gov.bnl.olog.PropertyRepository;
import gov.bnl.olog.TagRepository;
import gov.bnl.olog.entity.Attachment;
import gov.bnl.olog.entity.Attribute;
import gov.bnl.olog.entity.Log;
import gov.bnl.olog.entity.Logbook;
import gov.bnl.olog.entity.Property;
import gov.bnl.olog.entity.State;
import gov.bnl.olog.entity.Tag;
import junitx.framework.FileAssert;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Config.class)
public class LogRepositoryIT
{
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;
    @Autowired
    private LogbookRepository logbookRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private GridFsOperations gridOperation;

    private static Logbook testLogbook;
    private static Tag testTag;
    private static Property testProperty;

    @Autowired
    private LogRepository logRepository;

    @BeforeClass
    public static void setup()
    {

    }

    @AfterClass
    public static void cleanup()
    {
    }

    private static final String testOwner = "test-owner";

    /**
     * Test the creation of a simple test log
     */
    @Test
    public void createLog()
    {
        testLogbook = new Logbook("test-logbook-1", testOwner, State.Active);
        logbookRepository.index(testLogbook);

        testTag = new Tag("test-tag-1", State.Active);
        tagRepository.index(testTag);

        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(new Attribute("test-attribute-1"));
        attributes.add(new Attribute("test-attribute-2"));
        testProperty = new Property("test-property-1", testOwner, State.Active, attributes);
        propertyRepository.index(testProperty);

        // create a log entry with a logbook only
        Log log = Log.LogBuilder.createLog("This is a test entry").owner(testOwner).withLogbook(testLogbook).build();
        Log createdLog = logRepository.index(log);

        assertTrue("Failed to create a log entry with a valid id", createdLog.getId() != null);
        assertTrue(createdLog.getLogbooks().contains(testLogbook));

        Log log2 = Log.LogBuilder.createLog("This is a test entry").owner(testOwner).withTag(testTag)
                .withLogbook(testLogbook).build();
        Log createdLog2 = logRepository.index(log2);
        assertTrue(createdLog2.getLogbooks().contains(testLogbook));
        assertTrue(createdLog2.getTags().contains(testTag));

        Log log3 = Log.LogBuilder.createLog("This is a test entry").owner(testOwner).withTag(testTag)
                .withLogbook(testLogbook).withProperty(testProperty).build();
        Log createdLog3 = logRepository.index(log3);
        assertTrue(createdLog3.getLogbooks().contains(testLogbook));
        assertTrue(createdLog3.getTags().contains(testTag));
        assertTrue(createdLog3.getProperties().contains(testProperty));

        // Manual cleanup since Olog does not delete things
        elasticsearchTemplate.getClient().prepareDelete(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, testLogbook.getName())
                .get("10s");

        // Manual cleanup since Olog does not delete things
        elasticsearchTemplate.getClient().prepareDelete(ES_TAG_INDEX, ES_TAG_TYPE, testTag.getName()).get("10s");

        // Manual cleanup since Olog does not delete things
        elasticsearchTemplate.getClient().prepareDelete(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, testProperty.getName())
                .get("10s");

    }

    /**
     * Test the creation of a simple test log
     */
    @Test
    public void createLogWithAttachment()
    {
        testLogbook = new Logbook("test-logbook-1", testOwner, State.Active);
        logbookRepository.index(testLogbook);

        testTag = new Tag("test-tag-1", State.Active);
        tagRepository.index(testTag);

        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(new Attribute("test-attribute-1"));
        attributes.add(new Attribute("test-attribute-2"));
        testProperty = new Property("test-property-1", testOwner, State.Active, attributes);
        propertyRepository.index(testProperty);

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
            Log createdLog = logRepository.index(log);

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

            // Manual cleanup since Olog does not delete things
            elasticsearchTemplate.getClient().prepareDelete(ES_LOG_INDEX, ES_LOG_TYPE, createdLog.getId().toString()).get("10s");
        } catch ( IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally
        {
            // Manual cleanup since Olog does not delete things
            elasticsearchTemplate.getClient().prepareDelete(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, testLogbook.getName()).get("10s");

            // Manual cleanup since Olog does not del`ete things
            elasticsearchTemplate.getClient().prepareDelete(ES_TAG_INDEX, ES_TAG_TYPE, testTag.getName()).get("10s");

            // Manual cleanup since Olog does not delete things
            elasticsearchTemplate.getClient().prepareDelete(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, testProperty.getName()).get("10s");

        }

    }
}
