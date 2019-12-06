package gov.bnl.olog;

import static gov.bnl.olog.OlogResourceDescriptors.ES_LOG_INDEX;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.disMaxQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.util.MultiValueMap;

/**
 * A utility class for creating a search query for log entires based on time,
 * logboks, tags, properties, description, etc.
 * 
 * @author kunal
 *
 */
public class LogSearchUtil
{

    final private static String MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    final public static DateTimeFormatter MILLI_FORMAT = DateTimeFormatter.ofPattern(MILLI_PATTERN).withZone(ZoneId.systemDefault());

    public static SearchRequest buildSearchRequest(MultiValueMap<String, String> searchParameters)
    {
        SearchRequest searchRequest = new SearchRequest(ES_LOG_INDEX+"*");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // The default temporal range for the query
        boolean temporalQuery = false;
        Instant start = Instant.EPOCH;
        Instant end = Instant.now();

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
            case "phrase":
                DisMaxQueryBuilder exactQuery = disMaxQuery();
                for (String value : parameter.getValue())
                {
                    exactQuery.add(matchPhraseQuery("description", value.trim()));
                }
                boolQuery.must(exactQuery);
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
            case "start":
                // If there are multiple start times submitted select the earliest
                Instant earliestStartTime = Instant.now();
                for (String value : parameter.getValue())
                {
                    Instant time = Instant.from(MILLI_FORMAT.parse(value));
                    earliestStartTime = earliestStartTime.isBefore(time) ? earliestStartTime : time;
                }
                temporalQuery = true;
                start = earliestStartTime;
                break;
            case "end":
                // If there are multiple end times submitted select the latest
                Instant latestEndTime = Instant.MIN;
                for (String value : parameter.getValue())
                {
                    Instant time = Instant.from(MILLI_FORMAT.parse(value));
                    latestEndTime = latestEndTime.isBefore(time) ? time : latestEndTime;
                }
                temporalQuery = true;
                end = latestEndTime;
                break;
            case "properties":
                DisMaxQueryBuilder propertyQuery = disMaxQuery();
                for (String value : parameter.getValue()) {
                    for (String pattern : value.split("[\\|,;]")) {
                        String[] propertySearchFields;
                        propertySearchFields = Arrays.copyOf(pattern.split("\\."), 3);

                        BoolQueryBuilder bq = boolQuery();
                        if (propertySearchFields[0] != null && !propertySearchFields[0].isEmpty())
                        {
                            bq.must(wildcardQuery("properties.name", propertySearchFields[0].trim()));
                        }
                        if (propertySearchFields[1] != null && !propertySearchFields[1].isEmpty())
                        {
                            bq.must(nestedQuery("properties.attributes",
                                    wildcardQuery("properties.attributes.name", propertySearchFields[1].trim()), ScoreMode.None));
                        }
                        if (propertySearchFields[2] != null && !propertySearchFields[2].isEmpty())
                        {
                            bq.must(nestedQuery("properties.attributes",
                                    wildcardQuery("properties.attributes.value", propertySearchFields[2].trim()), ScoreMode.None));
                        }
                        propertyQuery.add(nestedQuery("properties", bq, ScoreMode.None));
                    }
                }
                boolQuery.must(propertyQuery);
                break;
            }
        }
        // Add the temporal queries
        if(temporalQuery)
        {
            // check that the start is before the end
            if (start.isBefore(end))
            {
                boolQuery.must(rangeQuery("createdDate").from(start.toEpochMilli()).to(end.toEpochMilli()));
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
