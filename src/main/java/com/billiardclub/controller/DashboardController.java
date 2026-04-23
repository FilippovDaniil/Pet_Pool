package com.billiardclub.controller;

import com.billiardclub.model.BookingStatus;
import com.billiardclub.service.BookingService;
import com.billiardclub.service.ClientService;
import com.billiardclub.service.TableService;
import com.billiardclub.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final BookingService bookingService;
    private final ClientService clientService;
    private final TableService tableService;
    private final TournamentService tournamentService;

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalTables", tableService.findAll().size());
        model.addAttribute("totalClients", clientService.findAll().size());
        model.addAttribute("activeBookings", bookingService.findByStatus(BookingStatus.ACTIVE).size());
        model.addAttribute("recentRecords", tournamentService.findAll().stream().limit(5).toList());
        return "dashboard";
    }
}
