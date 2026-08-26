package com.ecom.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    // These must match the orders service's exchange/queue/routing-key
    // (rabbitmq.exchange.* in config-server's order-service.yaml), not
    // spring.rabbitmq.* - that prefix is reserved for the broker connection
    // settings (host/port/username/password).
    @Value("${rabbitmq.exchange.queue.name:order-queue}")
    private String queueName;
    @Value("${rabbitmq.exchange.name:order-exchange}")
    private String exchangeName;
    @Value("${rabbitmq.exchange.routing-key:order-routing-key}")
    private String routingKey;

    @Bean
    public Queue queue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public TopicExchange exchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setExchange(exchangeName);
        rabbitTemplate.setRoutingKey(routingKey);
        return rabbitTemplate;
    }
}

