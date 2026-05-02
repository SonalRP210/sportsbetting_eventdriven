package com.sportsbetting.betting.integration;

import com.sportsbetting.betting.model.OutboxEventEntity;
import com.sportsbetting.betting.repository.OutboxEventRepository;
import com.sportsbetting.betting.outbox.OutboxDispatcher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OddsConsumerDlqIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Autowired
    private OutboxDispatcher outboxDispatcher;
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.consumers.enabled", () -> "false");
        registry.add("app.kafka.listener.error-handler.enabled", () -> "true");
        registry.add("app.kafka.consumer.max-attempts", () -> "2");
        registry.add("app.kafka.consumer.backoff-ms", () -> "50");
        registry.add("app.kafka.dlq.suffix", () -> ".dlq");
        registry.add("app.outbox.poll.enabled", () -> "true");
    }

    @Test
    void outboxDispatcherPublishesToKafkaAndMarksEventAsPublished() {
        OutboxEventEntity row = new OutboxEventEntity();
        row.setId(UUID.randomUUID());
        row.setEventType("odds.odds.updated.v1");
        row.setPayload("{\"eventId\":\"event-it-1\",\"selection\":\"HOME\",\"odds\":2.42}");
        row.setPublished(false);
        row.setCreatedAt(Instant.now());
        outboxEventRepository.save(row);

        int sent = outboxDispatcher.dispatchPending();
        assertThat(sent).isEqualTo(1);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    OutboxEventEntity stored = outboxEventRepository.findById(row.getId()).orElse(null);
                    assertThat(stored).isNotNull();
                    assertThat(stored.isPublished()).isTrue();
                    assertThat(stored.getPublishedAt()).isNotNull();

                    List<ConsumerRecord<String, String>> records = consumeFrom("odds.odds.updated.v1");
                    assertThat(records).isNotEmpty();
                    assertThat(records.get(0).value()).contains("\"eventId\":\"event-it-1\"");
                });
    }

    private List<ConsumerRecord<String, String>> consumeFrom(String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-outbox-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            ConsumerRecords<String, String> polled = consumer.poll(Duration.ofSeconds(1));
            List<ConsumerRecord<String, String>> out = new ArrayList<>();
            for (ConsumerRecord<String, String> record : polled.records(topic)) {
                out.add(record);
            }
            return out;
        }
    }
}
