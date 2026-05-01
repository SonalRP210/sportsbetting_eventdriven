package com.sportsbetting.apigateway.integration;

import com.sportsbetting.apigateway.service.GatewayService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApiIntegrationTest {

    @LocalServerPort
    int port;

    @MockBean
    GatewayService gatewayService;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        when(gatewayService.healthV1()).thenReturn(Map.of("status", "UP", "service", "api-gateway", "timestamp", "now"));
        when(gatewayService.login("{\"username\":\"u\",\"password\":\"p\"}"))
                .thenReturn(ResponseEntity.ok("{\"token\":\"ok\"}"));
    }

    @Test
    void healthEndpointReturnsMonolithCompatiblePayload() {
        given()
                .when()
                .get("/api/v1/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void authLoginIsExposedThroughGatewayRoute() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"u\",\"password\":\"p\"}")
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body(containsString("\"token\":\"ok\""));
    }
}
