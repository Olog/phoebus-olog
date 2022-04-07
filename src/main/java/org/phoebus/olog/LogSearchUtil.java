package org.phoebus.olog;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.disMaxQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.fuzzyQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

/**
 * A utility class for creating a search query for log entries based on time,
 * logbooks, tags, properties, description, etc.
 *
 * @author Kunal Shroff
 *
 */
@Service
public class LogSearchUtil
{

    final private static String MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    final public static DateTimeFormatter MILLI_FORMAT = DateTimeFormatter.ofPattern(MILLI_PATTERN).withZone(ZoneId.systemDefault());

    @Value("${elasticsearch.log.index:olog_logs}")
    @SuppressWarnings("unused")
    private String ES_LOG_INDEX;
    @Value("${elasticsearch.log.type:olog_log}")
    @SuppressWarnings("unused")
    private String ES_LOG_TYPE;
    @Value("${elasticsearch.result.size.search.default:100}")
    @SuppressWarnings("unused")
    private int defaultSearchSize;
    @Value("${elasticsearch.result.size.search.max:1000}")
    @SuppressWarnings("unused")
    private int maxSearchSize;

    /**
     *
     * @param searchParameters - the various search parameters
     * @return A {@link SearchRequest} based on the provided search parameters
     */
    public SearchRequest buildSearchRequest(MultiValueMap<String, String> searchParameters)
    {
        SearchRequest searchRequest = new SearchRequest(ES_LOG_INDEX+"*");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // The default temporal range for the query
        boolean fuzzySearch = false;
        List<String> searchTerms = new ArrayList<>();
        List<String> titleSearchTerms = new ArrayList<>();
        List<String> levelSearchTerms = new ArrayList<>();
        boolean temporalSearch = false;
        ZonedDateTime start = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.now();
        boolean includeEvents = false;

        int searchResultSize = defaultSearchSize;
        int from = 0;

        // Default sort order
        SortOrder sortOrder = SortOrder.DESC;

        for (Entry<String, List<String>> parameter : searchParameters.entrySet())
        {
            switch (parameter.getKey().strip().toLowerCase()) {
            case "desc":
            case "description":
            case "text":
                for (String value : parameter.getValue())
                {
                    for (String pattern : value.split("[\\|,;\\s+]"))
                    {
                        searchTerms.add(pattern.trim().toLowerCase());
                    }
                }
                break;
            case "title":
                for (String value : parameter.getValue())
                {
                    for (String pattern : value.split("[\\|,;\\s+]"))
                    {
                        titleSearchTerms.add(pattern.trim().toLowerCase());
                    }
                }
                break;
            case "fuzzy":
                fuzzySearch = true;
                break;
            case "phrase":
                DisMaxQueryBuilder exactQuery = disMaxQuery();
                for (String value : parameter.getValue())
                {
                    exactQuery.add(matchPhraseQuery("description", value.trim().toLowerCase()));
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
                ZonedDateTime earliestStartTime = ZonedDateTime.now();
                for (String value : parameter.getValue())
                {
                    ZonedDateTime time = ZonedDateTime.from(MILLI_FORMAT.parse(value));
                    earliestStartTime = earliestStartTime.isBefore(time) ? earliestStartTime : time;
                }
                temporalSearch = true;
                start = earliestStartTime;
                break;
            case "end":
                // If there are multiple end times submitted select the latest
                ZonedDateTime latestEndTime =  Instant.ofEpochMilli(Long.MIN_VALUE).atZone(ZoneId.systemDefault());
                for (String value : parameter.getValue())
                {
                    ZonedDateTime time = ZonedDateTime.from(MILLI_FORMAT.parse(value));
                    latestEndTime = latestEndTime.isBefore(time) ? time : latestEndTime;
                }
                temporalSearch = true;
                end = latestEndTime;
                break;
            case "includeevents":
            case "includeevent":
                includeEvents = true;
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
                            BoolQueryBuilder bq2 = boolQuery();
                            bq2.must(wildcardQuery("properties.attributes.name", propertySearchFields[1].trim()));
                            if (propertySearchFields[2] != null && !propertySearchFields[2].isEmpty()){
                                bq2.must(wildcardQuery("properties.attributes.value", propertySearchFields[2].trim()));
                            }
                            bq.must(nestedQuery("properties.attributes", bq2, ScoreMode.None));
                        }
                        
                        propertyQuery.add(nestedQuery("properties", bq, ScoreMode.None));
                    }
                }
                boolQuery.must(propertyQuery);
                break;
            case "level":
                for (String value : parameter.getValue())
                {
                    for (String pattern : value.split("[\\|,;\\s+]"))
                    {
                        levelSearchTerms.add(pattern.trim().toLowerCase());
                    }
                }
                break;
            case "size":
            case "limit":
                Optional<String> maxSize = parameter.getValue().stream().max((o1, o2) -> {
                    return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
                });
                if (maxSize.isPresent()) {
                    searchResultSize = Integer.valueOf(maxSize.get());
                }
                break;
            case "from":
                Optional<String> maxFrom = parameter.getValue().stream().max((o1, o2) -> {
                    return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
                });
                if (maxFrom.isPresent()) {
                    from = Integer.valueOf(maxFrom.get());
                }
                break;
            case "sort": // Honor sort order if client specifies it
                List<String> sortList = parameter.getValue();
                if(sortList != null && sortList.size() > 0){
                    String sort = sortList.get(0);
                    if(sort.toUpperCase().startsWith("ASC") || sort.toUpperCase().startsWith("UP")){
                        sortOrder = SortOrder.ASC;
                    }
                    else if(sort.toUpperCase().startsWith("DESC") || sort.toUpperCase().startsWith("DOWN")){
                        sortOrder = SortOrder.DESC;
                    }
                }
                break;
            case "attachments":
                DisMaxQueryBuilder attachmentsQuery = disMaxQuery();
                List<String> parameterValues = parameter.getValue();
                if(parameterValues.isEmpty()){ // user does not specify type -> all attachment types
                    attachmentsQuery.add(existsQuery("attachments"));
                }
                else{
                    for (String value : parameterValues)
                    {
                        for (String pattern : value.split("[\\|,;]"))
                        {
                            // only image* and plt types considered
                            pattern = pattern.trim();
                            if(pattern.startsWith("image")){
                                attachmentsQuery.add(wildcardQuery("attachments.fileMetadataDescription", "image*"));
                            }
                            else if(pattern.equals("plt")){
                                attachmentsQuery.add(termsQuery("attachments.fileMetadataDescription", "plt"));
                            }
                        }
                    }
                }
                boolQuery.must(attachmentsQuery);
                break;
            default:
                // Unsupported search parameters are ignored
                break;
            }
        }
        // Add the temporal queries
        if(temporalSearch)
        {
            // check that the start is before the end
            if (start.isBefore(end) || start.equals(end))
            {
                if (includeEvents)
                {
                    DisMaxQueryBuilder temporalQuery = disMaxQuery();
                    // Add a query based on the create time
                    temporalQuery.add(rangeQuery("createdDate").from(1000 * start.toEpochSecond()).to(1000 * end.toEpochSecond()));
                    // Add a query based on the time of the associated events
                    temporalQuery.add(nestedQuery("events",
                                                  rangeQuery("events.instant").from(1000 * start.toEpochSecond()).to(1000 * end.toEpochSecond()),
                                                  ScoreMode.None));
                    boolQuery.must(temporalQuery);
                }
                else {
                    boolQuery.must(rangeQuery("createdDate").from(1000 * start.toEpochSecond()).to(1000 * end.toEpochSecond()));
                }
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Failed to parse search parameters: " + searchParameters + ", CAUSE: Invalid start and end times");
            }
        }
        // Add the description query
        if (!searchTerms.isEmpty())
        {
            DisMaxQueryBuilder descQuery = disMaxQuery();
            if (fuzzySearch)
            {
                searchTerms.stream().forEach(searchTerm -> {
                    descQuery.add(fuzzyQuery("description", searchTerm));
                });
            } else
            {
                searchTerms.stream().forEach(searchTerm -> {
                    descQuery.add(wildcardQuery("description", searchTerm));
                });
            }
            boolQuery.must(descQuery);
        }
        // Add the title query
        if (!titleSearchTerms.isEmpty())
        {
            DisMaxQueryBuilder titleQuery = disMaxQuery();
            if (fuzzySearch)
            {
                titleSearchTerms.stream().forEach(searchTerm -> {
                    titleQuery.add(fuzzyQuery("title", searchTerm));
                });
            } else
            {
                titleSearchTerms.stream().forEach(searchTerm -> {
                    titleQuery.add(wildcardQuery("title", searchTerm));
                });
            }
            boolQuery.must(titleQuery);
        }
        // Add the level query
        if (!levelSearchTerms.isEmpty())
        {
            DisMaxQueryBuilder levelQuery = disMaxQuery();
            if (fuzzySearch)
            {
                levelSearchTerms.stream().forEach(searchTerm -> {
                    levelQuery.add(fuzzyQuery("level", searchTerm));
                });
            } else
            {
                levelSearchTerms.stream().forEach(searchTerm -> {
                    levelQuery.add(wildcardQuery("level", searchTerm));
                });
            }
            boolQuery.must(levelQuery);
        }

        searchSourceBuilder.sort("createdDate", sortOrder);
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(Math.min(searchResultSize, maxSearchSize));
        searchSourceBuilder.from(Math.max(0, from));
        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }
}
