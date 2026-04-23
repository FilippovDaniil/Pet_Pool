package com.billiardclub.service;

import com.billiardclub.dto.BookingRequestDto;
import com.billiardclub.dto.PaymentDto;
import com.billiardclub.exception.BusinessException;
import com.billiardclub.model.*;
import com.billiardclub.repository.BookingRepository;
import com.billiardclub.repository.ClientRepository;
import com.billiardclub.repository.PaymentRepository;
import com.billiardclub.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TableRepository tableRepository;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;

    public List<Booking> findAll() {
        return bookingRepository.findAllByOrderByStartTimeDesc();
    }

    public List<Booking> findByStatus(BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }

    public List<Booking> findByDate(LocalDate date) {
        return bookingRepository.findAllByOrderByStartTimeDesc().stream()
                .filter(b -> b.getStartTime().toLocalDate().equals(date))
                .toList();
    }

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Бронирование не найдено"));
    }

    @Transactional
    public Booking create(BookingRequestDto dto) {
        BilliardTable table = tableRepository.findById(dto.getTableId())
                .orElseThrow(() -> new BusinessException("Стол не найден"));
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new BusinessException("Клиент не найден"));

        validateTimeRange(dto.getStartTime(), dto.getEndTime());

        List<BookingStatus> blockingStatuses = List.of(
                BookingStatus.PENDING, BookingStatus.PAID, BookingStatus.ACTIVE);
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                table, dto.getStartTime(), dto.getEndTime(), blockingStatuses);
        if (!conflicts.isEmpty()) {
            throw new BusinessException("Стол №" + table.getNumber() +
                    " занят в указанное время");
        }

        BigDecimal totalPrice = calculatePrice(table, dto.getStartTime(), dto.getEndTime());

        Booking booking = Booking.builder()
                .table(table)
                .client(client)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(BookingStatus.PENDING)
                .totalPrice(totalPrice)
                .createdAt(LocalDateTime.now())
                .build();

        log.info("Создана бронь стола №{} для клиента {} на {}", table.getNumber(), client.getFullName(), dto.getStartTime());
        return bookingRepository.save(booking);
    }

    @Transactional
    public void pay(Long bookingId, PaymentDto dto) {
        Booking booking = findById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Бронирование не в статусе ожидания оплаты");
        }

        booking.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking);

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalPrice())
                .paymentMethod(dto.getPaymentMethod())
                .timestamp(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        log.info("Принята оплата за бронь id={}, метод: {}", bookingId, dto.getPaymentMethod());
    }

    @Transactional
    public void cancel(Long bookingId) {
        Booking booking = findById(bookingId);
        if (booking.getStatus() == BookingStatus.ACTIVE || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Нельзя отменить активную или завершённую бронь");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Бронь id={} отменена", bookingId);
    }

    public BigDecimal calculatePrice(BilliardTable table, LocalDateTime start, LocalDateTime end) {
        long minutes = ChronoUnit.MINUTES.between(start, end);
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(table.getPricePerHour()).multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new BusinessException("Время окончания должно быть позже времени начала");
        }
        if (ChronoUnit.MINUTES.between(start, end) < 30) {
            throw new BusinessException("Минимальная продолжительность бронирования — 30 минут");
        }
    }
}
