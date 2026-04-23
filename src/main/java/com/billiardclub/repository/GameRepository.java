package com.billiardclub.repository;

import com.billiardclub.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByBooking_Id(Long bookingId);
    List<Game> findByWinnerIsNull();
}
