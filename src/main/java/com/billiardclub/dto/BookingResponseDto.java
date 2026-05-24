package com.billiardclub.dto;

import com.billiardclub.model.Booking;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingResponseDto {

    private Long id;
    private Long tableId;
    private int tableNumber;
    private String tableType;
    private Long clientId;
    private String clientName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;

    public static BookingResponseDto from(Booking b) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.id          = b.getId();
        dto.tableId     = b.getTable().getId();
        dto.tableNumber = b.getTable().getNumber();
        dto.tableType   = b.getTable().getType().name();
        dto.clientId    = b.getClient().getId();
        dto.clientName  = b.getClient().getFullName();
        dto.startTime   = b.getStartTime();
        dto.endTime     = b.getEndTime();
        dto.status      = b.getStatus().name();
        dto.totalPrice  = b.getTotalPrice();
        dto.createdAt   = b.getCreatedAt();
        return dto;
    }
}
