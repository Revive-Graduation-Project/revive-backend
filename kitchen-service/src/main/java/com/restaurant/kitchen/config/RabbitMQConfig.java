package com.restaurant.kitchen.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queues.user-created.name}")
    private String userCreatedQueue;

    @Value("${app.rabbitmq.queues.user-created.routing-key}")
    private String userCreatedRoutingKey;

    @Value("${app.rabbitmq.queues.user-created.dlq-name}")
    private String userCreatedDLQName;

    @Value("${app.rabbitmq.queues.user-created.dlq-routing-key}")
    private String userCreatedDLQRoutingKey;

    // --------- Converter & Template ----------
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

    // --------- Exchange ----------
    @Bean
    public DirectExchange restaurantExchange() {
        return new DirectExchange(exchange);
    }

    // --------- Queues ----------
    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable(userCreatedQueue)   // durable queue
                .withArgument("x-dead-letter-exchange", exchange) // DLX
                .withArgument("x-dead-letter-routing-key", userCreatedDLQRoutingKey) // DLQ routing key
                .withArgument("x-message-ttl", 30000) // TTL 30s
                .build();
    }

    @Bean
    public Queue userCreatedDLQ() {
        return new Queue(userCreatedDLQName, true);
    }

    // --------- Bindings ----------
    @Bean
    public Binding bindingUserCreatedQueue() {
        return BindingBuilder
                .bind(userCreatedQueue())
                .to(restaurantExchange())
                .with(userCreatedRoutingKey);
    }

    @Bean
    public Binding bindingUserCreatedDLQ() {
        return BindingBuilder
                .bind(userCreatedDLQ())
                .to(restaurantExchange())
                .with(userCreatedDLQRoutingKey);
    }
}