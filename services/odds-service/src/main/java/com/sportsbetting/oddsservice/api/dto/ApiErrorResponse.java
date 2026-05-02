package com.sportsbetting.oddsservice.api.dto;

/**
 * Generic client error body ({@code 400}) for illegal arguments from domain/services.
 */
public record ApiErrorResponse(String error) {
}
