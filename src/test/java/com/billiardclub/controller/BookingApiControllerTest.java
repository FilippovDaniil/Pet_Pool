package com.billiardclub.controller;

import com.billiardclub.config.SecurityConfig;
import com.billiardclub.dto.BookingRequestDto;
import com.billiardclub.model.*;
import com.billiardclub.service.BookingService;
import com.billiardclub.service.GameService;
import com.billiardclub.service.TableService;
import com.billiardclub.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingApiController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"opensearch.enabled=false"})
class BookingApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private GameService gameService;

    @MockBean
    private TableService tableService;

    // Required by SecurityConfig → DaoAuthenticationProvider
    @MockBean
    private UserService userService;

    private ObjectMapper json;
    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        json = new ObjectMapper().registerModule(new JavaTimeModule());

        Client client = new Client(1L, "Иванов Иван", "Любитель", "+7-900-000-0001");
        BilliardTable table = new BilliardTable();
        table.setId(1L);
        table.setNumber(3);
        table.setType(TableType.RUSSIAN);

        sampleBooking = Booking.builder()
                .id(1L)
                .client(client)
                .table(table)
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(3))
                .status(BookingStatus.PENDING)
                .totalPrice(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── GET /api/bookings ────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void list_returnsBookings() throws Exception {
        when(bookingService.findAll()).thenReturn(List.of(sampleBooking));

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].clientName").value("Иванов Иван"))
                .andExpect(jsonPath("$[0].tableNumber").value(3));
    }

    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/bookings ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    void create_validRequest_returns201() throws Exception {
        when(bookingService.create(any(BookingRequestDto.class))).thenReturn(sampleBooking);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setTableId(1L);
        dto.setClientId(1L);
        dto.setStartTime(LocalDateTime.now().plusHours(1));
        dto.setEndTime(LocalDateTime.now().plusHours(3));

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser
    void create_missingTableId_returns400() throws Exception {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setClientId(1L);
        dto.setStartTime(LocalDateTime.now().plusHours(1));
        dto.setEndTime(LocalDateTime.now().plusHours(3));
        // tableId is null → @NotNull → 400

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /api/bookings/{id} — cancel ────────────────────────────────────

    @Test
    @WithMockUser
    void patch_cancel_returns200() throws Exception {
        doNothing().when(bookingService).cancel(1L);

        Booking cancelled = Booking.builder()
                .id(1L).client(sampleBooking.getClient()).table(sampleBooking.getTable())
                .startTime(sampleBooking.getStartTime()).endTime(sampleBooking.getEndTime())
                .status(BookingStatus.CANCELLED).totalPrice(sampleBooking.getTotalPrice())
                .createdAt(sampleBooking.getCreatedAt()).build();
        when(bookingService.findById(1L)).thenReturn(cancelled);

        mockMvc.perform(patch("/api/bookings/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ── PATCH /api/bookings/{id} — start game ────────────────────────────────

    @Test
    @WithMockUser
    void patch_startGame_missingOpponentId_returns400() throws Exception {
        mockMvc.perform(patch("/api/bookings/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void patch_startGame_validRequest_returns200() throws Exception {
        Booking activeBooking = Booking.builder()
                .id(1L).client(sampleBooking.getClient()).table(sampleBooking.getTable())
                .startTime(sampleBooking.getStartTime()).endTime(sampleBooking.getEndTime())
                .status(BookingStatus.ACTIVE).totalPrice(sampleBooking.getTotalPrice())
                .createdAt(sampleBooking.getCreatedAt()).build();

        doReturn(null).when(gameService).startGame(eq(1L), eq(2L));
        when(bookingService.findById(1L)).thenReturn(activeBooking);

        mockMvc.perform(patch("/api/bookings/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\",\"opponentId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ── POST /api/bookings/{id}/payments ─────────────────────────────────────

    @Test
    @WithMockUser
    void pay_validRequest_returns201() throws Exception {
        doNothing().when(bookingService).pay(eq(1L), any());

        Booking paidBooking = Booking.builder()
                .id(1L).client(sampleBooking.getClient()).table(sampleBooking.getTable())
                .startTime(sampleBooking.getStartTime()).endTime(sampleBooking.getEndTime())
                .status(BookingStatus.PAID).totalPrice(sampleBooking.getTotalPrice())
                .createdAt(sampleBooking.getCreatedAt()).build();
        when(bookingService.findById(1L)).thenReturn(paidBooking);

        mockMvc.perform(post("/api/bookings/1/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"));
    }

    // ── GET /api/bookings/price ──────────────────────────────────────────────

    @Test
    @WithMockUser
    void price_validParams_returns200() throws Exception {
        BilliardTable table = sampleBooking.getTable();
        when(tableService.findById(1L)).thenReturn(table);
        when(bookingService.calculatePrice(eq(table), any(), any()))
                .thenReturn(new BigDecimal("1000.00"));

        mockMvc.perform(get("/api/bookings/price")
                        .param("tableId", "1")
                        .param("startTime", "2025-06-01T10:00")
                        .param("endTime", "2025-06-01T12:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(1000.00));
    }
}
