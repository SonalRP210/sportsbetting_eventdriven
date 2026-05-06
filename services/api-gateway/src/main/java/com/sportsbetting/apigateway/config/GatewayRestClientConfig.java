package com.sportsbetting.apigateway.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.client.RestClientSsl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Optional TLS for outbound calls: set {@code gateway.downstream.ssl.bundle-name} to a
 * {@link org.springframework.boot.ssl.SslBundles} name (PEM/JKS from Vault-injected files or classpath)
 * when backends use HTTPS / mTLS.
 */
@Configuration
public class GatewayRestClientConfig {

    @Bean
    RestClient gatewayRestClient(
            RestClient.Builder restClientBuilder,
            ObjectProvider<RestClientSsl> restClientSslProvider,
            @Value("${gateway.downstream.ssl.bundle-name:}") String sslBundleName
    ) {
        RestClientSsl ssl = restClientSslProvider.getIfAvailable();
        if (ssl != null && sslBundleName != null && !sslBundleName.isBlank()) {
            return restClientBuilder
                    .apply(ssl.fromBundle(sslBundleName))
                    .build();
        }
        return restClientBuilder.build();
    }
}
