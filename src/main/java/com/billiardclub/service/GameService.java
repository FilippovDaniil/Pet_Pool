package com.billiardclub.service;

import com.billiardclub.exception.BusinessException;
import com.billiardclub.model.*;
import com.billiardclub.repository.BookingRepository;
import com.billiardclub.repository.ClientRepository;
import com.billiardclub.repository.GameRepository;
import com.billiardclub.repository.TournamentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final TournamentRecordRepository tournamentRecordRepository;

    public List<Game> findActiveGames() {
        return gameRepository.findByWinnerIsNull();
    }

    @Transactional
    public Game startGame(Long bookingId, Long client2Id) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("Бронирование не найдено"));

        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BusinessException("Бронирование должно быть оплачено перед началом игры");
        }

        if (gameRepository.findByBooking_Id(bookingId).isPresent()) {
            throw new BusinessException("Игра для этой брони уже создана");
        }

        Client client2 = clientRepository.findById(client2Id)
                .orElseThrow(() -> new BusinessException("Второй клиент не найден"));

        Game game = Game.builder()
                .booking(booking)
                .client1(booking.getClient())
                .client2(client2)
                .startTime(LocalDateTime.now())
                .build();

        booking.setStatus(BookingStatus.ACTIVE);
        bookingRepository.save(booking);

        Game saved = gameRepository.save(game);
        log.info("Начата игра: {} vs {} на столе №{}", booking.getClient().getFullName(),
                client2.getFullName(), booking.getTable().getNumber());
        return saved;
    }

    @Transactional
    public TournamentRecord finishGame(Long bookingId, Long winnerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("Бронирование не найдено"));

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BusinessException("Игра не активна");
        }

        Game game = gameRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new BusinessException("Игра не найдена"));

        Client winner = clientRepository.findById(winnerId)
                .orElseThrow(() -> new BusinessException("Победитель не найден"));

        if (!winner.getId().equals(game.getClient1().getId()) &&
                !winner.getId().equals(game.getClient2().getId())) {
            throw new BusinessException("Победитель должен быть участником игры");
        }

        game.setWinner(winner);
        game.setEndTime(LocalDateTime.now());
        gameRepository.save(game);

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        Client loser = winner.getId().equals(game.getClient1().getId())
                ? game.getClient2() : game.getClient1();

        TournamentRecord record = TournamentRecord.builder()
                .game(game)
                .winnerName(winner.getFullName())
                .loserName(loser.getFullName())
                .tableNumber(booking.getTable().getNumber())
                .gameDate(game.getEndTime())
                .createdAt(LocalDateTime.now())
                .build();

        TournamentRecord saved = tournamentRecordRepository.save(record);
        log.info("Завершена игра: победитель {} над {} на столе №{}",
                winner.getFullName(), loser.getFullName(), booking.getTable().getNumber());
        return saved;
    }

    public Game findByBookingId(Long bookingId) {
        return gameRepository.findByBooking_Id(bookingId).orElse(null);
    }
}
