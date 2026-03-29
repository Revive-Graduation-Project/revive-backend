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

    // user.created
    @Value("${app.rabbitmq.queues.user-created.name}")
    private String userCreatedQueue;

    @Value("${app.rabbitmq.queues.user-created.routing-key}")
    private String userCreatedRoutingKey;

    @Value("${app.rabbitmq.queues.user-created.dlq-name}")
    private String userCreatedDLQName;

    @Value("${app.rabbitmq.queues.user-created.dlq-routing-key}")
    private String userCreatedDLQRoutingKey;

    // order.created
    @Value("${app.rabbitmq.queues.order-created.name}")
    private String orderCreatedQueue;

    @Value("${app.rabbitmq.queues.order-created.routing-key}")
    private String orderCreatedRoutingKey;

    @Value("${app.rabbitmq.queues.order-created.dlq-name}")
    private String orderCreatedDLQName;

    @Value("${app.rabbitmq.queues.order-created.dlq-routing-key}")
    private String orderCreatedDLQRoutingKey;

    // --------- Converter ----------
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    // --------- RabbitTemplate ----------
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

    // --------- Exchange ----------
    @Bean
    public TopicExchange restaurantExchange() {
        return new TopicExchange(exchange, true, false);
    }

    // --------- Queues ----------
    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable(userCreatedQueue)
                .withArgument("x-dead-letter-exchange", exchange)
                .withArgument("x-dead-letter-routing-key", userCreatedDLQRoutingKey)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue userCreatedDLQ() {
        return new Queue(userCreatedDLQName, true);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(orderCreatedQueue)
                .withArgument("x-dead-letter-exchange", exchange)
                .withArgument("x-dead-letter-routing-key", orderCreatedDLQRoutingKey)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue orderCreatedDLQ() {
        return new Queue(orderCreatedDLQName, true);
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

    @Bean
    public Binding bindingOrderCreatedQueue() {
        return BindingBuilder
                .bind(orderCreatedQueue())
                .to(restaurantExchange())
                .with(orderCreatedRoutingKey);
    }

    @Bean
    public Binding bindingOrderCreatedDLQ() {
        return BindingBuilder
                .bind(orderCreatedDLQ())
                .to(restaurantExchange())
                .with(orderCreatedDLQRoutingKey);
    }
}