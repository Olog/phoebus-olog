package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import gov.bnl.olog.entity.State;
import gov.bnl.olog.entity.Tag;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ElasticConfig.class)
public class TagRepositoryIT {

    @Autowired
    ElasticConfig elasticConfig;
    @Autowired
    private TagRepository tagRepository;

    @BeforeClass
    public static void setup() {

    }

    @AfterClass
    public static void cleanup() {

    }

    /**
     * Test the creation of a test tag
     * @throws IOException 
     */
    @Test
    public void createTag() throws IOException {
        Tag testTag = new Tag("test-tag-1", State.Active);
        tagRepository.index(testTag);
        Optional<Tag> result = tagRepository.findById(testTag.getName());
        assertThat("Failed to create Tag " + testTag, result.isPresent() && result.get().equals(testTag));

        // Manual cleanup since Olog does not delete things
        elasticConfig.getIndexClient().delete(new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, testTag.getName()), RequestOptions.DEFAULT);
    }

    /**
     * Test the deletion of a test tag
     * @throws IOException 
     */
    @Test
    public void deleteTag() throws IOException {
        Tag testTag = new Tag("test-tag-2", State.Active);
        tagRepository.index(testTag);
        Optional<Tag> result = tagRepository.findById(testTag.getName());
        assertThat("Failed to create Tag " + testTag, result.isPresent() && result.get().equals(testTag));

        tagRepository.delete(testTag);
        result = tagRepository.findById(testTag.getName());
        testTag.setState(State.Inactive);
        assertThat("Failed to delete Tag", result.isPresent() && result.get().equals(testTag));

        // Manual cleanup since Olog does not delete things
        elasticConfig.getIndexClient().delete(new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, testTag.getName()), RequestOptions.DEFAULT);
    }

    /**
     * create a set of tags
     * @throws IOException 
     */
    @Test
    public void createTags() throws IOException {
        Tag testTag1 = new Tag("test-tag-1", State.Active);
        Tag testTag2 = new Tag("test-tag-2", State.Active);
        Tag testTag3 = new Tag("test-tag-3", State.Active);
        Tag testTag4 = new Tag("test-tag-4", State.Active);
        List<Tag> tags = Arrays.asList(testTag1, testTag2, testTag3, testTag4);
        List<Tag> result = new ArrayList<Tag>();
        tagRepository.saveAll(tags).forEach(tag -> {
            result.add(tag);
        });
        assertThat("Failed to create all tags", result.containsAll(tags));

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        List<Tag> findAll = new ArrayList<Tag>();
        tagRepository.findAll().forEach(tag -> {
            findAll.add(tag);
        });
        assertThat("Failed to list all tags", findAll.containsAll(tags));

        // Manual cleanup since Olog does not delete things
        BulkRequest bulk = new BulkRequest();
        tags.forEach(tag -> {
            bulk.add(new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, tag.getName()));
        });
        elasticConfig.getIndexClient().bulk(bulk, RequestOptions.DEFAULT);
    }

}
