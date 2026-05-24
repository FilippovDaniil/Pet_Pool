package com.billiardclub.controller;

import com.billiardclub.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Thymeleaf views for tournament records.
 * Mutations (PUT, DELETE) are handled by TournamentApiController under /api/tournament.
 */
@Controller
@RequestMapping("/tournament")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("records", tournamentService.findAll());
        return "tournament/list";
    }

    // Path fixed: /tournament/{id}/edit instead of /tournament/record/{id}/edit
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("record", tournamentService.findById(id));
        return "tournament/edit";
    }
}
