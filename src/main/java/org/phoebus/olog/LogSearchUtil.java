package org.phoebus.olog;

import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.DateRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DateDecayFunction;
import co.elastic.clients.elasticsearch._types.query_dsl.DecayFunction;
import co.elastic.clients.elasticsearch._types.query_dsl.DecayPlacement;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FuzzyQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import org.phoebus.util.time.TimeParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A utility class for creating a search query for log entries based on time,
 * logbooks, tags, properties, description, etc.
 *
 * @author Kunal Shroff
 */
@Service
public class LogSearchUtil {


    @SuppressWarnings("unused")
    @Value("${elasticsearch.log.index:olog_logs}")
    private String ES_LOG_INDEX;
    @Value("${elasticsearch.log.type:olog_log}")
    @SuppressWarnings("unused")
    private String ES_LOG_TYPE;
    @SuppressWarnings("unused")
    @Value("${elasticsearch.result.size.search.default:100}")
    private int defaultSearchSize;
    @SuppressWarnings("unused")
    @Value("${elasticsearch.result.size.search.max:1000}")
    private int maxSearchSize;

    private static final Logger LOGGER = Logger.getLogger(LogSearchUtil.class.getName());
    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    private static final String DESCRIPTION = "description";
    private static final String TITLE = "title";
    private static final String EPOCH_SECOND = "epoch_second";
    private static final String CREATED_DATE = "createdDate";
    private static final String TAGS = "tags";
    private static final String LOGBOOKS = "logbooks";
    private static final String LEVEL = "level";
    private static final String START = "start";
    private static final String END = "end";
    private static final String OWNER = "owner";
    private static final String PROPERTIES = "properties";
    private static final String ATTACHMENTS = "attachments";
    private static final String TEXT = "text";
    private static final String DESC = "desc";
    private static final String FUZZY = "fuzzy";
    private static final String PHRASE = "phrase";
    private static final String SIZE = "size";
    private static final String LIMIT = "limit";
    private static final String FROM = "from";
    private static final String SORT = "sort";
    private static final String INCLUDE_EVENTS = "includeevents";

    /**
     * @param searchParameters - the various search parameters
     * @return A {@link SearchRequest} based on the provided search parameters
     */
    public SearchRequest buildSearchRequest(MultiValueMap<String, String> searchParameters) {
        TimeZone timeZone = getTimezone(searchParameters);
        BoolQuery.Builder boolQueryBuilder = new Builder();
        boolean fuzzySearch = false;
        List<String> searchTerms = new ArrayList<>();
        List<String> descriptionPhraseSearchTerms = new ArrayList<>();
        List<String> titleSearchTerms = new ArrayList<>();
        List<String> titlePhraseSearchTerms = new ArrayList<>();
        boolean temporalSearch = false;
        boolean includeEvents = false;
        ZonedDateTime start = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.now();
        List<String> levelSearchTerms = new ArrayList<>();
        List<String> levelPhraseSearchTerms = new ArrayList<>();
        int searchResultSize = defaultSearchSize;
        int from = 0;

        // Default sort order
        SortOrder sortOrder = null;

        boolean hasDescriptionOrTitle = searchParameters.keySet().stream()
                .map(k -> k.strip().toLowerCase())
                .anyMatch(k -> k.equals(DESC) || k.equals(DESCRIPTION) || k.equals(TEXT) || k.equals(TITLE));

        for (Entry<String, List<String>> parameter : searchParameters.entrySet()) {
            switch (parameter.getKey().strip().toLowerCase()) {
                case "query":
                    if (!hasDescriptionOrTitle) {
                        return getFreeTextSearchRequest(searchParameters);
                    }
                    break;
                case DESC:
                case DESCRIPTION:
                case TEXT:
                    for (String value : parameter.getValue()) {
                        for (String pattern : getSearchTerms(value)) {
                            String term = pattern.trim().toLowerCase();
                            // Quoted strings will be mapped to a phrase query
                            if (term.startsWith("\"") && term.endsWith("\"")) {
                                descriptionPhraseSearchTerms.add(term.substring(1, term.length() - 1));
                            } else {
                                searchTerms.add(term);
                            }
                        }
                    }
                    break;
                case TITLE:
                    for (String value : parameter.getValue()) {
                        for (String pattern : getSearchTerms(value)) {
                            String term = pattern.trim().toLowerCase();
                            // Quoted strings will be mapped to a phrase query
                            if (term.startsWith("\"") && term.endsWith("\"")) {
                                titlePhraseSearchTerms.add(term.substring(1, term.length() - 1));
                            } else {
                                titleSearchTerms.add(term);
                            }
                        }
                    }
                    break;
                case FUZZY:
                    fuzzySearch = true;
                    break;
                case PHRASE:
                    DisMaxQuery.Builder phraseQuery = new DisMaxQuery.Builder();
                    List<Query> phraseQueries = new ArrayList<>();
                    for (String value : parameter.getValue()) {
                        phraseQueries.add(MatchPhraseQuery.of(m -> m.field(DESCRIPTION).query(value.trim().toLowerCase()))._toQuery());
                    }
                    phraseQuery.queries(phraseQueries);
                    boolQueryBuilder.must(phraseQuery.build()._toQuery());
                    break;
                case OWNER:
                    DisMaxQuery.Builder ownerQuery = new DisMaxQuery.Builder();
                    List<Query> ownerQueries = new ArrayList<>();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;\\s+]")) {
                            ownerQueries.add(WildcardQuery.of(w -> w.field(OWNER)
                                    .caseInsensitive(true)
                                    .value(pattern.trim()))._toQuery());
                        }
                    }
                    ownerQuery.queries(ownerQueries);
                    boolQueryBuilder.must(ownerQuery.build()._toQuery());
                    break;
                case TAGS:
                    boolQueryBuilder.must(buildNestedWildcardQuery(TAGS, TAGS + ".name", parameter));
                    break;
                case LOGBOOKS:
                    boolQueryBuilder.must(buildNestedWildcardQuery(LOGBOOKS, LOGBOOKS + ".name", parameter));
                    break;
                case START:
                    ZonedDateTime startTime = determineDateAndTime(parameter, timeZone);
                    start = startTime != null ? startTime : ZonedDateTime.now();
                    temporalSearch = true;
                    break;
                case END:
                    ZonedDateTime endTime = determineDateAndTime(parameter, timeZone);
                    end = endTime != null ? endTime : Instant.ofEpochMilli(Long.MIN_VALUE).atZone(ZoneId.systemDefault());
                    temporalSearch = true;
                    break;
                case INCLUDE_EVENTS:
                case "includeevent":
                    includeEvents = true;
                    break;
                case PROPERTIES:
                    DisMaxQuery.Builder propertyQuery = new DisMaxQuery.Builder();
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            String[] propertySearchFields;
                            propertySearchFields = Arrays.copyOf(pattern.split("\\."), 3);
                            BoolQuery.Builder bqb = new BoolQuery.Builder();
                            if (propertySearchFields[0] != null && !propertySearchFields[0].isEmpty()) {
                                bqb.must(WildcardQuery.of(w -> w.field("properties.name")
                                        .caseInsensitive(true)
                                        .value(propertySearchFields[0].trim()))._toQuery());
                            }
                            if (propertySearchFields[1] != null && !propertySearchFields[1].isEmpty()) {
                                BoolQuery.Builder bqb2 = new BoolQuery.Builder();
                                bqb2.must(WildcardQuery.of(w -> w.field("properties.attributes.name")
                                        .caseInsensitive(true)
                                        .value(propertySearchFields[1].trim()))._toQuery());
                                if (propertySearchFields[2] != null && !propertySearchFields[2].isEmpty()) {
                                    bqb2.must(WildcardQuery.of(w -> w.field("properties.attributes.value")
                                            .caseInsensitive(true)
                                            .value(propertySearchFields[2].trim()))._toQuery());
                                }
                                bqb.must(NestedQuery.of(n -> n.path("properties.attributes").query(bqb2.build()._toQuery()).scoreMode(ChildScoreMode.None))._toQuery());
                            }
                            propertyQuery.queries(q -> q.nested(NestedQuery.of(n -> n.path("properties").query(bqb.build()._toQuery()).scoreMode(ChildScoreMode.None))));
                        }
                    }
                    boolQueryBuilder.must(propertyQuery.build()._toQuery());
                    break;
                case LEVEL:
                    for (String value : parameter.getValue()) {
                        for (String pattern : value.split("[\\|,;]")) {
                            String term = pattern.trim().toLowerCase();
                            // Quoted strings, or string containing space chars, will be mapped to a phrase query
                            if ((term.startsWith("\"") && term.endsWith("\""))) {
                                levelPhraseSearchTerms.add(term.substring(1, term.length() - 1));
                            } else if (term.contains(" ")) {
                                levelPhraseSearchTerms.add(term);
                            } else {
                                levelSearchTerms.add(term);
                            }
                        }
                    }
                    break;
                case SIZE:
                case LIMIT:
                    Optional<String> maxSize = parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
                    if (maxSize.isPresent()) {
                        try {
                            searchResultSize = Integer.valueOf(maxSize.get());
                        } catch (NumberFormatException e) {
                            LOGGER.log(Level.WARNING, () -> MessageFormat.format(TextUtil.SEARCH_CANNOT_PARSE_SIZE_VALUE, maxSize.get()));
                        }
                    }
                    break;
                case FROM:
                    Optional<String> maxFrom = parameter.getValue().stream().max(Comparator.comparing(Integer::valueOf));
                    if (maxFrom.isPresent()) {
                        try {
                            from = Integer.valueOf(maxFrom.get());
                        } catch (NumberFormatException e) {
                            LOGGER.log(Level.WARNING, () -> MessageFormat.format(TextUtil.SEARCH_CANNOT_PARSE_FROM_VALUE, maxFrom.get()));
                        }
                    }
                    break;
                case SORT:
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
                case ATTACHMENTS:
                    DisMaxQuery.Builder attachmentsQuery = new DisMaxQuery.Builder();
                    attachmentsQuery.queries(Collections.emptyList());
                    List<String> parameterValues = parameter.getValue();
                    boolean searchAll = false;
                    for (String value : parameterValues) {
                        for (String pattern : value.split("[\\|,;]")) {
                            // No value for attachments -> search for existence of attachment, regardless of name and type
                            if (pattern == null || "null".equals(pattern) || pattern.isEmpty()) {
                                attachmentsQuery.queries(ExistsQuery.of(e -> e.field(ATTACHMENTS))._toQuery());
                                searchAll = true;
                                break;
                            } else {
                                attachmentsQuery.queries(WildcardQuery.of(m -> m.field(ATTACHMENTS + ".filename").caseInsensitive(true).value(pattern.trim()))._toQuery());
                            }
                        }
                        if (searchAll) { // search all -> ignore other parameter values
                            break;
                        }
                    }
                    DisMaxQuery disMaxQuery = attachmentsQuery.build();
                    if (!disMaxQuery.queries().isEmpty()) {
                        boolQueryBuilder.must(disMaxQuery._toQuery());
                    }
                    //boolQueryBuilder.must(attachmentsQuery.build()._toQuery());
                    break;
                default:
                    // Unsupported search parameters are ignored
                    break;
            }
        }

        ZonedDateTime _start = start;
        ZonedDateTime _end = end;
        // Add the temporal queries
        if (temporalSearch) {
            // check that the start is before the end
            if (start.isBefore(end) || start.equals(end)) {
                DisMaxQuery.Builder temporalQuery = new DisMaxQuery.Builder();
                RangeQuery.Builder rangeQuery = new RangeQuery.Builder();
                // Add a query based on the create time
                rangeQuery.date(DateRangeQuery.of(b ->
                        b.field(CREATED_DATE).gte(JsonData.of(_start.toEpochSecond()).toString())
                                .lte(JsonData.of(_end.toEpochSecond()).toString())
                                        .format(EPOCH_SECOND)));
                if (includeEvents) {
                    RangeQuery.Builder eventsRangeQuery = new RangeQuery.Builder();
                    // Add a query based on the time of the associated events
                    eventsRangeQuery.date(DateRangeQuery.of(b ->
                            b.field(CREATED_DATE).gte(JsonData.of(_start.toEpochSecond()).toString())
                                    .lte(JsonData.of(_end.toEpochSecond()).toString())
                                    .format(EPOCH_SECOND)));
                    NestedQuery.Builder nestedQuery = new NestedQuery.Builder();
                    nestedQuery.path("events").query(eventsRangeQuery.build()._toQuery());

                    temporalQuery.queries(rangeQuery.build()._toQuery(), nestedQuery.build()._toQuery());
                    boolQueryBuilder.must(temporalQuery.build()._toQuery());
                } else {
                    boolQueryBuilder.must(rangeQuery.build()._toQuery());
                }
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        MessageFormat.format(TextUtil.SEARCH_FAILED_PARSE_PARAMETERS_INVALID_START_END, searchParameters));
            }
        }

        // Add the description query. Multiple search terms will be AND:ed.
        if (!searchTerms.isEmpty()) {
            if (fuzzySearch) {
                searchTerms.stream().forEach(searchTerm ->
                        boolQueryBuilder.must(FuzzyQuery.of(f -> f.field(DESCRIPTION).value(searchTerm))._toQuery())
                );
            } else {
                searchTerms.stream().forEach(searchTerm ->
                        boolQueryBuilder.must(WildcardQuery.of(w -> w.field(DESCRIPTION).value(searchTerm))._toQuery())
                );
            }
        }

        // Add phrase queries for description key. Multiple search terms will be AND:ed.
        if (!descriptionPhraseSearchTerms.isEmpty()) {
            descriptionPhraseSearchTerms.stream().forEach(phraseSearchTerm ->
                    boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field(DESCRIPTION).query(phraseSearchTerm))._toQuery())
            );
        }

        // Add the title query. Multiple search terms will be AND:ed.
        if (!titleSearchTerms.isEmpty()) {
            if (fuzzySearch) {
                titleSearchTerms.stream().forEach(searchTerm ->
                        boolQueryBuilder.must(FuzzyQuery.of(f -> f.field(TITLE).value(searchTerm))._toQuery())
                );
            } else {
                titleSearchTerms.stream().forEach(searchTerm ->
                        boolQueryBuilder.must(WildcardQuery.of(w -> w.field(TITLE).value(searchTerm))._toQuery())
                );
            }
        }

        // Add phrase queries for title key. Multiple search terms will be AND:ed.
        if (!titlePhraseSearchTerms.isEmpty()) {
            titlePhraseSearchTerms.stream().forEach(phraseSearchTerm ->
                    boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field(TITLE).query(phraseSearchTerm))._toQuery())
            );
        }

        List<Query> levelQueries = new ArrayList<>();
        DisMaxQuery.Builder levelQuery = new DisMaxQuery.Builder();
        // Add the level query
        if (!levelSearchTerms.isEmpty()) {
            if (fuzzySearch) {
                levelSearchTerms.stream().forEach(searchTerm ->
                        levelQueries.add(FuzzyQuery.of(f -> f.field(LEVEL).value(searchTerm))._toQuery())
                );
            } else {
                levelSearchTerms.stream().forEach(searchTerm ->
                        levelQueries.add(WildcardQuery.of(w -> w.field(LEVEL).value(searchTerm))._toQuery())
                );
            }

        }

        // Add phrase queries for level key. Multiple search terms will be AND:ed.
        if (!levelPhraseSearchTerms.isEmpty()) {
            levelPhraseSearchTerms.stream().forEach(phraseSearchTerm ->
                    levelQueries.add(MatchPhraseQuery.of(m -> m.field(LEVEL).query(phraseSearchTerm))._toQuery())
            );
        }

        // Level query may be a mix of quoted and unquoted terms, combine them here
        if (!levelQueries.isEmpty()) {
            levelQuery.queries(levelQueries);
            boolQueryBuilder.must(levelQuery.build()._toQuery());
        }

        int _searchResultSize = searchResultSize;
        int _from = from;
        FieldSort.Builder fb = new FieldSort.Builder();
        fb.field(CREATED_DATE);
        fb.order(sortOrder);

        return SearchRequest.of(s -> s.index(ES_LOG_INDEX)
                .query(boolQueryBuilder.build()._toQuery())
                .timeout("60s")
                .sort(SortOptions.of(so -> so.field(fb.build())))
                .size(Math.min(_searchResultSize, maxSearchSize))
                .from(_from));
    }

    /**
     * Parses a search query terms string into a string array. In particular,
     * quoted search terms must be maintained even if they contain the
     * separator chars used to tokenize the terms.
     *
     * @param searchQueryTerms String as specified by client
     * @return A {@link List} of search terms, some of which may be
     * quoted. Is void of any zero-length strings.
     */
    public List<String> getSearchTerms(String searchQueryTerms) {
        // Count double quote chars. Odd number of quote chars
        // is not supported -> throw exception
        long quoteCount = searchQueryTerms.chars().filter(c -> c == '\"').count();
        if (quoteCount == 0) {
            return Arrays.stream(searchQueryTerms.split("[\\|,;\\s+]")).filter(t -> t.length() > 0).collect(Collectors.toList());
        }
        if (quoteCount % 2 == 1) {
            throw new IllegalArgumentException(TextUtil.SEARCH_UNBALANCED_QUOTES);
        }
        // If we come this far then at least one quoted term is
        // contained in user input
        List<String> terms = new ArrayList<>();
        int nextStartIndex = searchQueryTerms.indexOf('\"');
        while (nextStartIndex >= 0) {
            int endIndex = searchQueryTerms.indexOf('\"', nextStartIndex + 1);
            String quotedTerm = searchQueryTerms.substring(nextStartIndex, endIndex + 1);
            terms.add(quotedTerm);
            // Remove the quoted term from user input
            searchQueryTerms = searchQueryTerms.replace(quotedTerm, "");
            // Check next occurrence
            nextStartIndex = searchQueryTerms.indexOf('\"');
        }
        // Add remaining terms...
        List<String> remaining = Arrays.asList(searchQueryTerms.split("[\\|,;\\s+]"));
        //...but remove empty strings, which are "leftovers" when quoted terms are removed
        terms.addAll(remaining.stream().filter(t -> t.length() > 0).collect(Collectors.toList()));
        return terms;
    }

    protected Query buildWildcardDisMaxQuery(String field, Entry<String, List<String>> parameter) {
        List<Query> queries = new ArrayList<>();
        for (String value : parameter.getValue()) {
            for (String pattern : value.split("[\\|,;]")) {
                queries.add(WildcardQuery.of(w -> w.field(field)
                        .caseInsensitive(true)
                        .value(pattern.trim()))._toQuery());
            }
        }
        return new DisMaxQuery.Builder().queries(queries).build()._toQuery();
    }

    protected Query buildNestedWildcardQuery(String nestedPath, String field, Entry<String, List<String>> parameter) {
        Query innerQuery = buildWildcardDisMaxQuery(field, parameter);
        return NestedQuery.of(n -> n.path(nestedPath).query(innerQuery).scoreMode(ChildScoreMode.None))._toQuery();
    }

    private SearchRequest getFreeTextSearchRequest(MultiValueMap<String, String> searchParameters) {
        TimeZone timeZone = getTimezone(searchParameters);
        BoolQuery.Builder builder = new Builder();

        boolean temporalSearch = false;
        ZonedDateTime start = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        ZonedDateTime end = ZonedDateTime.now();

        String sizeParam = searchParameters.containsKey(SIZE) ? searchParameters.getFirst(SIZE) : searchParameters.getFirst(LIMIT);
        int size = sizeParam != null ? Integer.parseInt(sizeParam) : defaultSearchSize;
        int from = searchParameters.containsKey(FROM) ? Integer.parseInt(searchParameters.getFirst(FROM)) : 0;

        for (Entry<String, List<String>> parameter : searchParameters.entrySet()) {
            switch (parameter.getKey().strip().toLowerCase()) {
                case "query":
                    String query = parameter.getValue().get(0);
                    MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m ->
                            m.query(query)
                                .type(TextQueryType.BestFields)
                                .fuzziness("AUTO")
                                .prefixLength(2)
                                .fields(List.of(
                                    "title^3",
                                    "title.stemmed^2",
                                    "description^1.5",
                                    "description.stemmed",
                                    "owner^2"
                                )));
                    builder.must(multiMatchQuery._toQuery());
                    break;
                case TAGS:
                    builder.filter(buildNestedWildcardQuery(TAGS, TAGS + ".name", parameter));
                    break;
                case LOGBOOKS:
                    builder.filter(buildNestedWildcardQuery(LOGBOOKS, LOGBOOKS + ".name", parameter));
                    break;
                case LEVEL:
                    builder.filter(buildWildcardDisMaxQuery(LEVEL, parameter));
                    break;
                case START:
                    ZonedDateTime startTime = determineDateAndTime(parameter, timeZone);
                    start = startTime != null ? startTime : ZonedDateTime.now();
                    temporalSearch = true;
                    break;
                case END:
                    ZonedDateTime endTime = determineDateAndTime(parameter, timeZone);
                    end = endTime != null ? endTime : Instant.ofEpochMilli(Long.MIN_VALUE).atZone(ZoneId.systemDefault());
                    temporalSearch = true;
                    break;
            }
        }

        if (temporalSearch) {
            if (start.isBefore(end) || start.equals(end)) {
                ZonedDateTime startTs = start;
                ZonedDateTime endTs = end;
                RangeQuery.Builder rangeQuery = new RangeQuery.Builder();
                rangeQuery.date(DateRangeQuery.of(b ->
                        b.field(CREATED_DATE)
                                .gte(JsonData.of(startTs.toEpochSecond()).toString())
                                .lte(JsonData.of(endTs.toEpochSecond()).toString())
                                .format(EPOCH_SECOND)));
                builder.filter(rangeQuery.build()._toQuery());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        MessageFormat.format(TextUtil.SEARCH_FAILED_PARSE_PARAMETERS_INVALID_START_END, searchParameters));
            }
        }

        BoolQuery _hybridQuery = builder.build();

        // Adds createdDate to the relavancy score of the query
        FunctionScoreQuery functionScoreQuery = FunctionScoreQuery.of(fs -> fs
                .query(_hybridQuery._toQuery())
                .functions(f -> f
                        .exp(DecayFunction.of(d -> d
                                .date(DateDecayFunction.of(dd -> dd
                                        .field(CREATED_DATE)
                                        .placement(DecayPlacement.of(p -> p
                                                .origin("now")
                                                .scale(Time.of(t -> t.time("30d")))
                                                .offset(Time.of(t -> t.time("1d")))
                                                .decay(0.5)))))
                        ))
                )
                .scoreMode(FunctionScoreMode.Multiply));

        int cappedSize = Math.min(size, maxSearchSize);
        int fromOffset = from;

        return SearchRequest.of(s -> s
                .index(ES_LOG_INDEX)
                .query(functionScoreQuery._toQuery())
                .timeout("60s")
                .size(cappedSize)
                .from(fromOffset));
    }

    /**
     * Computes a UTC {@link ZonedDateTime} based on client provided start/end search parameter, and time zone,
     * if specified.
     *
     * @param parameter The start or end search parameter
     * @param timeZone  Client provided tz, or system default.
     * @return A {@link ZonedDateTime} if search parameter can be parsed, otherwise <code>null</code>.
     * @throws ResponseStatusException if client provided {@link TemporalAmount} specifier is invalid.
     */
    protected ZonedDateTime determineDateAndTime(Entry<String, List<String>> parameter, TimeZone timeZone) {
        String value = parameter.getValue().get(0); // Even if client specifies start=, there is still one element in the parameter object
        if (!value.isEmpty()) {
            // If multiple time specifiers are provided by client, consider only first...
            String timeSpecifier = value.split("[\\|,;]")[0];
            if (!timeSpecifier.isEmpty()) {
                Object time = TimeParser.parseInstantOrTemporalAmount(timeSpecifier, timeZone.toZoneId());
                if (time instanceof Instant instant) {
                    return ZonedDateTime.ofInstant(instant, UTC_ZONE_ID);
                } else if (time instanceof TemporalAmount) {
                    try {
                        return ZonedDateTime.ofInstant(Instant.now().minus((TemporalAmount) time), timeZone.toZoneId());
                    } catch (UnsupportedTemporalTypeException e) { // E.g. if client sends "months" or "years"
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageFormat.format(TextUtil.UNSUPPORTED_DATE_TIME, timeSpecifier));
                    }
                }
            }
        }

        return null;
    }

    /**
     * Determines time zone based on client provided tz, if present.
     *
     * @param searchParameters Search parameters provided by client, may or may not include tz
     * @return Client provided {@link TimeZone}, or system default.
     * @throws IllegalArgumentException if client specified time zone identifier is invalid
     */
    protected TimeZone getTimezone(MultiValueMap<String, String> searchParameters) {
        for (Entry<String, List<String>> parameter : searchParameters.entrySet()) {
            if ("tz".equals(parameter.getKey().strip().toLowerCase())) {
                String timezoneString = parameter.getValue().get(0);
                if(timezoneString == null || timezoneString.isEmpty()){
                    return TimeZone.getDefault();
                }
                ZoneId zoneId;
                try {
                    zoneId = ZoneId.of(timezoneString);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Invalid time zone identifier \"" + timezoneString + "\"");
                    throw new IllegalArgumentException("Invalid time zone identifier \"" + timezoneString + "\"");
                }
                return TimeZone.getTimeZone(zoneId);
            }
        }
        return TimeZone.getDefault();
    }
}
