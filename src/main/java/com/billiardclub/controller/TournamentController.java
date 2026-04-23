package com.billiardclub.controller;

import com.billiardclub.model.TournamentRecord;
import com.billiardclub.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping("/record/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("record", tournamentService.findById(id));
        return "tournament/edit";
    }

    @PutMapping("/record/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String winnerName,
                         @RequestParam String loserName,
                         RedirectAttributes ra) {
        try {
            tournamentService.update(id, winnerName, loserName);
            ra.addFlashAttribute("successMessage", "Запись обновлена");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tournament";
    }

    @DeleteMapping("/record/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            tournamentService.delete(id);
            ra.addFlashAttribute("successMessage", "Запись удалена с доски");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tournament";
    }
}
