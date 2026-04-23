package com.billiardclub.controller;

import com.billiardclub.model.Client;
import com.billiardclub.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private static final List<String> RANKS = List.of(
            "Любитель", "1 разряд", "Кандидат в мастера", "Мастер");

    private final ClientService clientService;

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String rank,
                       Model model) {
        model.addAttribute("clients", clientService.search(search, rank));
        model.addAttribute("ranks", RANKS);
        model.addAttribute("search", search);
        model.addAttribute("selectedRank", rank);
        return "clients/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("client", new Client());
        model.addAttribute("ranks", RANKS);
        return "clients/form";
    }

    @PostMapping
    public String create(@ModelAttribute Client client, RedirectAttributes ra) {
        try {
            clientService.create(client);
            ra.addFlashAttribute("successMessage", "Клиент " + client.getFullName() + " добавлен");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/clients";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("client", clientService.findById(id));
        model.addAttribute("ranks", RANKS);
        return "clients/form";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Client client,
                         Authentication auth, RedirectAttributes ra) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        try {
            clientService.update(id, client, isAdmin);
            ra.addFlashAttribute("successMessage", "Данные клиента обновлены");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/clients";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            clientService.delete(id);
            ra.addFlashAttribute("successMessage", "Клиент удалён");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/clients";
    }
}
