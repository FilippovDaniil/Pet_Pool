package com.billiardclub.controller;

import com.billiardclub.config.SecurityConfig;
import com.billiardclub.model.TournamentRecord;
import com.billiardclub.service.TournamentService;
import com.billiardclub.service.UserService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TournamentApiController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"opensearch.enabled=false"})
class TournamentApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TournamentService tournamentService;

    @MockBean
    private UserService userService;

    private TournamentRecord sampleRecord;

    @BeforeEach
    void setUp() {
        sampleRecord = TournamentRecord.builder()
                .id(1L)
                .winnerName("Иванов Иван")
                .loserName("Петров Пётр")
                .tableNumber(5)
                .gameDate(LocalDateTime.of(2025, 6, 1, 14, 0))
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── GET /api/tournament ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    void list_returnsRecords() throws Exception {
        when(tournamentService.findAll()).thenReturn(List.of(sampleRecord));

        mockMvc.perform(get("/api/tournament"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].winnerName").value("Иванов Иван"))
                .andExpect(jsonPath("$[0].loserName").value("Петров Пётр"))
                .andExpect(jsonPath("$[0].tableNumber").value(5));
    }

    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/tournament"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/tournament/{id} ─────────────────────────────────────────────

    @Test
    @WithMockUser
    void getById_existing_returns200() throws Exception {
        when(tournamentService.findById(1L)).thenReturn(sampleRecord);

        mockMvc.perform(get("/api/tournament/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winnerName").value("Иванов Иван"));
    }

    // ── PUT /api/tournament/{id} ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_adminCanUpdate() throws Exception {
        TournamentRecord updated = TournamentRecord.builder()
                .id(1L).winnerName("Сидоров Алексей").loserName("Козлов Дмитрий")
                .tableNumber(5).gameDate(sampleRecord.getGameDate()).createdAt(sampleRecord.getCreatedAt())
                .build();
        when(tournamentService.update(eq(1L), eq("Сидоров Алексей"), eq("Козлов Дмитрий")))
                .thenReturn(updated);

        mockMvc.perform(put("/api/tournament/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"winnerName\":\"Сидоров Алексей\",\"loserName\":\"Козлов Дмитрий\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winnerName").value("Сидоров Алексей"));
    }

    @Test
    @WithMockUser(roles = "RECEPTION")
    void update_nonAdminForbidden() throws Exception {
        mockMvc.perform(put("/api/tournament/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"winnerName\":\"X\",\"loserName\":\"Y\"}"))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/tournament/{id} ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_adminCanDelete() throws Exception {
        doNothing().when(tournamentService).delete(1L);

        mockMvc.perform(delete("/api/tournament/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(tournamentService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "RECEPTION")
    void delete_nonAdminForbidden() throws Exception {
        mockMvc.perform(delete("/api/tournament/1").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
