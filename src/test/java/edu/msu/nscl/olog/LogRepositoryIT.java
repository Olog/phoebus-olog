package edu.msu.nscl.olog;

import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOGBOOK_INDEX;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOGBOOK_TYPE;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOG_INDEX;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_LOG_TYPE;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_PROPERTY_INDEX;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_PROPERTY_TYPE;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_TAG_INDEX;
import static edu.msu.nscl.olog.OlogResourceDescriptors.ES_TAG_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.msu.nscl.olog.entity.State;
import edu.msu.nscl.olog.entity.Tag;
import edu.msu.nscl.olog.entity.Attribute;
import edu.msu.nscl.olog.entity.Log;
import edu.msu.nscl.olog.entity.Logbook;
import edu.msu.nscl.olog.entity.Property;

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
        System.out.println();
        

        // Manual cleanup since Olog does not delete things
        elasticsearchTemplate.getClient().prepareDelete(ES_LOGBOOK_INDEX, ES_LOGBOOK_TYPE, testLogbook.getName())
                .get("10s");

        // Manual cleanup since Olog does not delete things
        elasticsearchTemplate.getClient().prepareDelete(ES_TAG_INDEX, ES_TAG_TYPE, testTag.getName()).get("10s");

        // Manual cleanup since Olog does not delete things
        elasticsearchTemplate.getClient().prepareDelete(ES_PROPERTY_INDEX, ES_PROPERTY_TYPE, testProperty.getName())
                .get("10s");

    }

}
