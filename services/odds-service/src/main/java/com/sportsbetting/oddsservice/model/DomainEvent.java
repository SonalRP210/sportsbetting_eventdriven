package com.sportsbetting.oddsservice.model;

public record DomainEvent(
        String type,
        Object payload
) {
}
