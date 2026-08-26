package com.ecom.apigateway.filters;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// Global rate limiter applied to every request that reaches the gateway,
// backed by Resilience4j's in-memory RateLimiter (config in
// config-server's api-gateway.yaml under resilience4j.ratelimiter). No
// external store (e.g. Redis) is required, unlike Spring Cloud Gateway's
// built-in RequestRateLimiter/RedisRateLimiter.
@Component
@Slf4j
public class RateLimiterFilter implements GlobalFilter, Ordered {

    private final RateLimiter rateLimiter;

    public RateLimiterFilter(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiter = rateLimiterRegistry.rateLimiter("gatewayRateLimiter");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .onErrorResume(RequestNotPermitted.class, ex -> {
                    log.warn("Rate limit exceeded for {} {}",
                            exchange.getRequest().getMethod(), exchange.getRequest().getURI());
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    return exchange.getResponse().setComplete();
                });
    }

    @Override
    public int getOrder() {
        // Run before JwtAuthFilter/business filters, so throttled requests
        // are rejected as cheaply as possible.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

