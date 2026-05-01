package com.sportsbetting.notificationservice.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "app.kafka.listener.error-handler.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaListenerInfrastructureConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.consumer.max-attempts:3}") long maxAttempts,
            @Value("${app.kafka.consumer.backoff-ms:1000}") long backoffMs,
            @Value("${app.kafka.dlq.suffix:.dlq}") String dlqSuffix
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + dlqSuffix, record.partition())
        );
        long retries = Math.max(0, maxAttempts - 1);
        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, new FixedBackOff(backoffMs, retries)));
        return factory;
    }
}
