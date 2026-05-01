package com.sportsbetting.settlementservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = KafkaListenerInfrastructureConfig.class)
@TestPropertySource(properties = "app.kafka.listener.error-handler.enabled=true")
class KafkaListenerInfrastructureConfigTest {

    @MockBean
    private ConsumerFactory<String, String> consumerFactory;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory;

    @Test
    void factoryCreated() {
        assertThat(kafkaListenerContainerFactory).isNotNull();
        assertThat(kafkaListenerContainerFactory.getConsumerFactory()).isSameAs(consumerFactory);
    }
}
