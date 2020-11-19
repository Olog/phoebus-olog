package gov.bnl.olog;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import gov.bnl.olog.entity.State;
import gov.bnl.olog.entity.Tag;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations="classpath:test_application.properties")
public class TagResouceIT {

    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient client;

    @Autowired
    private TagRepository tagRepository;
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    private Tag testTag1 = new Tag("test-tag-1", State.Active);
    private Tag testTag2 = new Tag("test-tag-2", State.Active);
    private Tag testTag3 = new Tag("test-tag-3", State.Active);
    private Tag testTag4 = new Tag("test-tag-4", State.Active);

    // Read the elatic index and type from the application.properties
    @Value("${elasticsearch.tag.index:olog_tags}")
    private String ES_TAG_INDEX;
    @Value("${elasticsearch.tag.type:olog_tag}")
    private String ES_TAG_TYPE;

    /**
     * Test the creation of the same test tag fails
     * @throws IOException 
     */
    @Test
    public void createSameTagTwice() throws IOException {
    }

    /**
     * Test the deletion of a non existing test tag
     * @throws IOException 
     */
    @Test
    public void deleteTag() throws IOException {
    }


    /**
     * Cleanup the given tags
     * @param tags
     */
    private void cleanUp(List<Tag> tags)
    {
        try
        {
            BulkRequest bulk = new BulkRequest();
            tags.forEach(tag -> {
                bulk.add(new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName()));
            });
            client.bulk(bulk, RequestOptions.DEFAULT);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
