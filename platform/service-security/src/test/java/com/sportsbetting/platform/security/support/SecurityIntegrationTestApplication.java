package com.sportsbetting.platform.security.support;

import com.sportsbetting.platform.security.HttpApiSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = "com.sportsbetting.platform.security.support")
@Import(HttpApiSecurityAutoConfiguration.class)
public class SecurityIntegrationTestApplication {}
