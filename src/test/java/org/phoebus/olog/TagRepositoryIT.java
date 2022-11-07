package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.olog.entity.State;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@SuppressWarnings("unused")
public class TagRepositoryIT {


    @Autowired
    private TagRepository tagRepository;

    private final Tag testTag1 = new Tag("test-tag-1", State.Active);
    private final Tag testTag2 = new Tag("test-tag-2", State.Active);
    private final Tag testTag3 = new Tag("test-tag-3", State.Active);
    private final Tag testTag4 = new Tag("test-tag-4", State.Active);

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
            List<Tag> result = new ArrayList<>();
            tagRepository.saveAll(tags).forEach(result::add);
            assertThat("Failed to create multiple tags", result.containsAll(tags));

            List<Tag> findAll = new ArrayList<>();
            tagRepository.findAll().forEach(findAll::add);
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
            List<Tag> result = new ArrayList<>();
            tagRepository.saveAll(tags).forEach(result::add);

            tagRepository.deleteAll(tags);
            List<Tag> inactiveTags = new ArrayList<>();
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
            List<Tag> findAll = new ArrayList<>();
            tagRepository.findAll().forEach(findAll::add);
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

            List<Tag> findAllById = new ArrayList<>();
            tagRepository.findAllById(Arrays.asList("test-tag-1", "test-tag-2"))
                    .forEach(findAllById::add);
            assertTrue(
                    findAllById.size() == 2 && findAllById.contains(testTag1) && findAllById.contains(testTag2),
                    "Failed to search by id test-tag-1 and test-tag-2 ");
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
            assertEquals(testTag1, tagRepository.findById(testTag1.getName()).get(), "Failed to find by index tag: " + testTag1);
            assertEquals(testTag2, tagRepository.findById(testTag2.getName()).get(), "Failed to find by index tag: " + testTag2);
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

            assertTrue( tagRepository.existsById(testTag1.getName()),
                    "Failed to check if exists tag: " + testTag1);
            assertTrue( tagRepository.existsById(testTag2.getName()),
                    "Failed to check if exists tag: " + testTag2);

            assertFalse( tagRepository.existsById("non-existant-tag"),
                    "Failed to check if exists tag: non-existant-tag");
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
