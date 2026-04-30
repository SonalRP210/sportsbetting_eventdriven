package com.sportsbetting.settlementservice.model;

public record DomainEvent(
        String type,
        Object payload
) {
}
