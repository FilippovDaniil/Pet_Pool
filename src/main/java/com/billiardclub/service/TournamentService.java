package com.billiardclub.service;

import com.billiardclub.exception.BusinessException;
import com.billiardclub.model.TournamentRecord;
import com.billiardclub.repository.TournamentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRecordRepository tournamentRecordRepository;

    public List<TournamentRecord> findAll() {
        return tournamentRecordRepository.findAllByOrderByGameDateDesc();
    }

    public TournamentRecord findById(Long id) {
        return tournamentRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Запись турнирной доски не найдена"));
    }

    @Transactional
    public TournamentRecord update(Long id, String winnerName, String loserName) {
        TournamentRecord record = findById(id);
        record.setWinnerName(winnerName);
        record.setLoserName(loserName);
        log.info("Обновлена запись турнирной доски id={}", id);
        return tournamentRecordRepository.save(record);
    }

    @Transactional
    public void delete(Long id) {
        tournamentRecordRepository.deleteById(id);
        log.info("Удалена запись турнирной доски id={}", id);
    }
}
