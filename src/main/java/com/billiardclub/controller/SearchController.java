package com.billiardclub.controller;

import com.billiardclub.search.ClientDocument;
import com.billiardclub.search.ClientSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired(required = false)
    private ClientSearchService clientSearchService;

    /**
     * GET /api/search/clients?q=иванов&rank=Мастер&page=0&size=20
     *
     * Returns matching clients from OpenSearch index.
     * If OpenSearch is disabled (enabled=false), returns an empty list — graceful degradation.
     * Access: permitAll (same as GET /clients).
     */
    @GetMapping("/clients")
    public ResponseEntity<Map<String, Object>> searchClients(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String rank,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (clientSearchService == null) {
            return ResponseEntity.ok(Map.of(
                    "results", List.of(),
                    "opensearchEnabled", false));
        }

        List<ClientDocument> results = clientSearchService.search(q, rank, page, size);
        return ResponseEntity.ok(Map.of(
                "results", results,
                "page", page,
                "size", size,
                "count", results.size(),
                "opensearchEnabled", true));
    }
}
