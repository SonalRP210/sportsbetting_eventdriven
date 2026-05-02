package com.sportsbetting.platform.security.support;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestApiController {

    @GetMapping("/api/ping")
    String ping() {
        return "pong";
    }
}
