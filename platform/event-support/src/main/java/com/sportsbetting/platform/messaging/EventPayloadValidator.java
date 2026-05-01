package com.sportsbetting.platform.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates outbound JSON payloads against classpath {@code domain-schemas/&lt;eventType&gt;.json}
 * when a schema file is present. Missing schema is a no-op (allows gradual rollout).
 */
public class EventPayloadValidator {

    private static final Logger log = LoggerFactory.getLogger(EventPayloadValidator.class);

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private final Map<String, Optional<JsonSchema>> cache = new ConcurrentHashMap<>();

    public EventPayloadValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validateIfPresent(String eventType, String payload) {
        Optional<JsonSchema> schema = cache.computeIfAbsent(eventType, this::loadSchemaOptional);
        if (schema.isEmpty()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            Set<ValidationMessage> errors = schema.get().validate(node);
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("Event schema validation failed for " + eventType + ": " + errors);
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid JSON payload for " + eventType, ex);
        }
    }

    private Optional<JsonSchema> loadSchemaOptional(String eventType) {
        String location = "classpath:domain-schemas/" + eventType + ".json";
        try {
            Resource resource = new PathMatchingResourcePatternResolver().getResource(location);
            if (!resource.exists()) {
                return Optional.empty();
            }
            try (InputStream in = resource.getInputStream()) {
                JsonNode schemaNode = objectMapper.readTree(in);
                return Optional.of(schemaFactory.getSchema(schemaNode));
            }
        } catch (IOException ex) {
            log.warn("Failed to load JSON schema for event type {}", eventType, ex);
            return Optional.empty();
        }
    }
}
