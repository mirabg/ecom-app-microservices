package com.ecom.apigateway.config;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
@Requir
public class GatewayConfig {
    @Bean
    RouteLocator routeLocator(RouteLocator.Builder builder) {
        return builder.routes()
                .route("product-service", r -> r.path("/api/products/**")
                        .uri("lb://PRODUCT-SERVICE"))
                .route("order-service", r -> r.path("/api/orders/**")
                        .uri("lb://ORDER-SERVICE"))
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("lb://USER-SERVICE"))
                .build();
    }
}
