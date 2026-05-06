package com.sportsbetting.betting.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.betting.model.BetEntity;
import com.sportsbetting.betting.model.BetStatus;
import com.sportsbetting.betting.repository.BetRepository;
import com.sportsbetting.betting.repository.ProcessedEventRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.task.scheduling.enabled=false"
)
class SettlementProjectionIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Autowired
    private BetRepository betRepository;
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "betting");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces", () -> "true");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.consumers.enabled", () -> "true");
        registry.add("spring.kafka.listener.auto-startup", () -> "true");
        registry.add("app.kafka.listener.error-handler.enabled", () -> "true");
        registry.add("app.kafka.consumer.max-attempts", () -> "3");
        registry.add("app.kafka.consumer.backoff-ms", () -> "50");
        registry.add("app.kafka.dlq.suffix", () -> ".dlq");
        registry.add("app.outbox.poll.enabled", () -> "false");
        registry.add("app.kafka.schema-validation.enabled", () -> "false");
    }

    @BeforeEach
    void resetAndEnsureTopics() throws Exception {
        processedEventRepository.deleteAll();
        betRepository.deleteAll();
        ensureTopicExists("settlement.event.settled.v1");
        ensureTopicExists("odds.updated.v1");
    }

    private static void ensureTopicExists(String name) throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (AdminClient admin = AdminClient.create(props)) {
            try {
                admin.createTopics(List.of(new NewTopic(name, 1, (short) 1))).all().get();
            } catch (ExecutionException ex) {
                if (!(ex.getCause() instanceof TopicExistsException)) {
                    throw ex;
                }
            }
        }
    }

    @Test
    void settlementKafkaMessageMovesOpenBetToWonAndSecondDeliveryIsIdempotent() throws Exception {
        String eventId = "event-settle-it-" + UUID.randomUUID().toString().substring(0, 8);
        BetEntity bet = new BetEntity();
        bet.setBetId("BET-IT-" + UUID.randomUUID().toString().substring(0, 8));
        bet.setUserId("user-it-1");
        bet.setEventId(eventId);
        bet.setSelection("HOME");
        bet.setStake(new BigDecimal("10.00"));
        bet.setOdds(new BigDecimal("2.00"));
        bet.setStatus(BetStatus.OPEN);
        betRepository.save(bet);

        String payload = objectMapper.writeValueAsString(Map.of(
                "eventId", eventId,
                "winningSelection", "HOME",
                "releases", List.of(Map.of("userId", bet.getUserId(), "openRisk", 20))
        ));

        kafkaTemplate.send("settlement.event.settled.v1", eventId, payload).get(30, TimeUnit.SECONDS);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(400))
                .untilAsserted(() ->
                        assertThat(betRepository.findById(bet.getBetId()))
                                .isPresent()
                                .get()
                                .extracting(BetEntity::getStatus)
                                .isEqualTo(BetStatus.WON));

        kafkaTemplate.send("settlement.event.settled.v1", eventId, payload).get(30, TimeUnit.SECONDS);
        Thread.sleep(800);

        assertThat(betRepository.findById(bet.getBetId()).orElseThrow().getStatus()).isEqualTo(BetStatus.WON);
        assertThat(processedEventRepository.count()).isEqualTo(1L);
    }

    @Test
    void settlementMarksAwaySelectionAsLost() throws Exception {
        String eventId = "event-settle-lost-" + UUID.randomUUID().toString().substring(0, 8);
        BetEntity bet = new BetEntity();
        bet.setBetId("BET-LOST-" + UUID.randomUUID().toString().substring(0, 8));
        bet.setUserId("user-it-2");
        bet.setEventId(eventId);
        bet.setSelection("AWAY");
        bet.setStake(new BigDecimal("5.00"));
        bet.setOdds(new BigDecimal("3.00"));
        bet.setStatus(BetStatus.OPEN);
        betRepository.save(bet);

        String payload = objectMapper.writeValueAsString(Map.of(
                "eventId", eventId,
                "winningSelection", "HOME",
                "releases", List.of(Map.of("userId", bet.getUserId(), "openRisk", 15))
        ));

        kafkaTemplate.send("settlement.event.settled.v1", eventId, payload).get(30, TimeUnit.SECONDS);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(400))
                .untilAsserted(() ->
                        assertThat(betRepository.findById(bet.getBetId()).orElseThrow().getStatus())
                                .isEqualTo(BetStatus.LOST));
    }
}
