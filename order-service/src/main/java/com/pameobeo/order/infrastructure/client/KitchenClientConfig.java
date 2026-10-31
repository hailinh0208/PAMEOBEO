package com.pameobeo.order.infrastructure.client;

import com.pameobeo.order.application.port.out.KitchenGateway;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
class KitchenClientConfig {

    @Bean
    KitchenGateway kitchenGateway(@Value("${kitchen.base-url}") String baseUrl,
                                  @Value("${kitchen.connect-timeout}") Duration connectTimeout,
                                  @Value("${kitchen.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                // Access Token pattern: forward the caller's JWT so the kitchen knows who is asking
                .requestInterceptor((request, body, execution) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth instanceof JwtAuthenticationToken jwt) {
                        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getToken().getTokenValue());
                    }
                    return execution.execute(request, body);
                })
                .build();
        return new KitchenRestClient(restClient);
    }
}
