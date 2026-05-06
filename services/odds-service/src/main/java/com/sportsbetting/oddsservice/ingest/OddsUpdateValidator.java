package com.sportsbetting.oddsservice.ingest;

import com.sportsbetting.oddsservice.model.odds.OddsUpdate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single place for {@link OddsUpdate} Bean Validation used on programmatic ingest paths.
 * HTTP still uses {@code @Valid} on controllers for early 400 responses and OpenAPI clarity.
 */
@Component
public class OddsUpdateValidator {

    private final Validator validator;

    public OddsUpdateValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(OddsUpdate update) {
        Set<ConstraintViolation<OddsUpdate>> violations = validator.validate(update);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException(msg);
        }
    }

    public void validateAll(Collection<OddsUpdate> updates) {
        for (OddsUpdate update : updates) {
            validate(update);
        }
    }
}
