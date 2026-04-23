package com.billiardclub.service;

import com.billiardclub.exception.BusinessException;
import com.billiardclub.model.BilliardTable;
import com.billiardclub.model.BookingStatus;
import com.billiardclub.repository.BookingRepository;
import com.billiardclub.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableService {

    private final TableRepository tableRepository;
    private final BookingRepository bookingRepository;

    public List<BilliardTable> findAll() {
        return tableRepository.findAll();
    }

    public BilliardTable findById(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Стол не найден"));
    }

    @Transactional
    public BilliardTable create(BilliardTable table) {
        if (tableRepository.existsByNumber(table.getNumber())) {
            throw new BusinessException("Стол с номером " + table.getNumber() + " уже существует");
        }
        log.info("Создан новый стол №{} ({})", table.getNumber(), table.getType());
        return tableRepository.save(table);
    }

    @Transactional
    public BilliardTable update(Long id, BilliardTable updated) {
        BilliardTable existing = findById(id);
        if (existing.getNumber() != updated.getNumber() && tableRepository.existsByNumber(updated.getNumber())) {
            throw new BusinessException("Стол с номером " + updated.getNumber() + " уже существует");
        }
        existing.setNumber(updated.getNumber());
        existing.setType(updated.getType());
        return tableRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        BilliardTable table = findById(id);
        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING, BookingStatus.PAID, BookingStatus.ACTIVE, BookingStatus.COMPLETED);
        List<?> bookings = bookingRepository.findConflictingBookings(
                table, LocalDateTime.now().minusYears(100), LocalDateTime.now().plusYears(100), blockingStatuses);
        if (!bookings.isEmpty()) {
            throw new BusinessException("Нельзя удалить стол: у него есть связанные бронирования");
        }
        tableRepository.deleteById(id);
        log.info("Стол id={} удалён", id);
    }

    public boolean isTableAvailable(BilliardTable table, LocalDateTime startTime, LocalDateTime endTime) {
        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING, BookingStatus.PAID, BookingStatus.ACTIVE);
        return bookingRepository.findConflictingBookings(table, startTime, endTime, blockingStatuses).isEmpty();
    }

    public boolean isTableAvailableExcluding(BilliardTable table, LocalDateTime startTime,
                                              LocalDateTime endTime, Long excludeBookingId) {
        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING, BookingStatus.PAID, BookingStatus.ACTIVE);
        return bookingRepository.findConflictingBookingsExcluding(
                table, startTime, endTime, blockingStatuses, excludeBookingId).isEmpty();
    }

    public boolean isCurrentlyAvailable(BilliardTable table) {
        LocalDateTime now = LocalDateTime.now();
        return isTableAvailable(table, now, now.plusMinutes(1));
    }
}
