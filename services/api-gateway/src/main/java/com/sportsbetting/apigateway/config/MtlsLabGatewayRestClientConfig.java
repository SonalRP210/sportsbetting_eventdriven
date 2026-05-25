package com.sportsbetting.apigateway.config;

import com.example.mtls.core.MtlsMaterialService;
import com.example.mtls.core.ReloadableSslContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.http.HttpClient;

/**
 * Lab-only outbound mTLS: uses {@link ReloadableSslContext} from the mtls starter (Vault-backed).
 * Disables TLS hostname verification so self-signed lab certs work with Docker service names.
 */
@Configuration
@Profile("mtls-lab")
@ConditionalOnProperty(name = "gateway.downstream.mtls.enabled", havingValue = "true")
public class MtlsLabGatewayRestClientConfig {

    @Bean
    @Primary
    RestClient gatewayRestClient(
            RestClient.Builder restClientBuilder,
            ReloadableSslContext reloadableSslContext,
            MtlsMaterialService materialService) {
        // ApplicationRunner reload runs after bean creation; load Vault material synchronously here.
        if (reloadableSslContext.getSslContext().isEmpty()) {
            materialService.reload();
        }
        SSLContext sslContext = reloadableSslContext.getSslContext()
                .orElseThrow(() -> new IllegalStateException("Gateway mTLS material not loaded; check Vault secret sportsbetting/lab/mtls/gateway"));
        SSLParameters sslParameters = sslContext.getDefaultSSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm(null);

        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .sslParameters(sslParameters)
                .build();
        return restClientBuilder
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }
}
