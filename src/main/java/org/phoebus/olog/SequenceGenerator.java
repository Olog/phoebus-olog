/**
 * 
 */
package org.phoebus.olog;

import java.io.IOException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.phoebus.olog.entity.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author Kunal Shroff
 *
 */
@Service
public class SequenceGenerator
{
    @SuppressWarnings("unused")
    @Value("${elasticsearch.log.index:olog_logs}")
    private String ES_LOG_INDEX;

    @Autowired
    @Qualifier("client")
    private ElasticsearchClient client;

    private static long sequenceId = 0;

    @PostConstruct
    public void init()
    {
        Application.logger.config("Initializing the unique sequence id generator");
        FieldSort.Builder fb = new FieldSort.Builder();
        fb.field("id");
        fb.order(SortOrder.Desc);
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(ES_LOG_INDEX)
                .timeout("10s")
                .sort(SortOptions.of(so -> so.field(fb.build())))
                .size(1));
        try{
            SearchResponse<Log> result = client.search(searchRequest, Log.class);
            if(result.hits().hits().size() == 1){
                sequenceId = result.hits().hits().get(0).source().getId();
            }
            Logger.getLogger(SequenceGenerator.class.getName()).log(Level.INFO, "Initialized log entry sequence, next entry is " + (sequenceId + 1));
        }
        catch(Exception e){
            Logger.getLogger(SequenceGenerator.class.getName()).log(Level.INFO, "Failed to determine sequence id", e);
        }
    }

    /**
     * get a new unique id from the olog_sequnce index
     * 
     * @return a new unique id for a olog entry
     * @throws IOException The Elasticsearch client may throw this
     */
    public synchronized  long getID() throws IOException
    {
        return ++sequenceId;
    }
}
