package org.phoebus.olog;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.phoebus.olog.entity.Logbook;
import org.phoebus.olog.entity.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A element which creates the elastic rest clients used the olog service for
 * creating and retrieving logs and other resources
 *
 * @author kunal
 */
@Configuration
@ComponentScan(basePackages = {"org.phoebus.olog"})
@PropertySource("classpath:application.properties")
public class ElasticConfig {

    private static final Logger logger = Logger.getLogger(ElasticConfig.class.getName());

    // Read the elastic index and type from the application.properties
    @Value("${elasticsearch.http.connect_timeout_ms:" + RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS + "}")
    // default 1 second
    @SuppressWarnings("unused")
    private Integer ES_HTTP_CONNECT_TIMEOUT_MS;
    @Value("${elasticsearch.http.socket_timeout_ms:" + RestClientBuilder.DEFAULT_SOCKET_TIMEOUT_MILLIS + "}")
    // default 30 seconds
    @SuppressWarnings("unused")
    private Integer ES_HTTP_SOCKET_TIMEOUT_MS;
    @Value("${elasticsearch.http.keep_alive_timeout_ms:30000}") // default 30 seconds
    @SuppressWarnings("unused")
    private Long ES_HTTP_CLIENT_KEEP_ALIVE_TIMEOUT_MS;
    @Value("${elasticsearch.index.create.timeout:30s}")
    @SuppressWarnings("unused")
    private String ES_INDEX_CREATE_TIMEOUT;
    @Value("${elasticsearch.index.create.master_timeout:30s}")
    @SuppressWarnings("unused")
    private String ES_INDEX_CREATE_MASTER_TIMEOUT;

    public static String ES_TAG_INDEX;

    @Value("${elasticsearch.tag.index:olog_tags}")
    @SuppressWarnings("unused")
    public void setEsTagIndex(String indexName) {
        ES_TAG_INDEX = indexName;
    }

    public static String ES_LOGBOOK_INDEX;

    @Value("${elasticsearch.logbook.index:olog_logbooks}")
    @SuppressWarnings("unused")
    public void setEsLogbookIndex(String indexName) {
        ES_LOGBOOK_INDEX = indexName;
    }

    public static String ES_PROPERTY_INDEX;

    @Value("${elasticsearch.property.index:olog_properties}")
    @SuppressWarnings("unused")
    public void setEsPropertyIndex(String indexName) {
        ES_PROPERTY_INDEX = indexName;
    }

    public static String ES_LOG_INDEX;

    @Value("${elasticsearch.log.index:olog_logs}")
    @SuppressWarnings("unused")
    public void setEsLogIndex(String indexName) {
        ES_LOG_INDEX = indexName;
    }

    @Value("${elasticsearch.sequence.index:olog_sequence}")
    @SuppressWarnings("unused")
    private String ES_SEQ_INDEX;

    public static String ES_LOG_ARCHIVE_INDEX;

    @Value("${elasticsearch.log.archive.index:olog_archived_logs}")
    @SuppressWarnings("unused")
    public void setEsLogArchiveIndex(String indexName) {
        ES_LOG_ARCHIVE_INDEX = indexName;
    }

    public static String ES_LOG_TEMPLATE_INDEX;

    @Value("${elasticsearch.template.index:olog_templates}")
    @SuppressWarnings("unused")
    public void setEsLogTemplateIndex(String indexName) {
        ES_LOG_TEMPLATE_INDEX = indexName;
    }

    public static String ES_LEVEL_INDEX;

    @Value("${elasticsearch.level.index:olog_levels}")
    @SuppressWarnings("unused")
    public void setEsLevelIndex(String indexName) {
        ES_LEVEL_INDEX = indexName;
    }

    @Value("${elasticsearch.cluster.name:elasticsearch}")
    @SuppressWarnings("unused")
    private String clusterName;
    @Value("${elasticsearch.network.host:localhost}")
    @SuppressWarnings("unused")
    private String host;
    @Value("${elasticsearch.http.port:9200}")
    @SuppressWarnings("unused")
    private int port;
    @Value("${elasticsearch.http.protocol:http}")
    @SuppressWarnings("unused")
    private String protocol;
    @Value("${elasticsearch.create.indices:true}")
    @SuppressWarnings("unused")
    private String createIndices;

    @Value("${default.logbook.url}")
    @SuppressWarnings("unused")
    private String defaultLogbooksURL;
    @Value("${default.tags.url}")
    @SuppressWarnings("unused")
    private String defaultTagsURL;
    @Value("${default.properties.url}")
    @SuppressWarnings("unused")
    private String defaultPropertiesURL;
    @Value("${default.levels.url}")
    @SuppressWarnings("unused")
    private String defaultLevelsURL;

    private ElasticsearchClient client;
    private static final AtomicBoolean esInitialized = new AtomicBoolean();

    private CreateIndexRequest.Builder withTimeouts(CreateIndexRequest.Builder builder) {
        return builder
                .timeout(timeBuilder ->
                        timeBuilder.time(ES_INDEX_CREATE_TIMEOUT)
                ).masterTimeout(timeBuilder ->
                        timeBuilder.time(ES_INDEX_CREATE_MASTER_TIMEOUT)
                );
    }

    private void logCreateIndexRequest(CreateIndexRequest request) {
        logger.log(Level.INFO, () -> String.format(
                "CreateIndexRequest: " +
                        "index: %s, " +
                        "timeout: %s, " +
                        "masterTimeout: %s, " +
                        "waitForActiveShards: %s",
                request.index(),
                request.timeout() != null ? request.timeout().time() : null,
                request.masterTimeout() != null ? request.masterTimeout().time() : null,
                request.waitForActiveShards() != null ? request.waitForActiveShards()._toJsonString() : null
        ));
    }

    @Bean({"client"})
    public ElasticsearchClient getClient() {
        if (client == null) {
            // Create the low-level client
            logger.log(Level.INFO, () -> String.format("Creating HTTP client with " +
                            "host %s, " +
                            "port %s, " +
                            "protocol %s, " +
                            "keep-alive %s ms, " +
                            "connect timeout %s ms, " +
                            "socket timeout %s ms",
                    host, port, protocol,
                    ES_HTTP_CLIENT_KEEP_ALIVE_TIMEOUT_MS,
                    ES_HTTP_CONNECT_TIMEOUT_MS,
                    ES_HTTP_SOCKET_TIMEOUT_MS
            ));
            RestClient httpClient = RestClient.builder(new HttpHost(host, port, protocol))
                    .setRequestConfigCallback(builder ->
                            builder.setConnectTimeout(ES_HTTP_CONNECT_TIMEOUT_MS)
                                    .setSocketTimeout(ES_HTTP_SOCKET_TIMEOUT_MS)
                    )
                    .setHttpClientConfigCallback(builder ->
                            // Avoid timeout problems
                            // https://github.com/elastic/elasticsearch/issues/65213
                            builder.setKeepAliveStrategy((response, context) -> ES_HTTP_CLIENT_KEEP_ALIVE_TIMEOUT_MS)
                    )
                    .build();

            // Create the Java API Client with the same low level client
            ElasticsearchTransport transport = new RestClientTransport(
                    httpClient,
                    new JacksonJsonpMapper()
            );
            client = new ElasticsearchClient(transport);
            esInitialized.set(!Boolean.parseBoolean(createIndices));
            if (esInitialized.compareAndSet(false, true)) {
                elasticIndexValidation(client);
                elasticIndexInitialization(client);
            }
        }
        return client;
    }

    /**
     * Create the olog indices and templates if they don't exist
     *
     * @param client The {@link ElasticsearchClient} instance
     */
    void elasticIndexValidation(ElasticsearchClient client) {

        // Olog Sequence Index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/seq_mapping.json")) {
            BooleanResponse exists = client.indices().exists(ExistsRequest.of(e -> e.index(ES_SEQ_INDEX)));
            if (!exists.value()) {
                CreateIndexRequest request = CreateIndexRequest.of(
                        c -> withTimeouts(c).index(ES_SEQ_INDEX)
                                .withJson(is)
                );
                logCreateIndexRequest(request);
                CreateIndexResponse result = client.indices().create(
                        request
                );
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.ELASTIC_CREATED_INDEX_ACKNOWLEDGED, ES_SEQ_INDEX, result.acknowledged()));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format(TextUtil.ELASTIC_FAILED_TO_CREATE_INDEX, ES_SEQ_INDEX), e);
        }

        // Olog Logbook Index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/logbook_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_LOGBOOK_INDEX)));
            if (!exits.value()) {
                CreateIndexRequest request = CreateIndexRequest.of(
                        c -> withTimeouts(c).index(ES_LOGBOOK_INDEX).withJson(is)
                );
                logCreateIndexRequest(request);
                CreateIndexResponse result = client.indices().create(request);
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.ELASTIC_CREATED_INDEX_ACKNOWLEDGED, ES_LOGBOOK_INDEX, result.acknowledged()));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format(TextUtil.ELASTIC_FAILED_TO_CREATE_INDEX, ES_LOGBOOK_INDEX), e);
        }

        // Olog Tag Index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/tag_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_TAG_INDEX)));
            if (!exits.value()) {
                CreateIndexRequest request = CreateIndexRequest.of(
                        c -> withTimeouts(c).index(ES_TAG_INDEX).withJson(is)
                );
                logCreateIndexRequest(request);
                CreateIndexResponse result = client.indices().create(request);
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.ELASTIC_CREATED_INDEX_ACKNOWLEDGED, ES_TAG_INDEX, result.acknowledged()));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format(TextUtil.ELASTIC_FAILED_TO_CREATE_INDEX, ES_TAG_INDEX), e);
        }

        // Olog Property Index
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/property_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_PROPERTY_INDEX)));
            if (!exits.value()) {
                CreateIndexRequest request = CreateIndexRequest.of(
                        c -> withTimeouts(c).index(ES_PROPERTY_INDEX).withJson(is)
                );
                logCreateIndexRequest(request);
                CreateIndexResponse result = client.indices().create(request);
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.ELASTIC_CREATED_INDEX_ACKNOWLEDGED, ES_PROPERTY_INDEX, result.acknowledged()));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format(TextUtil.ELASTIC_FAILED_TO_CREATE_INDEX, ES_PROPERTY_INDEX), e);
        }

        // Olog Log Entry
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/log_entry_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_LOG_INDEX)));
            if (!exits.value()) {
                CreateIndexRequest request = CreateIndexRequest.of(
                        c -> withTimeouts(c).index(ES_LOG_INDEX).withJson(is)
                );
                logCreateIndexRequest(request);
                CreateIndexResponse result = client.indices().create(request);
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.ELASTIC_CREATED_INDEX_ACKNOWLEDGED, ES_LOG_INDEX, result.acknowledged()));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format(TextUtil.ELASTIC_FAILED_TO_CREATE_INDEX, ES_LOG_INDEX), e);
        }
        // Olog Archived Log Entry
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/log_entry_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_LOG_ARCHIVE_INDEX)));
            if (!exits.value()) {
                CreateIndexRequest request = CreateIndexRequest.of(
                        c -> withTimeouts(c).index(ES_LOG_ARCHIVE_INDEX).withJson(is)
                );
                logCreateIndexRequest(request);
                CreateIndexResponse result = client.indices().create(request);
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.ELASTIC_CREATED_INDEX_ACKNOWLEDGED, ES_LOG_ARCHIVE_INDEX, result.acknowledged()));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format(TextUtil.ELASTIC_FAILED_TO_CREATE_INDEX, ES_LOG_ARCHIVE_INDEX), e);
        }

        // Olog Template
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/log_template_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_LOG_TEMPLATE_INDEX)));
            if (!exits.value()) {
                CreateIndexRequest request = CreateIndexRequest.of(
                        c -> withTimeouts(c).index(ES_LOG_TEMPLATE_INDEX).withJson(is)
                );
                logCreateIndexRequest(request);
                CreateIndexResponse result = client.indices().create(request);
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.ELASTIC_CREATED_INDEX_ACKNOWLEDGED, ES_LOG_TEMPLATE_INDEX, result.acknowledged()));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format(TextUtil.ELASTIC_FAILED_TO_CREATE_INDEX, ES_LOG_TEMPLATE_INDEX), e);
        }

        // Olog Levels (aka Entry Types)
        try (InputStream is = ElasticConfig.class.getResourceAsStream("/level_mapping.json")) {
            BooleanResponse exits = client.indices().exists(ExistsRequest.of(e -> e.index(ES_LEVEL_INDEX)));
            if (!exits.value()) {
                CreateIndexRequest request = CreateIndexRequest.of(
                        c -> withTimeouts(c).index(ES_LEVEL_INDEX).withJson(is)
                );
                logCreateIndexRequest(request);
                CreateIndexResponse result = client.indices().create(request);
                logger.log(Level.INFO, () -> MessageFormat.format(TextUtil.ELASTIC_CREATED_INDEX_ACKNOWLEDGED, ES_LEVEL_INDEX, result.acknowledged()));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, MessageFormat.format(TextUtil.ELASTIC_FAILED_TO_CREATE_INDEX, ES_LEVEL_INDEX), e);
        }
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Create the default logbooks, tags, and properties
     *
     * @param indexClient the elastic client instance used to create the default resources
     */
    private void elasticIndexInitialization(ElasticsearchClient indexClient) {
        // Setup the default logbooks
        String logbooksURL = defaultLogbooksURL;
        if (logbooksURL.isEmpty()) {
            final URL resource = getClass().getResource("/default_logbooks.json");
            logbooksURL = resource.toExternalForm();
        }
        try (InputStream input = new URL(logbooksURL).openStream()) {
            List<Logbook> jsonLogbooks = mapper.readValue(input, new TypeReference<>() {
            });

            jsonLogbooks.forEach(logbook -> {
                try {
                    if (!indexClient.exists(e -> e.index(ES_LOGBOOK_INDEX).id(logbook.getName())).value()) {
                        IndexRequest<Logbook> indexRequest =
                                IndexRequest.of(i ->
                                        i.index(ES_LOGBOOK_INDEX)
                                                .id(logbook.getName())
                                                .document(logbook)
                                                .refresh(Refresh.True));
                        client.index(indexRequest);
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, MessageFormat.format(TextUtil.ELASTIC_FAILED_TO_INITIALIZE_LOGBOOK, logbook.getName()), e);
                }
            });
        } catch (IOException ex) {
            logger.log(Level.WARNING, TextUtil.ELASTIC_FAILED_TO_INITIALIZE_LOGBOOKS, ex);
        }

        // Setup the default tags
        String tagsURL = defaultTagsURL;
        if (defaultTagsURL.isEmpty()) {
            final URL resource = getClass().getResource("/default_tags.json");
            tagsURL = resource.toExternalForm();
        }
        try (InputStream input = new URL(tagsURL).openStream()) {
            List<Tag> jsonTag = mapper.readValue(input, new TypeReference<>() {
            });

            jsonTag.forEach(tag -> {
                try {
                    if (!indexClient.exists(e -> e.index(ES_TAG_INDEX).id(tag.getName())).value()) {
                        IndexRequest<Tag> indexRequest =
                                IndexRequest.of(i ->
                                        i.index(ES_TAG_INDEX)
                                                .id(tag.getName())
                                                .document(tag)
                                                .refresh(Refresh.True));
                        client.index(indexRequest);
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, MessageFormat.format(TextUtil.ELASTIC_FAILED_TO_INITIALIZE_TAG, tag.getName()), e);
                }
            });
        } catch (IOException ex) {
            logger.log(Level.WARNING, TextUtil.ELASTIC_FAILED_TO_INITIALIZE_TAGS, ex);
        }

        // Setup the default levels
        String levelsURL = defaultLevelsURL;
        if (levelsURL.isEmpty()) {
            final URL resource = getClass().getResource("/default_levels.json");
            levelsURL = resource.toExternalForm();
        }
        try (InputStream input = new URL(levelsURL).openStream()) {
            List<org.phoebus.olog.entity.Level> jsonTag = mapper.readValue(input, new TypeReference<>() {
            });

            // Get all (if any) to be able to determine if a level marked as default already exists.
            SearchRequest searchRequest = SearchRequest.of(s ->
                    s.index(ES_LEVEL_INDEX)
                            .query(q -> q.match(t -> t.field("defaultLevel").query(true)))
                            .timeout("10s")
                            .size(1000));

            SearchResponse<org.phoebus.olog.entity.Level> response =
                    client.search(searchRequest, org.phoebus.olog.entity.Level.class);

            final AtomicBoolean defaultLevelExists =
                    new AtomicBoolean(!response.hits().hits().isEmpty());

            jsonTag.forEach(level -> {
                if (defaultLevelExists.get() && level.defaultLevel()) {
                    logger.log(Level.WARNING, "Not inserting level \"" + level.name() + "\" as a default level already exists");
                } else {
                    try {
                        if (!indexClient.exists(e -> e.index(ES_LEVEL_INDEX).id(level.name())).value()) {
                            IndexRequest<org.phoebus.olog.entity.Level> indexRequest =
                                    IndexRequest.of(i ->
                                            i.index(ES_LEVEL_INDEX)
                                                    .id(level.name())
                                                    .document(level)
                                                    .refresh(Refresh.True));
                            client.index(indexRequest);
                        }
                        if (level.defaultLevel()) {
                            defaultLevelExists.set(true);
                        }
                    } catch (IOException e) {
                        logger.log(Level.WARNING, MessageFormat.format(TextUtil.ELASTIC_FAILED_TO_INITIALIZE_LEVEL,
                                level.name()), e);
                    }
                }
            });
        } catch (IOException ex) {
            logger.log(Level.WARNING, TextUtil.ELASTIC_FAILED_TO_INITIALIZE_LEVELS, ex);
        }
    }
}