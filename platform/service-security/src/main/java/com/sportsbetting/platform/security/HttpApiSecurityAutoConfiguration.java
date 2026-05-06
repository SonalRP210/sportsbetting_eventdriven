package com.sportsbetting.platform.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.web.SecurityFilterChain;

@AutoConfiguration
@ConditionalOnClass(SecurityFilterChain.class)
@EnableConfigurationProperties(HttpApiSecurityProperties.class)
@Import(HttpApiSecurityConfiguration.class)
public class HttpApiSecurityAutoConfiguration {

    @Bean
    HttpApiSecurityBootstrap httpApiSecurityBootstrap(HttpApiSecurityProperties props, Environment env) {
        return new HttpApiSecurityBootstrap(props, env);
    }
}
