package com.sportsbetting.userservice.controller;

import com.sportsbetting.userservice.model.UserProfile;
import com.sportsbetting.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
class UserWebMvcSliceTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @Test
    void getUserReturns200WhenPresent() throws Exception {
        when(userService.get("u1")).thenReturn(Optional.of(new UserProfile("u1", "u1@example.com", "ACTIVE")));
        mockMvc.perform(get("/api/v1/users/u1")).andExpect(status().isOk());
    }

    @Test
    void upsertReturns200() throws Exception {
        when(userService.upsert(new UserProfile("u2", "u2@example.com", "ACTIVE")))
                .thenReturn(new UserProfile("u2", "u2@example.com", "ACTIVE"));
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("{\"userId\":\"u2\",\"email\":\"u2@example.com\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
    }
}
