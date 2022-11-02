package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
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
import java.util.List;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@SuppressWarnings("unused")
public class TagResouceIT {


    @Autowired
    @Qualifier("client")
    ElasticsearchClient client;

    @Autowired
    private TagRepository tagRepository;

    private final Tag testTag1 = new Tag("test-tag-1", State.Active);
    private final Tag testTag2 = new Tag("test-tag-2", State.Active);
    private final Tag testTag3 = new Tag("test-tag-3", State.Active);
    private final Tag testTag4 = new Tag("test-tag-4", State.Active);

    // Read the elatic index and type from the application.properties
    @Value("${elasticsearch.tag.index:olog_tags}")
    private String ES_TAG_INDEX;

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
