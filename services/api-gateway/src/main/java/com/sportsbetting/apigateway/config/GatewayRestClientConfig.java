package com.sportsbetting.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GatewayRestClientConfig {

    @Bean
    public RestClient gatewayRestClient() {
        return RestClient.create();
    }
}
