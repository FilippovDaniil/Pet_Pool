package com.billiardclub.repository;

import com.billiardclub.model.BilliardTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TableRepository extends JpaRepository<BilliardTable, Long> {
    Optional<BilliardTable> findByNumber(int number);
    boolean existsByNumber(int number);
}
