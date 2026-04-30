package com.sportsbetting.walletservice.model;

public record DomainEvent(
        String type,
        Object payload
) {
}
