package com.billiardclub.config;

import com.billiardclub.repository.ClientRepository;
import com.billiardclub.search.ClientDocument;
import com.billiardclub.search.ClientSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class SearchInitializer implements CommandLineRunner {

    private final ClientRepository clientRepository;

    @Autowired(required = false)
    private ClientSearchService clientSearchService;

    @Override
    @Transactional(readOnly = true)
    public void run(String... args) {
        if (clientSearchService == null) {
            log.info("OpenSearch disabled — skipping reindex");
            return;
        }
        List<ClientDocument> docs = clientRepository.findAll().stream()
                .map(c -> ClientDocument.builder()
                        .id(String.valueOf(c.getId()))
                        .fullName(c.getFullName())
                        .rank(c.getRank())
                        .phone(c.getPhone())
                        .build())
                .toList();
        clientSearchService.reindexAll(docs);
        log.info("OpenSearch: scheduled reindex of {} clients", docs.size());
    }
}
