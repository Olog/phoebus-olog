package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.olog.entity.State;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
public class TagRepositoryIT {


    @Autowired
    private TagRepository tagRepository;

    private Tag testTag1 = new Tag("test-tag-1", State.Active);
    private Tag testTag2 = new Tag("test-tag-2", State.Active);
    private Tag testTag3 = new Tag("test-tag-3", State.Active);
    private Tag testTag4 = new Tag("test-tag-4", State.Active);

    // Read the elatic index and type from the application.properties
    @Value("${elasticsearch.tag.index:olog_tags}")
    private String ES_TAG_INDEX;

    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    /**
     * Test the creation of a test tag
     *
     * @throws IOException
     */
    @Test
    public void createTag() throws IOException {
        tagRepository.save(testTag1);
        Optional<Tag> result = tagRepository.findById(testTag1.getName());
        assertThat("Failed to create Tag " + testTag1, result.isPresent() && result.get().equals(testTag1));

        // Manual cleanup since Olog does not delete things
        client.delete(DeleteRequest.of(d -> d.index(ES_TAG_INDEX).id(testTag1.getName()).refresh(Refresh.True)));
    }

    /**
     * Test the deletion of a test tag
     *
     * @throws IOException
     */
    @Test
    public void deleteTag() throws IOException {
        tagRepository.save(testTag2);
        Optional<Tag> result = tagRepository.findById(testTag2.getName());
        assertThat("Failed to create Tag " + testTag2, result.isPresent() && result.get().equals(testTag2));

        tagRepository.delete(testTag2);
        result = tagRepository.findById(testTag2.getName());
        Tag expectedTag = new Tag("test-tag-2", State.Inactive);
        assertThat("Failed to delete Tag", result.isPresent() && result.get().equals(expectedTag));

        // Manual cleanup since Olog does not delete things
        client.delete(DeleteRequest.of(d -> d.index(ES_TAG_INDEX).id(testTag2.getName()).refresh(Refresh.True)));
    }

    /**
     * create a set of tags
     */
    @Test
    public void createTags() {
        List<Tag> tags = Arrays.asList(testTag1, testTag2, testTag3, testTag4);
        try {
            List<Tag> result = new ArrayList<Tag>();
            tagRepository.saveAll(tags).forEach(tag -> result.add(tag));
            assertThat("Failed to create multiple tags", result.containsAll(tags));

            List<Tag> findAll = new ArrayList<Tag>();
            tagRepository.findAll().forEach(tag -> findAll.add(tag));
            assertThat("Failed to create multiple tags ", findAll.containsAll(tags));
        } finally {
            // Manual cleanup
            cleanUp(tags);
        }
    }

    /**
     * delete a set of tags
     */
    @Test
    public void deleteTags() {
        List<Tag> tags = Arrays.asList(testTag1, testTag2, testTag3, testTag4);
        try {
            List<Tag> result = new ArrayList<Tag>();
            tagRepository.saveAll(tags).forEach(tag -> {
                result.add(tag);
            });

            tagRepository.deleteAll(tags);
            List<Tag> inactiveTags = new ArrayList<Tag>();
            tagRepository.findAllById(tags.stream().map(Tag::getName).collect(Collectors.toList())).forEach(tag -> {
                if (tag.getState().equals(State.Inactive)) {
                    inactiveTags.add(tag);
                }
            });
            assertThat("Failed to delete multiple tags ", inactiveTags.containsAll(tags));
        } finally {
            // Manual cleanup
            cleanUp(tags);
        }
    }

    @Test
    public void findAllTags() {
        List<Tag> tags = Arrays.asList(testTag1, testTag2, testTag3, testTag4);
        try {
            tagRepository.saveAll(tags);
            List<Tag> findAll = new ArrayList<Tag>();
            tagRepository.findAll().forEach(tag -> {
                findAll.add(tag);
            });
            assertThat("Failed to list all tags", findAll.containsAll(tags));
        } finally {
            // Manual cleanup
            cleanUp(tags);
        }
    }

    @Test
    public void findAllTagsByIds() throws IOException {
        List<Tag> tags = Arrays.asList(testTag1, testTag2, testTag3, testTag4);
        try {
            tagRepository.saveAll(tags);

            List<Tag> findAllById = new ArrayList<Tag>();
            tagRepository.findAllById(Arrays.asList("test-tag-1", "test-tag-2"))
                    .forEach(tag -> {
                        findAllById.add(tag);
                    });
            assertTrue("Failed to search by id test-tag-1 and test-tag-2 ",
                    findAllById.size() == 2 && findAllById.contains(testTag1) && findAllById.contains(testTag2));
        } finally {
            // Manual cleanup
            cleanUp(tags);
        }
    }

    @Test
    public void findAllInactiveTags() {

    }

    @Test
    public void findTagById() {
        List<Tag> tags = Arrays.asList(testTag1, testTag2);
        try {
            tagRepository.saveAll(tags);
            assertTrue("Failed to find by index tag: " + testTag1,
                    testTag1.equals(tagRepository.findById(testTag1.getName()).get()));
            assertTrue("Failed to find by index tag: " + testTag2,
                    testTag2.equals(tagRepository.findById(testTag2.getName()).get()));
        } finally {
            // Manual cleanup
            cleanUp(tags);
        }
    }

    @Test
    public void checkTagExists() {
        List<Tag> tags = Arrays.asList(testTag1, testTag2);
        try {
            tagRepository.saveAll(tags);

            assertTrue("Failed to check if exists tag: " + testTag1, tagRepository.existsById(testTag1.getName()));
            assertTrue("Failed to check if exists tag: " + testTag2, tagRepository.existsById(testTag2.getName()));

            assertFalse("Failed to check if exists tag: non-existant-tag", tagRepository.existsById("non-existant-tag"));
        } finally {
            // Manual cleanup
            cleanUp(tags);
        }
    }

    /**
     * Cleanup the given tags
     *
     * @param tags
     */

    private void cleanUp(List<Tag> tags) {
        List<BulkOperation> bulkOperations = new ArrayList<>();
        tags.forEach(tag -> bulkOperations.add(DeleteOperation.of(i ->
                i.index(ES_TAG_INDEX).id(tag.getName()))._toBulkOperation()));
        BulkRequest bulkRequest =
                BulkRequest.of(r ->
                        r.operations(bulkOperations).refresh(Refresh.True));
        try {
            client.bulk(bulkRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
