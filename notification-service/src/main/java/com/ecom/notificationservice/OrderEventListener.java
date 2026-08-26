package com.ecom.notificationservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderEventListener {

    // This method will be called when a message is received from the RabbitMQ queue
    @RabbitListener(queues = "${rabbitmq.exchange.queue.name:order-queue}")
    public void handleOrderEvent(String message) {
        log.info("Received order event: {}", message);
    }
}
