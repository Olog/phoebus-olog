package edu.msu.nscl.olog;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.client.AdminClient;

public class IndexMappers {

    public static final String seqIndex = "seq";

    public static void createSeqIndex() {
        AdminClient adminClient = ElasticSearchClient.getIndexClient().admin();
        adminClient.indices().create(new CreateIndexRequest(seqIndex)).actionGet();
        try {
            adminClient.indices()
            .preparePutMapping(seqIndex)
                    .setSource(jsonBuilder().startObject().startObject("mappings").startObject("sequence")
                            .startObject("_source").field("enabled", "0").endObject()
                            .startObject("_all").field("enabled", "0").endObject()
                            .startObject("_type").field("index", "no").endObject()
                            .endObject().endObject().endObject())
                    .get();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
