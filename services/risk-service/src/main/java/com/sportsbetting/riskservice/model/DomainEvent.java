package com.sportsbetting.riskservice.model;

public record DomainEvent(
        String type,
        Object payload
) {
}
