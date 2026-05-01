package com.sportsbetting.apigateway.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GatewayServiceTest {

    private static final String BETTING = "http://betting.local";
    private static final String RISK = "http://risk.local";
    private static final String WALLET = "http://wallet.local";
    private static final String AUTH = "http://auth.local";
    private static final String USER = "http://user.local";
    private static final String INGESTION = "http://ingestion.local";
    private static final String SETTLEMENT = "http://settlement.local";
    private static final String ODDS = "http://odds.local";

    private MockRestServiceServer server;
    private GatewayService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        service = new GatewayService(
                client,
                "api-gateway",
                BETTING,
                RISK,
                WALLET,
                AUTH,
                USER,
                INGESTION,
                SETTLEMENT,
                ODDS
        );
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void healthAndRoutes() {
        assertThat(service.health()).containsEntry("gateway", "ok");
        assertThat(service.healthV1()).containsKeys("status", "service", "timestamp");
        assertThat(service.routeMap()).containsEntry("betting", BETTING);
    }

    @Test
    void placeBetSuccessWithIdempotencyKey() {
        server.expect(requestTo(BETTING + "/api/v1/bets"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "idem-1"))
                .andExpect(content().string("{}"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{\"id\":\"b1\"}"));

        ResponseEntity<String> res = service.placeBet("{}", "idem-1");
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        assertThat(res.getBody()).isEqualTo("{\"id\":\"b1\"}");
    }

    @Test
    void placeBetPropagatesUpstreamError() {
        server.expect(requestTo(BETTING + "/api/v1/bets"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body("bad"));

        ResponseEntity<String> res = service.placeBet("{}", null);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(res.getBody()).isEqualTo("bad");
    }

    @Test
    void getBetCancelBetOddsFeedAndListings() {
        server.expect(requestTo(BETTING + "/api/v1/bets/b1")).andExpect(method(HttpMethod.GET)).andRespond(withSuccess("bet", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BETTING + "/api/v1/bets/b1/cancel")).andExpect(method(HttpMethod.POST)).andRespond(withSuccess("canceled", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ODDS + "/api/v1/odds-feed")).andExpect(method(HttpMethod.POST)).andRespond(withSuccess("ok", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BETTING + "/api/v1/users/u1/bets?page=0&size=20")).andExpect(method(HttpMethod.GET)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BETTING + "/api/v1/events/e1/bets?page=1&size=5")).andExpect(method(HttpMethod.GET)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(service.getBet("b1").getBody()).isEqualTo("bet");
        assertThat(service.cancelBet("b1").getBody()).isEqualTo("canceled");
        assertThat(service.oddsFeed("{}").getBody()).isEqualTo("ok");
        assertThat(service.userBets("u1", 0, 20).getBody()).isEqualTo("[]");
        assertThat(service.eventBets("e1", 1, 5).getBody()).isEqualTo("[]");
    }

    @Test
    void riskWalletAuthUserIngestionSettlement() {
        server.expect(requestTo(RISK + "/api/v1/risk/users/u1")).andExpect(method(HttpMethod.GET)).andRespond(withSuccess("exp", MediaType.APPLICATION_JSON));
        server.expect(requestTo(RISK + "/api/v1/risk/total")).andExpect(method(HttpMethod.GET)).andRespond(withSuccess("total", MediaType.APPLICATION_JSON));
        server.expect(requestTo(SETTLEMENT + "/api/v1/events/settlements")).andExpect(method(HttpMethod.POST)).andRespond(withSuccess("settled", MediaType.APPLICATION_JSON));
        server.expect(requestTo(WALLET + "/api/v1/wallet/u1/balance")).andExpect(method(HttpMethod.GET)).andRespond(withSuccess("100", MediaType.APPLICATION_JSON));
        server.expect(requestTo(AUTH + "/api/v1/auth/login")).andExpect(method(HttpMethod.POST)).andRespond(withSuccess("token", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER + "/api/v1/users/u1")).andExpect(method(HttpMethod.GET)).andRespond(withSuccess("user", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER + "/api/v1/users")).andExpect(method(HttpMethod.POST)).andRespond(withSuccess("upserted", MediaType.APPLICATION_JSON));
        server.expect(requestTo(INGESTION + "/api/v1/providers/events")).andExpect(method(HttpMethod.POST)).andRespond(withSuccess("ingested", MediaType.APPLICATION_JSON));

        assertThat(service.userExposure("u1").getBody()).isEqualTo("exp");
        assertThat(service.totalExposure().getBody()).isEqualTo("total");
        assertThat(service.settleEvent("{}").getBody()).isEqualTo("settled");
        assertThat(service.walletBalance("u1").getBody()).isEqualTo("100");
        assertThat(service.login("{}").getBody()).isEqualTo("token");
        assertThat(service.getUser("u1").getBody()).isEqualTo("user");
        assertThat(service.upsertUser("{}").getBody()).isEqualTo("upserted");
        assertThat(service.ingestProviderEvent("{}").getBody()).isEqualTo("ingested");
    }

    @Test
    void forwardReturnsEmptyStringWhenBodyNull() {
        server.expect(requestTo(RISK + "/api/v1/risk/total"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        ResponseEntity<String> res = service.totalExposure();
        assertThat(res.getBody()).isEqualTo("");
    }
}
