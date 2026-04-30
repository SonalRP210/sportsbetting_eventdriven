package com.sportsbetting.betting.model;

public record DomainEvent(
        String type,
        Object payload
) {
}
