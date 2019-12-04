package gov.bnl.olog;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.util.MultiValueMap;

import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_INDEX;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.disMaxQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * A utility class for creating a search query for log entires based on time,
 * logboks, tags, properties, description, etc.
 * 
 * @author kunal
 *
 */
public class LogSearchUtil
{

    public static SearchRequest buildSearchRequest(MultiValueMap<String, String> searchParameters)
    {
        SearchRequest searchRequest = new SearchRequest(ES_LOG_INDEX+"*");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        for (Entry<String, List<String>> parameter : searchParameters.entrySet())
        {
            switch (parameter.getKey()) {
            case "desc":
                DisMaxQueryBuilder descQuery = disMaxQuery();
                for (String value : parameter.getValue())
                {
                    for (String pattern : value.split("[\\|,;\\s+]"))
                    {
                        descQuery.add(wildcardQuery("description", pattern.trim()));
                    }
                }
                boolQuery.must(descQuery);
                break;
            case "owner":
                DisMaxQueryBuilder ownerQuery = disMaxQuery();
                for (String value : parameter.getValue())
                {
                    for (String pattern : value.split("[\\|,;\\s+]"))
                    {
                        ownerQuery.add(wildcardQuery("owner", pattern.trim()));
                    }
                }
                boolQuery.must(ownerQuery);
                break;
            case "tags":
                DisMaxQueryBuilder tagQuery = disMaxQuery();
                for (String value : parameter.getValue())
                {
                    for (String pattern : value.split("[\\|,;]"))
                    {
                        tagQuery.add(wildcardQuery("tags.name", pattern.trim()));
                    }
                }
                boolQuery.must(nestedQuery("tags", tagQuery, ScoreMode.None));
                break;
            case "logbooks":
                DisMaxQueryBuilder logbookQuery = disMaxQuery();
                for (String value : parameter.getValue())
                {
                    for (String pattern : value.split("[\\|,;]"))
                    {
                        logbookQuery.add(wildcardQuery("logbooks.name", pattern.trim()));
                    }
                }
                boolQuery.must(nestedQuery("logbooks", logbookQuery, ScoreMode.None));
                break;
            case "properties":
                DisMaxQueryBuilder propertyQuery = disMaxQuery();
                for (String value : parameter.getValue()) {
                    for (String pattern : value.split("[\\|,;]")) {
                        String[] propertySearchFields;
                        if (pattern.contains("."))
                            propertySearchFields = pattern.split(".");
                        else
                            propertySearchFields = new String[] { pattern, "", "" };

                        BoolQueryBuilder bq = boolQuery();
                        if (!propertySearchFields[0].isEmpty())
                        {
                            bq.must(wildcardQuery("properties.name", propertySearchFields[0].trim()));
                        }
                        if (!propertySearchFields[1].isEmpty())
                        {
                            bq.must(nestedQuery("properties.attributes",
                                    wildcardQuery("name", propertySearchFields[1].trim()), ScoreMode.None));
                        }
                        propertyQuery.add(nestedQuery("properties", bq, ScoreMode.None));
                    }
                }
                boolQuery.must(propertyQuery);
                break;
            }

        }
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(100);

        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }
}
