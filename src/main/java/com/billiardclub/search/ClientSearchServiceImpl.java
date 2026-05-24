package com.billiardclub.search;

import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ClientSearchServiceImpl implements ClientSearchService {

    private static final String INDEX = "clients";
    private static final int PAGE_SIZE = 20;

    @Autowired(required = false)
    private OpenSearchClient client;

    @PostConstruct
    public void ensureIndex() {
        if (client == null) return;
        try {
            boolean exists = client.indices().exists(r -> r.index(INDEX)).value();
            if (!exists) {
                client.indices().create(r -> r.index(INDEX));
                log.info("OpenSearch index '{}' created", INDEX);
            }
        } catch (Exception e) {
            log.warn("OpenSearch unavailable at startup: {}", e.getMessage());
        }
    }

    @Override
    public void indexClient(ClientDocument doc) {
        if (client == null) return;
        try {
            client.index(r -> r.index(INDEX).id(doc.getId()).document(doc));
        } catch (Exception e) {
            log.warn("OpenSearch index failed for client {}: {}", doc.getId(), e.getMessage());
        }
    }

    @Override
    public void removeClient(String id) {
        if (client == null) return;
        try {
            client.delete(r -> r.index(INDEX).id(id));
        } catch (Exception e) {
            log.warn("OpenSearch delete failed for client {}: {}", id, e.getMessage());
        }
    }

    @Override
    public void reindexAll(List<ClientDocument> docs) {
        if (client == null) return;
        try {
            ensureIndex();
            for (ClientDocument doc : docs) {
                client.index(r -> r.index(INDEX).id(doc.getId()).document(doc));
            }
            log.info("OpenSearch: reindexed {} clients", docs.size());
        } catch (Exception e) {
            log.warn("OpenSearch reindexAll failed: {}", e.getMessage());
        }
    }

    @Override
    public List<ClientDocument> search(String query, String rank, int page, int size) {
        if (client == null) return Collections.emptyList();
        try {
            List<Query> clauses = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                // fullName^2: имя важнее телефона при ранжировании
                clauses.add(Query.of(q -> q.multiMatch(m -> m
                        .fields("fullName^2", "phone")
                        .query(query)
                        .fuzziness("AUTO"))));
            }
            if (rank != null && !rank.isBlank()) {
                // term query: точный match по rank (keyword)
                clauses.add(Query.of(q -> q.term(t -> t
                        .field("rank")
                        .value(FieldValue.of(rank)))));
            }

            Query finalQuery = clauses.isEmpty()
                    ? Query.of(q -> q.matchAll(m -> m))
                    : (clauses.size() == 1
                            ? clauses.get(0)
                            : Query.of(q -> q.bool(BoolQuery.of(b -> b.must(clauses)))));

            SearchRequest request = new SearchRequest.Builder()
                    .index(INDEX)
                    .from(page * size)
                    .size(size)
                    .query(finalQuery)
                    .build();

            SearchResponse<ClientDocument> response = client.search(request, ClientDocument.class);

            return response.hits().hits().stream()
                    .map(h -> h.source())
                    .filter(s -> s != null)
                    .toList();
        } catch (Exception e) {
            log.warn("OpenSearch search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
