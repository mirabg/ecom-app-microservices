package com.ecom.orders.client;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder(ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        return configureObservation(RestClient.builder(), observationRegistryProvider);
    }

    @Bean(name = "loadBalancedRestClientBuilder")
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder(ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        return configureObservation(RestClient.builder(), observationRegistryProvider);
    }

    private RestClient.Builder configureObservation(RestClient.Builder builder,
                                                    ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        ObservationRegistry observationRegistry = observationRegistryProvider.getIfAvailable();
        if (observationRegistry != null) {
            return builder.observationRegistry(observationRegistry);
        }
        return builder;
    }


}
