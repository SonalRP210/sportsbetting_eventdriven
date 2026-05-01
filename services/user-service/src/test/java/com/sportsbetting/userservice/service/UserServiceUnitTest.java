package com.sportsbetting.userservice.service;

import com.sportsbetting.userservice.model.UserProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceUnitTest {

    @Test
    void upsertAndGetRoundTrip() {
        UserService service = new UserService();
        UserProfile profile = new UserProfile("u-2", "u2@example.com", "ACTIVE");

        service.upsert(profile);

        assertThat(service.get("u-2")).contains(profile);
    }

    @Test
    void seededUserExists() {
        UserService service = new UserService();
        assertThat(service.get("user-1")).isPresent();
    }
}
