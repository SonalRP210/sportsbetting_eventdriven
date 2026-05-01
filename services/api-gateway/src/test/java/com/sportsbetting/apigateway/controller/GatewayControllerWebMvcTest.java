package com.sportsbetting.apigateway.controller;

import com.sportsbetting.apigateway.config.RateLimitingFilter;
import com.sportsbetting.apigateway.config.RequestCorrelationFilter;
import com.sportsbetting.apigateway.error.GlobalExceptionHandler;
import com.sportsbetting.apigateway.service.GatewayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GatewayController.class)
@Import({RateLimitingFilter.class, RequestCorrelationFilter.class, GlobalExceptionHandler.class})
class GatewayControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayService gatewayService;

    @Test
    void allRoutesDelegateToService() throws Exception {
        when(gatewayService.healthV1()).thenReturn(Map.of("status", "UP"));
        when(gatewayService.health()).thenReturn(Map.of("gateway", "ok"));
        when(gatewayService.routeMap()).thenReturn(Map.of("betting", "http://b"));
        when(gatewayService.login(anyString())).thenReturn(ResponseEntity.ok("{}"));
        when(gatewayService.getUser("u1")).thenReturn(ResponseEntity.ok("user"));
        when(gatewayService.upsertUser(anyString())).thenReturn(ResponseEntity.ok("ok"));
        when(gatewayService.ingestProviderEvent(anyString())).thenReturn(ResponseEntity.ok("ok"));
        when(gatewayService.placeBet(anyString(), nullable(String.class))).thenReturn(ResponseEntity.status(201).body("bet"));
        when(gatewayService.getBet("b1")).thenReturn(ResponseEntity.ok("bet"));
        when(gatewayService.cancelBet("b1")).thenReturn(ResponseEntity.ok("canceled"));
        when(gatewayService.oddsFeed(anyString())).thenReturn(ResponseEntity.ok("feed"));
        when(gatewayService.settleEvent(anyString())).thenReturn(ResponseEntity.ok("settled"));
        when(gatewayService.userBets(eq("u1"), anyInt(), anyInt())).thenReturn(ResponseEntity.ok("[]"));
        when(gatewayService.eventBets(eq("e1"), anyInt(), anyInt())).thenReturn(ResponseEntity.ok("[]"));
        when(gatewayService.userExposure("u1")).thenReturn(ResponseEntity.ok("exp"));
        when(gatewayService.totalExposure()).thenReturn(ResponseEntity.ok("total"));
        when(gatewayService.walletBalance("u1")).thenReturn(ResponseEntity.ok("100"));

        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        // Avoid /api/v1/gateway/health here: @WebMvcTest context is cached and shares RateLimitingFilter with GatewayWebMvcSliceTest burst test.
        mockMvc.perform(get("/api/v1/gateway/routes")).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/u1")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/providers/events").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/bets").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/bets").header("Idempotency-Key", "k").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/bets/b1")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/bets/b1/cancel")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/odds-feed").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/events/settlements").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/u1/bets")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/u1/bets").param("page", "1").param("size", "10")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/events/e1/bets")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/u1/exposure")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/risk/total")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/wallet/u1/balance")).andExpect(status().isOk());
    }
}
