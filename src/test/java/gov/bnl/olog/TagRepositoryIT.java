package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_INDEX;
import static gov.bnl.olog.OlogResourceDescriptors.ES_TAG_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import gov.bnl.olog.entity.State;
import gov.bnl.olog.entity.Tag;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations="classpath:test_application.properties")
public class TagRepositoryIT {

    @Autowired
    @Qualifier("indexClient")
    RestHighLevelClient client;

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
        tagRepository.save(testTag);
        Optional<Tag> result = tagRepository.findById(testTag.getName());
        assertThat("Failed to create Tag " + testTag, result.isPresent() && result.get().equals(testTag));

        // Manual cleanup since Olog does not delete things
        client.delete(new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, testTag.getName()), RequestOptions.DEFAULT);
    }

    /**
     * Test the deletion of a test tag
     * @throws IOException 
     */
    @Test
    public void deleteTag() throws IOException {
        Tag testTag = new Tag("test-tag-2", State.Active);
        tagRepository.save(testTag);
        Optional<Tag> result = tagRepository.findById(testTag.getName());
        assertThat("Failed to create Tag " + testTag, result.isPresent() && result.get().equals(testTag));

        tagRepository.delete(testTag);
        result = tagRepository.findById(testTag.getName());
        testTag.setState(State.Inactive);
        assertThat("Failed to delete Tag", result.isPresent() && result.get().equals(testTag));

        // Manual cleanup since Olog does not delete things
        client.delete(new DeleteRequest(ES_TAG_INDEX, ES_TAG_TYPE, testTag.getName()), RequestOptions.DEFAULT);
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
        assertThat("Failed to create multiple tags", result.containsAll(tags));

        List<Tag> findAll = new ArrayList<Tag>();
        tagRepository.findAll().forEach(tag -> {
            findAll.add(tag);
        });
        assertThat("Failed to create multiple tags ", findAll.containsAll(tags));

        // Manual cleanup
        cleanUp(tags);
    }

    @Test
    public void findAllTags() throws IOException
    {
        Tag testTag1 = new Tag("test-tag-1", State.Active);
        Tag testTag2 = new Tag("test-tag-2", State.Active);
        Tag testTag3 = new Tag("test-tag-3", State.Active);
        Tag testTag4 = new Tag("test-tag-4", State.Active);
        List<Tag> tags = Arrays.asList(testTag1, testTag2, testTag3, testTag4);
        tagRepository.saveAll(tags);

        List<Tag> findAll = new ArrayList<Tag>();
        tagRepository.findAll().forEach(tag -> {
            findAll.add(tag);
        });
        assertThat("Failed to list all tags", findAll.containsAll(tags));
        // Manual cleanup
        cleanUp(tags);
    }

    @Test
    public void findAllTagsById() throws IOException
    {
        Tag testTag1 = new Tag("test-tag-1", State.Active);
        Tag testTag2 = new Tag("test-tag-2", State.Active);
        Tag testTag3 = new Tag("test-tag-3", State.Active);
        Tag testTag4 = new Tag("test-tag-4", State.Active);
        List<Tag> tags = Arrays.asList(testTag1, testTag2, testTag3, testTag4);
        tagRepository.saveAll(tags);

        List<Tag> findAllById = new ArrayList<Tag>();
        tagRepository.findAllById(Arrays.asList("test-tag-1", "test-tag-2")).forEach(tag -> {
            findAllById.add(tag);
        });
        assertTrue("Failed to search by id test-tag-1 and test-tag-2 " ,
                findAllById.size() == 2
                && findAllById.contains(testTag1)
                && findAllById.contains(testTag2));
        // Manual cleanup
        cleanUp(tags);
    }

    @Test
    public void findAllInactiveTags()
    {

    }
    @Test
    public void findById() throws IOException {
        Tag testTag1 = new Tag("test-tag-1", State.Active);
        Tag testTag2 = new Tag("test-tag-2", State.Active);
        List<Tag> tags = Arrays.asList(testTag1, testTag2);
        tagRepository.saveAll(tags);

        assertTrue("Failed to find by index tag: " + testTag1, testTag1.equals(tagRepository.findById(testTag1.getName()).get()));
        assertTrue("Failed to find by index tag: " + testTag2, testTag2.equals(tagRepository.findById(testTag2.getName()).get()));

        // Manual cleanup
        cleanUp(tags);
    }

    @Test
    public void checkTagExists() throws IOException {
        Tag testTag1 = new Tag("test-tag-1", State.Active);
        Tag testTag2 = new Tag("test-tag-2", State.Active);
        List<Tag> tags = Arrays.asList(testTag1, testTag2);
        tagRepository.saveAll(tags);

        assertTrue("Failed to check if exists tag: " + testTag1, tagRepository.existsById(testTag1.getName()));
        assertTrue("Failed to check if exists tag: " + testTag2, tagRepository.existsById(testTag2.getName()));

        assertFalse("Failed to check if exists tag: non-existant-tag", tagRepository.existsById("non-existant-tag"));

        // Manual cleanup
        cleanUp(tags);
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
