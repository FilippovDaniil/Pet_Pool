package com.billiardclub.repository;

import com.billiardclub.model.TournamentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentRecordRepository extends JpaRepository<TournamentRecord, Long> {
    List<TournamentRecord> findAllByOrderByGameDateDesc();
}
