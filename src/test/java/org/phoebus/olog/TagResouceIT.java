package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import org.elasticsearch.client.RequestOptions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
public class TagResouceIT {


    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

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

    /**
     * Test the creation of the same test tag fails
     *
     * @throws IOException
     */
    @Test
    public void createSameTagTwice() throws IOException {
    }

    /**
     * Test the deletion of a non existing test tag
     *
     * @throws IOException
     */
    @Test
    public void deleteTag() throws IOException {
    }


    /**
     * Cleanup the given tags
     *
     * @param tags
     */
    private void cleanUp(List<Tag> tags) {
        List<BulkOperation> bulkOperations = new ArrayList<>();
        tags.forEach(tag -> bulkOperations.add(IndexOperation.of(i ->
                i.index(ES_TAG_INDEX).document(tag).id(tag.getName()))._toBulkOperation()));
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
