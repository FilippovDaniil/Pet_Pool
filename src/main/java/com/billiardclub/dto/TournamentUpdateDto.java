package com.billiardclub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TournamentUpdateDto {

    @NotBlank(message = "Укажите имя победителя")
    private String winnerName;

    @NotBlank(message = "Укажите имя проигравшего")
    private String loserName;
}
