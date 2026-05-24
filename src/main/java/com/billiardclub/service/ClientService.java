package com.billiardclub.service;

import com.billiardclub.exception.BusinessException;
import com.billiardclub.model.Client;
import com.billiardclub.repository.BookingRepository;
import com.billiardclub.repository.ClientRepository;
import com.billiardclub.search.ClientDocument;
import com.billiardclub.search.ClientSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final BookingRepository bookingRepository;

    @Autowired(required = false)
    private ClientSearchService clientSearchService;

    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    public List<Client> search(String name, String rank) {
        boolean hasName = StringUtils.hasText(name);
        boolean hasRank = StringUtils.hasText(rank);

        if (hasName && hasRank) {
            return clientRepository.findByFullNameContainingIgnoreCaseAndRank(name, rank);
        } else if (hasName) {
            return clientRepository.findByFullNameContainingIgnoreCase(name);
        } else if (hasRank) {
            return clientRepository.findByRank(rank);
        }
        return clientRepository.findAll();
    }

    public Client findById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Клиент не найден"));
    }

    @Transactional
    public Client create(Client client) {
        Client saved = clientRepository.save(client);
        indexClient(saved);
        log.info("Создан новый клиент: {}", saved.getFullName());
        return saved;
    }

    @Transactional
    public Client update(Long id, Client updated, boolean canChangeRank) {
        Client existing = findById(id);
        existing.setFullName(updated.getFullName());
        existing.setPhone(updated.getPhone());
        if (canChangeRank) {
            existing.setRank(updated.getRank());
        }
        Client saved = clientRepository.save(existing);
        indexClient(saved);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        if (!bookingRepository.findByClient_Id(id).isEmpty()) {
            throw new BusinessException("Нельзя удалить клиента: у него есть бронирования");
        }
        clientRepository.deleteById(id);
        if (clientSearchService != null) {
            clientSearchService.removeClient(String.valueOf(id));
        }
        log.info("Клиент id={} удалён", id);
    }

    private void indexClient(Client client) {
        if (clientSearchService == null) return;
        clientSearchService.indexClient(ClientDocument.builder()
                .id(String.valueOf(client.getId()))
                .fullName(client.getFullName())
                .rank(client.getRank())
                .phone(client.getPhone())
                .build());
    }
}
