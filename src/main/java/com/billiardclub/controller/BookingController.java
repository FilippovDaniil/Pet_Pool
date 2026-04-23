package com.billiardclub.controller;

import com.billiardclub.dto.BookingRequestDto;
import com.billiardclub.dto.PaymentDto;
import com.billiardclub.model.Booking;
import com.billiardclub.model.BookingStatus;
import com.billiardclub.model.BilliardTable;
import com.billiardclub.model.Game;
import com.billiardclub.service.BookingService;
import com.billiardclub.service.ClientService;
import com.billiardclub.service.GameService;
import com.billiardclub.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final TableService tableService;
    private final ClientService clientService;
    private final GameService gameService;

    @GetMapping
    public String list(@RequestParam(required = false) BookingStatus status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Model model) {
        List<Booking> bookings;
        if (status != null) {
            bookings = bookingService.findByStatus(status);
        } else if (date != null) {
            bookings = bookingService.findByDate(date);
        } else {
            bookings = bookingService.findAll();
        }

        // Attach game info for ACTIVE bookings
        Map<Long, Game> activeGames = new java.util.HashMap<>();
        bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.ACTIVE)
                .forEach(b -> {
                    Game g = gameService.findByBookingId(b.getId());
                    if (g != null) activeGames.put(b.getId(), g);
                });

        model.addAttribute("bookings", bookings);
        model.addAttribute("activeGames", activeGames);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedDate", date);
        model.addAttribute("clients", clientService.findAll());
        return "bookings/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dto", new BookingRequestDto());
        model.addAttribute("tables", tableService.findAll());
        model.addAttribute("clients", clientService.findAll());
        return "bookings/new";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("dto") BookingRequestDto dto,
                         BindingResult br, Model model, RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("tables", tableService.findAll());
            model.addAttribute("clients", clientService.findAll());
            return "bookings/new";
        }
        try {
            bookingService.create(dto);
            ra.addFlashAttribute("successMessage", "Бронирование создано");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings/new";
        }
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/pay")
    public String pay(@PathVariable Long id,
                      @RequestParam(defaultValue = "CASH") String paymentMethod,
                      RedirectAttributes ra) {
        try {
            PaymentDto dto = new PaymentDto();
            dto.setPaymentMethod(paymentMethod);
            bookingService.pay(id, dto);
            ra.addFlashAttribute("successMessage", "Оплата принята");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        try {
            bookingService.cancel(id);
            ra.addFlashAttribute("successMessage", "Бронирование отменено");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/start-game")
    public String startGame(@PathVariable Long id,
                            @RequestParam Long client2Id,
                            RedirectAttributes ra) {
        try {
            gameService.startGame(id, client2Id);
            ra.addFlashAttribute("successMessage", "Игра начата");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/finish-game")
    public String finishGame(@PathVariable Long id,
                             @RequestParam Long winnerId,
                             RedirectAttributes ra) {
        try {
            gameService.finishGame(id, winnerId);
            ra.addFlashAttribute("successMessage", "Игра завершена. Результат записан в турнирную доску");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bookings";
    }

    // AJAX endpoint: calculate price
    @GetMapping("/calculate-price")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> calculatePrice(
            @RequestParam Long tableId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endTime) {
        try {
            BilliardTable table = tableService.findById(tableId);
            BigDecimal price = bookingService.calculatePrice(table, startTime, endTime);
            return ResponseEntity.ok(Map.of("price", price, "pricePerHour", table.getPricePerHour()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
