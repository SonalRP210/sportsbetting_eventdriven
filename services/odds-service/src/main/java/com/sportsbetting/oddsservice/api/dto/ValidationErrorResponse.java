package com.sportsbetting.oddsservice.api.dto;

import java.util.List;

/**
 * Body for Bean Validation failures on request bodies ({@code 400}).
 */
public record ValidationErrorResponse(List<String> errors) {
}
