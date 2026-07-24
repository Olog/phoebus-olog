package org.phoebus.olog.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

@SpringBootTest
class SemanticApplicationTests {
	@MockitoBean
	private ElasticsearchClient elasticsearchClient;

	@MockitoBean
	private ElasticsearchVectorStore elasticsearchVectorStore;

	@MockitoBean
	private SemanticSearchService semanticSearchService;

	@MockitoBean
	private QueryPlannerService queryPlannerService;

	@MockitoBean
	private EmbeddingModel embeddingModel;

	@Test
	void contextLoads() {
	}

}