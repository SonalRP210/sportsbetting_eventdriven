package com.sportsbetting.platform.messaging.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportsbetting.platform.messaging.EventPayloadValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(name = "app.kafka.schema-validation.enabled", havingValue = "true")
public class EventMessagingAutoConfiguration {

    @Bean
    public EventPayloadValidator eventPayloadValidator(ObjectMapper objectMapper) {
        return new EventPayloadValidator(objectMapper);
    }
}
