package com.billiardclub.controller;

import com.billiardclub.dto.BookingPatchDto;
import com.billiardclub.dto.BookingRequestDto;
import com.billiardclub.dto.BookingResponseDto;
import com.billiardclub.dto.PaymentDto;
import com.billiardclub.model.Booking;
import com.billiardclub.model.BookingStatus;
import com.billiardclub.model.BilliardTable;
import com.billiardclub.service.BookingService;
import com.billiardclub.service.GameService;
import com.billiardclub.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST API for bookings.
 *
 * GET    /api/bookings               — list (filter by status or date)
 * POST   /api/bookings               — create               → 201 Created
 * PATCH  /api/bookings/{id}          — status transition    → 200 OK
 * POST   /api/bookings/{id}/payments — pay                  → 201 Created
 * GET    /api/bookings/price         — price calculation     → 200 OK
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingApiController {

    private final BookingService bookingService;
    private final GameService gameService;
    private final TableService tableService;

    @GetMapping
    public List<BookingResponseDto> list(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<Booking> bookings;
        if (status != null) {
            bookings = bookingService.findByStatus(status);
        } else if (date != null) {
            bookings = bookingService.findByDate(date);
        } else {
            bookings = bookingService.findAll();
        }
        return bookings.stream().map(BookingResponseDto::from).toList();
    }

    @PostMapping
    public ResponseEntity<BookingResponseDto> create(@Valid @RequestBody BookingRequestDto dto) {
        Booking created = bookingService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(BookingResponseDto.from(created));
    }

    /**
     * Status transitions:
     *   {"status":"CANCELLED"}                         — cancel booking
     *   {"status":"ACTIVE","opponentId":123}           — start game (booking must be PAID)
     *   {"status":"COMPLETED","winnerId":123}          — finish game, records tournament result
     */
    @PatchMapping("/{id}")
    public ResponseEntity<BookingResponseDto> patch(
            @PathVariable Long id,
            @Valid @RequestBody BookingPatchDto dto) {

        return switch (dto.getStatus().toUpperCase()) {
            case "CANCELLED" -> {
                bookingService.cancel(id);
                yield ResponseEntity.ok(BookingResponseDto.from(bookingService.findById(id)));
            }
            case "ACTIVE" -> {
                if (dto.getOpponentId() == null) {
                    yield ResponseEntity.badRequest()
                            .<BookingResponseDto>build();
                }
                gameService.startGame(id, dto.getOpponentId());
                yield ResponseEntity.ok(BookingResponseDto.from(bookingService.findById(id)));
            }
            case "COMPLETED" -> {
                if (dto.getWinnerId() == null) {
                    yield ResponseEntity.badRequest()
                            .<BookingResponseDto>build();
                }
                gameService.finishGame(id, dto.getWinnerId());
                yield ResponseEntity.ok(BookingResponseDto.from(bookingService.findById(id)));
            }
            default -> ResponseEntity.badRequest().<BookingResponseDto>build();
        };
    }

    /**
     * POST /api/bookings/{id}/payments — creates a payment for the booking.
     * Booking transitions: PENDING → PAID.
     */
    @PostMapping("/{id}/payments")
    public ResponseEntity<Map<String, Object>> pay(
            @PathVariable Long id,
            @Valid @RequestBody PaymentDto dto) {

        bookingService.pay(id, dto);
        Booking booking = bookingService.findById(id);
        return ResponseEntity.status(201).body(Map.of(
                "bookingId", id,
                "status", booking.getStatus().name(),
                "paymentMethod", dto.getPaymentMethod(),
                "amount", booking.getTotalPrice()));
    }

    /**
     * GET /api/bookings/price?tableId=1&startTime=2025-06-01T10:00&endTime=2025-06-01T12:00
     * Calculates price without creating a booking.
     */
    @GetMapping("/price")
    public ResponseEntity<Map<String, Object>> price(
            @RequestParam Long tableId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endTime) {

        BilliardTable table = tableService.findById(tableId);
        BigDecimal price = bookingService.calculatePrice(table, startTime, endTime);
        return ResponseEntity.ok(Map.of(
                "tableId", tableId,
                "pricePerHour", table.getPricePerHour(),
                "totalPrice", price));
    }
}
