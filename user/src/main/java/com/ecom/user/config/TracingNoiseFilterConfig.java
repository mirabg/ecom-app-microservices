package com.ecom.user.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Keeps Zipkin readable by dropping infrastructure traffic that would otherwise
 * dominate the trace list:
 * <ul>
 *   <li>inbound actuator calls (Prometheus scrapes, health checks)</li>
 *   <li>outbound Eureka registry heartbeats / delta polling</li>
 * </ul>
 * Business traffic is still traced.
 */
@Configuration
public class TracingNoiseFilterConfig {

    @Bean
    ObservationPredicate skipInfrastructureObservations() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext serverContext) {
                return !serverContext.getCarrier().getRequestURI().startsWith("/actuator");
            }
            if (context instanceof ClientRequestObservationContext clientContext) {
                return !String.valueOf(clientContext.getCarrier().getURI()).contains("/eureka");
            }
            return true;
        };
    }
}

