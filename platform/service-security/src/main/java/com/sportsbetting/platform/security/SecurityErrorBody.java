package com.sportsbetting.platform.security;

public record SecurityErrorBody(String error) {

    public static SecurityErrorBody unauthorized() {
        return new SecurityErrorBody("Unauthorized");
    }

    public static SecurityErrorBody forbidden() {
        return new SecurityErrorBody("Forbidden");
    }
}
