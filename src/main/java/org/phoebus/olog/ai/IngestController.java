package org.phoebus.olog.ai;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/Olog/ai/ingest")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    // curl -X POST "http://localhost:8080/Olog/ai/ingest/logs"
    @PostMapping("/logs")
    public ResponseEntity<String> ingestOperationLogs() {
        try {
            ingestService.ingestAll();
            return ResponseEntity.ok("Successfully ingested.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ingest failed: " + e.getMessage());
        }
    }
}
