package org.phoebus.olog;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FuzzyQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * A utility class for creating a search query for log entries based on time,
 * logbooks, tags, properties, description, etc.
 *
 * @author Kunal Shroff
 */
@Service
public class LogSearchUtil {

    final private static String MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    final public static DateTimeFormatter MILLI_FORMAT = DateTimeFormatter.ofPattern(MILLI_PATTERN).withZone(ZoneId.systemDefault());

    @SuppressWarnings("unused")
    @Value("${elasticsearch.log.index:olog_logs}")
    private String ES_LOG_INDEX;
    @Value("${elasticsearch.log.type:olog_log}")
    @SuppressWarnings("unused")
    private String ES_LOG_TYPE;
    @Value("${elasticsearch.result.size.search.default:100}")
    private int defaultSearchSize;
    @SuppressWarnings("unused")
    @Value("${elasticsearch.result.size.search.max:1000}")
    private int maxSearchSize;

    /**
     * @param searchParameters - the various search parameters
     * @return A {@link SearchRequest} based on the provided search parameters
     */
    public SearchRequest buildSearchRequest(MultiValueMap<String, String> searchParameters) {
        BoolQuery.Builder boolQueryBuilder = new Builder();
        boolean fuzzySearch = false;
        List<String> searchTerms = new ArrayList<>();
        List<String> phraseSearchTerms = new ArrayList<>();
        List<String> titleSearchTerms = new ArrayList<>();
        boolean temporalSearch = false;
        boolean includeEvents = false;
        ZonedDateTime start = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.now();
        List<String> levelSearchTerms = new ArrayList<>();
        int searchResultSize = defaultSearchSize;
        int from = 0;

        // Default sort order
        SortOrder sortOrder = null;

        for (Entry<String, List<String>> parameter : searchParameters.entrySet()) {
            switch (parameter.getKey().strip().toLowerCase()) {
                case "desc":
                case "description":
                case "text":
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;\\s+]")) {
                            String term = pattern.trim().toLowerCase();
                            // Quoted strings will be mapped to a phrase query
                            if (term.startsWith("\"") && term.endsWith("\"")) {
                                phraseSearchTerms.add(term.substring(1, term.length() - 1));
                            } else {
                                searchTerms.add(term);
                            }
                        }
                    }
                    break;
                case "title":
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;\\s+]")) {
                            titleSearchTerms.add(pattern.trim().toLowerCase());
                        }
                    }
                    break;
                case "fuzzy":
                    fuzzySearch = true;
                    break;
                case "phrase":
                    DisMaxQuery.Builder phraseQuery = new DisMaxQuery.Builder();
                    List<Query> phraseQueries = new ArrayList<>();
                    for (String value : parameter.getValue()) {
                        phraseQueries.add(MatchPhraseQuery.of(m -> m.field("description").query(value.trim().toLowerCase()))._toQuery());
                    }
                    phraseQuery.queries(phraseQueries);
                    boolQueryBuilder.must(phraseQuery.build()._toQuery());
                    break;
                case "owner":
                    DisMaxQuery.Builder ownerQuery = new DisMaxQuery.Builder();
                    List<Query> ownerQueries = new ArrayList<>();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;\\s+]")) {
                            ownerQueries.add(WildcardQuery.of(w -> w.field("owner").value(pattern.trim()))._toQuery());
                        }
                    }
                    ownerQuery.queries(ownerQueries);
                    boolQueryBuilder.must(ownerQuery.build()._toQuery());
                    break;
                case "tags":
                    DisMaxQuery.Builder tagQuery = new DisMaxQuery.Builder();
                    List<Query> tagsQueries = new ArrayList<>();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            tagsQueries.add(WildcardQuery.of(w -> w.field("tags.name").value(pattern.trim()))._toQuery());
                        }
                    }
                    Query tagsQuery = tagQuery.queries(tagsQueries).build()._toQuery();
                    NestedQuery nestedTagsQuery = NestedQuery.of(n -> n.path("tags").query(tagsQuery));
                    boolQueryBuilder.must(nestedTagsQuery._toQuery());
                    break;
                case "logbooks":
                    DisMaxQuery.Builder logbookQuery = new DisMaxQuery.Builder();
                    List<Query> logbooksQueries = new ArrayList<>();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            logbooksQueries.add(WildcardQuery.of(w -> w.field("logbooks.name").value(pattern.trim()))._toQuery());
                        }
                    }
                    Query logbooksQuery = logbookQuery.queries(logbooksQueries).build()._toQuery();
                    NestedQuery nestedLogbooksQuery = NestedQuery.of(n -> n.path("logbooks").query(logbooksQuery).scoreMode(ChildScoreMode.None));
                    boolQueryBuilder.must(nestedLogbooksQuery._toQuery());
                    break;
                case "start":
                    // If there are multiple start times submitted select the earliest
                    ZonedDateTime earliestStartTime = ZonedDateTime.now();
                    for (String value : parameter.getValue()) {
                        ZonedDateTime time = ZonedDateTime.from(MILLI_FORMAT.parse(value));
                        earliestStartTime = earliestStartTime.isBefore(time) ? earliestStartTime : time;
                    }
                    temporalSearch = true;
                    start = earliestStartTime;
                    break;
                case "end":
                    // If there are multiple end times submitted select the latest
                    ZonedDateTime latestEndTime = Instant.ofEpochMilli(Long.MIN_VALUE).atZone(ZoneId.systemDefault());
                    for (String value : parameter.getValue()) {
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
                    DisMaxQuery.Builder propertyQuery = new DisMaxQuery.Builder();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            String[] propertySearchFields;
                            propertySearchFields = Arrays.copyOf(pattern.split("\\."), 3);
                            BoolQuery.Builder bqb = new BoolQuery.Builder();
                            if (propertySearchFields[0] != null && !propertySearchFields[0].isEmpty()) {
                                bqb.must(WildcardQuery.of(w -> w.field("properties.name").value(propertySearchFields[0].trim()))._toQuery());
                            }

                            if (propertySearchFields[1] != null && !propertySearchFields[1].isEmpty()) {
                                BoolQuery.Builder bqb2 = new BoolQuery.Builder();
                                bqb2.must(WildcardQuery.of(w -> w.field("properties.attributes.name").value(propertySearchFields[1].trim()))._toQuery());
                                if (propertySearchFields[2] != null && !propertySearchFields[2].isEmpty()) {
                                    bqb2.must(WildcardQuery.of(w -> w.field("properties.attributes.value").value(propertySearchFields[2].trim()))._toQuery());
                                }
                                bqb.must(NestedQuery.of(n -> n.path("properties.attributes").query(bqb2.build()._toQuery()).scoreMode(ChildScoreMode.None))._toQuery());
                            }
                            propertyQuery.queries(q -> q.nested(NestedQuery.of(n -> n.path("properties").query(bqb.build()._toQuery()).scoreMode(ChildScoreMode.None))));
                        }
                    }
                    boolQueryBuilder.must(propertyQuery.build()._toQuery());
                    break;
                case "level":
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;\\s+]")) {
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
                    if (sortList != null && sortList.size() > 0) {
                        String sort = sortList.get(0);
                        if (sort.toUpperCase().startsWith("ASC") || sort.toUpperCase().startsWith("UP")) {
                            sortOrder = SortOrder.Asc;
                        } else if (sort.toUpperCase().startsWith("DESC") || sort.toUpperCase().startsWith("DOWN")) {
                            sortOrder = SortOrder.Desc;
                        }
                    }
                    break;
                case "attachments":
                    DisMaxQuery.Builder attachmentsQuery = new DisMaxQuery.Builder();
                    List<String> parameterValues = parameter.getValue();
                    boolean searchAll = false;
                    // If query string contains attachments= or attachments=all, then this overrides any
                    // other parameter values and results in a search for entries with at least one attachment.
                    for (String value : parameterValues) {
                        for (String pattern : value.split("[\\|,;]")) {
                            String parameterValue = pattern.trim();
                            if ("all".equals(parameterValue) || parameterValue.isEmpty()) {
                                attachmentsQuery.queries(ExistsQuery.of(e -> e.field("attachments"))._toQuery());
                                searchAll = true;
                                break;
                            } else {
                                attachmentsQuery.queries(WildcardQuery.of(w -> w.field("attachments.fileMetadataDescription").value(pattern.trim()))._toQuery());
                            }
                        }
                        if (searchAll) { // search all -> ignore other parameter values
                            break;
                        }
                    }
                    boolQueryBuilder.must(attachmentsQuery.build()._toQuery());
                    break;
                default:
                    // Unsupported search parameters are ignored
                    break;
            }
        }

        // Add the temporal queries
        if (temporalSearch) {
            // check that the start is before the end
            if (start.isBefore(end) || start.equals(end)) {
                DisMaxQuery.Builder temporalQuery = new DisMaxQuery.Builder();
                RangeQuery.Builder rangeQuery = new RangeQuery.Builder();
                // Add a query based on the create time
                rangeQuery.field("createdDate").from(Long.toString(1000 * start.toEpochSecond()))
                        .to(Long.toString(1000 * end.toEpochSecond()));
                if (includeEvents) {
                    RangeQuery.Builder eventsRangeQuery = new RangeQuery.Builder();
                    // Add a query based on the time of the associated events
                    eventsRangeQuery.field("events.instant").from(Long.toString(1000 * start.toEpochSecond()))
                            .to(Long.toString(1000 * end.toEpochSecond()));
                    NestedQuery.Builder nestedQuery = new NestedQuery.Builder();
                    nestedQuery.path("events").query(eventsRangeQuery.build()._toQuery());

                    temporalQuery.queries(rangeQuery.build()._toQuery(), nestedQuery.build()._toQuery());
                    boolQueryBuilder.must(temporalQuery.build()._toQuery());
                } else {
                    boolQueryBuilder.must(rangeQuery.build()._toQuery());
                }
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Failed to parse search parameters: " + searchParameters + ", CAUSE: Invalid start and end times");
            }
        }

        // Add the description query. Multiple search terms will be AND:ed.
        if (!searchTerms.isEmpty()) {
            if (fuzzySearch) {
                searchTerms.stream().forEach(searchTerm -> {
                    boolQueryBuilder.must(FuzzyQuery.of(f -> f.field("description").value(searchTerm))._toQuery());
                });
            } else {
                searchTerms.stream().forEach(searchTerm -> {
                    boolQueryBuilder.must(WildcardQuery.of(w -> w.field("description").value(searchTerm))._toQuery());
                });
            }
        }

        // Add phrase queries for description key. Multiple search terms will be AND:ed.
        if (!phraseSearchTerms.isEmpty()) {
            phraseSearchTerms.stream().forEach(phraseSearchTerm -> {
                boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("description").query(phraseSearchTerm))._toQuery());
            });
        }

        // Add the title query
        if (!titleSearchTerms.isEmpty()) {
            DisMaxQuery.Builder titleQuery = new DisMaxQuery.Builder();
            List<Query> titleQueries = new ArrayList<>();
            if (fuzzySearch) {
                titleSearchTerms.stream().forEach(searchTerm -> {
                    titleQueries.add(FuzzyQuery.of(f -> f.field("title").value(searchTerm))._toQuery());
                });
            } else {
                titleSearchTerms.stream().forEach(searchTerm -> {
                    titleQueries.add(WildcardQuery.of(w -> w.field("title").value(searchTerm))._toQuery());
                });
            }
            titleQuery.queries(titleQueries);
            boolQueryBuilder.must(titleQuery.build()._toQuery());
        }

        // Add the level query
        if (!levelSearchTerms.isEmpty()) {
            DisMaxQuery.Builder levelQuery = new DisMaxQuery.Builder();
            List<Query> levelQueries = new ArrayList<>();
            if (fuzzySearch) {
                levelSearchTerms.stream().forEach(searchTerm -> {
                    levelQueries.add(FuzzyQuery.of(f -> f.field("level").value(searchTerm))._toQuery());
                });
            } else {
                levelSearchTerms.stream().forEach(searchTerm -> {
                    levelQueries.add(WildcardQuery.of(w -> w.field("level").value(searchTerm))._toQuery());
                });
            }
            levelQuery.queries(levelQueries);
            boolQueryBuilder.must(levelQuery.build()._toQuery());
        }

        int _searchResultSize = searchResultSize;
        int _from = from;
        FieldSort.Builder fb = new FieldSort.Builder();
        fb.field("createdDate");
        fb.order(sortOrder);

        return SearchRequest.of(s -> s.index(ES_LOG_INDEX)
                .query(boolQueryBuilder.build()._toQuery())
                .timeout("60s")
                .sort(SortOptions.of(so -> so.field(fb.build())))
                .size(Math.min(_searchResultSize, maxSearchSize))
                .from(_from));
    }
}
