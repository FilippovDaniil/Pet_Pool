package com.billiardclub.search;

import java.util.List;

public interface ClientSearchService {

    void indexClient(ClientDocument doc);

    void removeClient(String id);

    void reindexAll(List<ClientDocument> docs);

    List<ClientDocument> search(String query, String rank, int page, int size);
}
