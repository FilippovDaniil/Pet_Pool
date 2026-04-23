package com.billiardclub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GameStartDto {

    @NotNull(message = "Выберите второго игрока")
    private Long client2Id;
}
