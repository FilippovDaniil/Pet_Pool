package com.billiardclub.dto;

import com.billiardclub.model.TournamentRecord;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TournamentResponseDto {

    private Long id;
    private String winnerName;
    private String loserName;
    private int tableNumber;
    private LocalDateTime gameDate;
    private LocalDateTime createdAt;

    public static TournamentResponseDto from(TournamentRecord r) {
        TournamentResponseDto dto = new TournamentResponseDto();
        dto.id          = r.getId();
        dto.winnerName  = r.getWinnerName();
        dto.loserName   = r.getLoserName();
        dto.tableNumber = r.getTableNumber();
        dto.gameDate    = r.getGameDate();
        dto.createdAt   = r.getCreatedAt();
        return dto;
    }
}
