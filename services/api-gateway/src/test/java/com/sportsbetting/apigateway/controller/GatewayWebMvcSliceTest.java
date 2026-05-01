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
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GatewayController.class)
@Import({RateLimitingFilter.class, RequestCorrelationFilter.class, GlobalExceptionHandler.class})
class GatewayWebMvcSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayService gatewayService;

    @Test
    void requestCorrelationFilterAddsHeader() throws Exception {
        when(gatewayService.healthV1()).thenReturn(Map.of("status", "UP"));
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void requestCorrelationFilterEchoesProvidedRequestId() throws Exception {
        when(gatewayService.healthV1()).thenReturn(Map.of("status", "UP"));
        mockMvc.perform(get("/api/v1/health").header("X-Request-Id", "fixed-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "fixed-id"));
    }

    @Test
    void globalHandlerMapsIllegalArgument() throws Exception {
        when(gatewayService.getUser("missing-user"))
                .thenThrow(new IllegalArgumentException("Invalid user id"));
        mockMvc.perform(get("/api/v1/users/missing-user"))
                .andExpect(status().isUnprocessableEntity());
    }
}
