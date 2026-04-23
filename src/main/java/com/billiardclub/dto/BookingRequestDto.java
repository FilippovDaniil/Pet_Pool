package com.billiardclub.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class BookingRequestDto {

    @NotNull(message = "Выберите стол")
    private Long tableId;

    @NotNull(message = "Выберите клиента")
    private Long clientId;

    @NotNull(message = "Укажите время начала")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @NotNull(message = "Укажите время окончания")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;
}
