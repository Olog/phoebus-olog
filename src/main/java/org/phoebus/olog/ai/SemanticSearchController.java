package org.phoebus.olog.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Olog/ai/search")
public class SemanticSearchController {

    private static final Logger logger = LoggerFactory.getLogger(SemanticSearchController.class);

    private final SemanticSearchService searchService;

    public SemanticSearchController(SemanticSearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/semantic")
    public SimpleSearchResponse semanticSearch(@RequestBody SearchQueryRequest request) {
        long start = System.currentTimeMillis();
        logger.info("[SEARCH] Query: {}", request.getQuery());
        logger.info("[SEARCH] UI Filters: logbooks={}, tags={}, dateRange={} to {}",
            request.getLogbooks(), request.getTags(),
            request.getCreatedDateFrom() != null ? request.getCreatedDateFrom() : "any",
            request.getCreatedDateTo() != null ? request.getCreatedDateTo() : "any");

        SimpleSearchResponse response = searchService.search(request);

        logger.info("[SEARCH] Completed in {}s, found {} results",
            (System.currentTimeMillis() - start) / 1000.0,
            response.getHits() != null ? response.getHits().size() : 0);

        return response;
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyzeResults(@RequestBody AnalysisRequest request) {
        long start = System.currentTimeMillis();
        logger.info("[ANALYSIS] Starting analysis for query: {}", request.getQuery());

        AnalysisResponse response = searchService.analyze(request);

        logger.info("[ANALYSIS] Completed in {}s",
            (System.currentTimeMillis() - start) / 1000.0);

        return response;
    }
}