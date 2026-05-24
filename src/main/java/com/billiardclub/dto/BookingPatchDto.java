package com.billiardclub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingPatchDto {

    @NotBlank(message = "Укажите статус")
    private String status;   // CANCELLED | ACTIVE | COMPLETED

    private Long opponentId; // required when status=ACTIVE (start game)
    private Long winnerId;   // required when status=COMPLETED (finish game)
}
