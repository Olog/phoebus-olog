package org.phoebus.olog.docker;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.*;
import org.phoebus.olog.entity.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogbookVisibilityIT {
    @Test void filtersBeforePaginationAndGuardsAttachments() throws Exception {
        try (GenericContainer<?> elastic = new GenericContainer<>("elasticsearch:8.2.3")
                .withEnv("discovery.type", "single-node")
                .withEnv("xpack.security.enabled", "false")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withExposedPorts(9200).waitingFor(Wait.forHttp("/").withStartupTimeout(Duration.ofMinutes(2)))) {
            elastic.start();
            try (var transport = new RestClientTransport(RestClient.builder(new HttpHost(elastic.getHost(), elastic.getMappedPort(9200))).build(), new JacksonJsonpMapper())) {
                var client = new ElasticsearchClient(transport);
                client.indices().create(c -> c.index("visibility-test").withJson(getClass().getResourceAsStream("/log_entry_mapping.json")));
                client.indices().create(c -> c.index("members-test").withJson(getClass().getResourceAsStream("/logbook_member_mapping.json")));
                for (int day = 1; day <= 4; day++) {
                    int id = day;
                    var document = Map.of("id", id, "createdDate", Instant.parse("2026-09-0" + day + "T12:00:00Z").toEpochMilli(),
                            "logbooks", List.of(Map.of("name", "operations")), "attachments", List.of(Map.of("id", "file-" + id)));
                    client.index(i -> i.index("visibility-test").id(Integer.toString(id)).document(document));
                }
                client.indices().refresh(r -> r.index("visibility-test"));
                var books = mock(LogbookRepository.class);
                var members = mock(LogbookMemberRepository.class);
                var book = new Logbook("operations", "owner");
                when(books.findById("operations")).thenReturn(Optional.of(book));
                when(books.findAll()).thenReturn(List.of(book));
                var member = new LogbookMember("operations", "reader", Set.of(LogbookPermission.READ), "owner", "2026-09-01T00:00:00Z", "2026-09-01T00:00:00Z");
                member.setVisibleFrom("2026-09-02");
                member.setVisibleTo("2026-09-03");
                client.index(i -> i.index("members-test").id(member.getId()).document(member));
                var persisted = client.get(g -> g.index("members-test").id(member.getId()), LogbookMember.class).source();
                assertEquals("2026-09-02", persisted.getVisibleFrom());
                assertEquals("2026-09-03", persisted.getVisibleTo());
                when(members.find("operations", "reader")).thenReturn(Optional.of(persisted));
                var access = new LogbookAccessService(books, members);
                var search = new LogSearchUtil();
                ReflectionTestUtils.setField(search, "ES_LOG_INDEX", "visibility-test");
                ReflectionTestUtils.setField(search, "maxSearchSize", 1000);
                ReflectionTestUtils.setField(search, "defaultSearchSize", 100);
                var params = new LinkedMultiValueMap<String, String>();
                params.set("size", "1");
                params.set("sort", "up");
                var first = client.search(search.buildSearchRequest(params, access.readFilter(() -> "reader")), Map.class);
                assertEquals(2, first.hits().total().value());
                assertEquals("2", first.hits().hits().get(0).id());
                params.set("from", "1");
                var second = client.search(search.buildSearchRequest(params, access.readFilter(() -> "reader")), Map.class);
                assertEquals("3", second.hits().hits().get(0).id());
                params.set("from", "2");
                assertTrue(client.search(search.buildSearchRequest(params, access.readFilter(() -> "reader")), Map.class).hits().hits().isEmpty());
                String oldIndex = ElasticConfig.ES_LOG_INDEX;
                try {
                    ElasticConfig.ES_LOG_INDEX = "visibility-test";
                    var repository = new LogRepository();
                    ReflectionTestUtils.setField(repository, "client", client);
                    ReflectionTestUtils.setField(repository, "logbookAccessService", access);
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("reader", "", List.of()));
                    assertDoesNotThrow(() -> repository.requireReadableAttachment("file-2"));
                    assertThrows(ResponseStatusException.class, () -> repository.requireReadableAttachment("file-1"));
                } finally {
                    ElasticConfig.ES_LOG_INDEX = oldIndex;
                    SecurityContextHolder.clearContext();
                }
            }
        }
    }
}
