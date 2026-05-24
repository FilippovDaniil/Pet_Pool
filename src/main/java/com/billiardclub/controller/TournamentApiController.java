package com.billiardclub.controller;

import com.billiardclub.dto.TournamentResponseDto;
import com.billiardclub.dto.TournamentUpdateDto;
import com.billiardclub.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for tournament records.
 *
 * GET    /api/tournament       — list all records (ordered by date desc)
 * GET    /api/tournament/{id}  — get single record
 * PUT    /api/tournament/{id}  — update winner/loser names      → 200 OK
 * DELETE /api/tournament/{id}  — delete record                  → 204 No Content
 *
 * Note: records are created automatically when a game finishes (via GameService).
 * Previously the path was /tournament/record/{id} — the /record segment was redundant.
 */
@RestController
@RequestMapping("/api/tournament")
@RequiredArgsConstructor
public class TournamentApiController {

    private final TournamentService tournamentService;

    @GetMapping
    public List<TournamentResponseDto> list() {
        return tournamentService.findAll().stream()
                .map(TournamentResponseDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TournamentResponseDto getById(@PathVariable Long id) {
        return TournamentResponseDto.from(tournamentService.findById(id));
    }

    @PutMapping("/{id}")
    public TournamentResponseDto update(@PathVariable Long id,
                                        @Valid @RequestBody TournamentUpdateDto dto) {
        return TournamentResponseDto.from(
                tournamentService.update(id, dto.getWinnerName(), dto.getLoserName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tournamentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
