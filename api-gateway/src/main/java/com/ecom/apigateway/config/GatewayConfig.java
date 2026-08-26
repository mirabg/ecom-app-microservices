package com.ecom.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    // A "${...}" placeholder inside a plain Java string literal is NOT resolved
    // by Spring - only fields bound via @Value (or property binding) go through
    // placeholder resolution. Inject it here instead of inlining the literal
    // below (inlining it caused: URISyntaxException: Illegal character in
    // scheme name at index 0).
    @Value("${EUREKA_DASHBOARD_URL:http://localhost:8761}")
    private String eurekaDashboardUrl;

    @Bean
    RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("product-service", r -> r.path("/api/products/**")
                        .filters(f -> f.circuitBreaker(config ->
                                config.setName("gatewayCircuitBreaker")
                                .setFallbackUri("forward:/fallback")))
                        .uri("lb://PRODUCT-SERVICE"))

                .route("order-service", r -> r.path("/api/orders/**", "/api/cart/**")
                        .uri("lb://ORDER-SERVICE"))
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("lb://USER-SERVICE"))
                .route("eureka-dashboard", r -> r.path("/eureka/dashboard/**")
                        .filters(f -> f.setPath("/"))
                        .uri(eurekaDashboardUrl))
                .route("eureka-dashboard-assets", r -> r.path("/eureka/eureka/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(eurekaDashboardUrl))

                .build();
    }
}



