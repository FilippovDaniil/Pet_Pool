package com.billiardclub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GameFinishDto {

    @NotNull(message = "Выберите победителя")
    private Long winnerId;
}
