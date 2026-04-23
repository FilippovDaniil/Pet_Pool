package com.billiardclub.controller;

import com.billiardclub.model.BilliardTable;
import com.billiardclub.model.TableType;
import com.billiardclub.service.TableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tables")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    @GetMapping
    public String list(Model model) {
        List<BilliardTable> tables = tableService.findAll();
        Map<Long, Boolean> availability = tables.stream()
                .collect(Collectors.toMap(
                        BilliardTable::getId,
                        tableService::isCurrentlyAvailable));
        model.addAttribute("tables", tables);
        model.addAttribute("availability", availability);
        return "tables/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("table", new BilliardTable());
        model.addAttribute("tableTypes", TableType.values());
        return "tables/form";
    }

    @PostMapping
    public String create(@ModelAttribute BilliardTable table, RedirectAttributes ra) {
        try {
            tableService.create(table);
            ra.addFlashAttribute("successMessage", "Стол №" + table.getNumber() + " добавлен");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tables";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("table", tableService.findById(id));
        model.addAttribute("tableTypes", TableType.values());
        return "tables/form";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute BilliardTable table, RedirectAttributes ra) {
        try {
            tableService.update(id, table);
            ra.addFlashAttribute("successMessage", "Стол обновлён");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tables";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            tableService.delete(id);
            ra.addFlashAttribute("successMessage", "Стол удалён");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tables";
    }
}
