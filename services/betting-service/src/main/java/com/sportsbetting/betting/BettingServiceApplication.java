package com.sportsbetting.betting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BettingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BettingServiceApplication.class, args);
    }
}
