package com.sportsbetting.apigateway.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    record Payload(@NotBlank String name) {}

    private static MethodParameter payloadParameter() throws Exception {
        Method m = Stimulus.class.getDeclaredMethod("post", Payload.class);
        return new MethodParameter(m, 0);
    }

    @Test
    void validationMapsTo400() throws Exception {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Payload(""), "payload");
        errors.addError(new FieldError("payload", "name", "", false, null, null, "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(payloadParameter(), errors);

        ResponseEntity<?> res = new GlobalExceptionHandler().validation(ex);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isEqualTo(java.util.Map.of("code", "VALIDATION_ERROR", "message", "Request validation failed"));
    }

    @Test
    void businessMapsTo422() {
        ResponseEntity<?> res = new GlobalExceptionHandler().business(new IllegalArgumentException("nope"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody()).isEqualTo(java.util.Map.of("code", "BUSINESS_RULE_VIOLATION", "message", "nope"));
    }

    @Test
    void unexpectedMapsTo500() {
        ResponseEntity<?> res = new GlobalExceptionHandler().unexpected(new RuntimeException("x"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody()).isEqualTo(java.util.Map.of("code", "INTERNAL_ERROR", "message", "Unexpected server error"));
    }

    static class Stimulus {
        @SuppressWarnings("unused")
        void post(@Valid Payload payload) {}
    }
}
