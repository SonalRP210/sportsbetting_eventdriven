package com.sportsbetting.oddsservice.controller;

import com.sportsbetting.oddsservice.service.OddsService;
import com.sportsbetting.oddsservice.service.OutboxDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OddsController.class)
class OddsWebMvcSliceTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    OddsService oddsService;
    @MockBean
    OutboxDispatcher outboxDispatcher;

    @Test
    void oddsFeedReturnsAccepted() throws Exception {
        mockMvc.perform(post("/api/v1/odds-feed")
                        .contentType("application/json")
                        .content("[{\"eventId\":\"e1\",\"selection\":\"HOME\",\"odds\":2.10}]"))
                .andExpect(status().isAccepted());
    }

    @Test
    void getOddsReturnsOkWhenPresent() throws Exception {
        when(oddsService.getOdds("e1", "HOME")).thenReturn(Optional.of(new BigDecimal("2.10")));
        mockMvc.perform(get("/api/v1/odds/e1/HOME")).andExpect(status().isOk());
    }
}
