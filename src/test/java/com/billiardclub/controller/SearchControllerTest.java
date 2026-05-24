package com.billiardclub.controller;

import com.billiardclub.config.SecurityConfig;
import com.billiardclub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"opensearch.enabled=false"})
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Required by SecurityConfig
    @MockBean
    private UserService userService;

    @Test
    void searchClients_noAuth_returns200() throws Exception {
        // /api/search/** is permitAll in SecurityConfig
        mockMvc.perform(get("/api/search/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opensearchEnabled").value(false))
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void searchClients_withQuery_returns200WhenDisabled() throws Exception {
        mockMvc.perform(get("/api/search/clients").param("q", "иванов"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opensearchEnabled").value(false))
                .andExpect(jsonPath("$.results").isEmpty());
    }
}
