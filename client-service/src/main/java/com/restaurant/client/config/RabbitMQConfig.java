package com.restaurant.client.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.restaurant.client.event.PointRedemptionRequestedEvent;
import com.restaurant.client.event.PointsEarnedEvent;
import com.restaurant.client.event.UserCreatedEvent;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queues.client-user-created.name}")
    private String clientUserCreatedQueue;

    @Value("${app.rabbitmq.queues.client-user-created.routing-key}")
    private String clientUserCreatedRoutingKey;

    @Value("${app.rabbitmq.queues.client-user-created.dlq-name}")
    private String clientUserCreatedDLQName;

    @Value("${app.rabbitmq.queues.client-user-created.dlq-routing-key}")
    private String clientUserCreatedDLQRoutingKey;

    // points-redemption-requested
    @Value("${app.rabbitmq.queues.points-redemption-requested.name}")
    private String pointsRedemptionRequestedQueue;

    @Value("${app.rabbitmq.queues.points-redemption-requested.routing-key}")
    private String pointsRedemptionRequestedRoutingKey;

    @Value("${app.rabbitmq.queues.points-redemption-requested.dlq-name}")
    private String pointsRedemptionRequestedDLQName;

    @Value("${app.rabbitmq.queues.points-redemption-requested.dlq-routing-key}")
    private String pointsRedemptionRequestedDLQRoutingKey;

    // points-earned
    @Value("${app.rabbitmq.queues.points-earned.name}")
    private String pointsEarnedQueue;

    @Value("${app.rabbitmq.queues.points-earned.routing-key}")
    private String pointsEarnedRoutingKey;

    @Value("${app.rabbitmq.queues.points-earned.dlq-name}")
    private String pointsEarnedDLQName;

    @Value("${app.rabbitmq.queues.points-earned.dlq-routing-key}")
    private String pointsEarnedDLQRoutingKey;

    // --------- Converter ----------
    @Bean
    public MessageConverter converter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");

        java.util.Map<String, Class<?>> idClassMapping = new java.util.HashMap<>();
        idClassMapping.put("com.restaurant.auth.event.UserCreatedEvent", UserCreatedEvent.class);
        idClassMapping.put("com.restaurant.order.events.points.PointRedemptionRequestedEvent", PointRedemptionRequestedEvent.class);
        idClassMapping.put("com.restaurant.order.events.points.PointsEarnedEvent", PointsEarnedEvent.class);
        typeMapper.setIdClassMapping(idClassMapping);

        converter.setJavaTypeMapper(typeMapper);
        return converter;
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
    public Queue clientUserCreatedQueue() {
        return QueueBuilder.durable(clientUserCreatedQueue)
                .withArgument("x-dead-letter-exchange", exchange)
                .withArgument("x-dead-letter-routing-key", clientUserCreatedDLQRoutingKey)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue clientUserCreatedDLQ() {
        return new Queue(clientUserCreatedDLQName, true);
    }

    @Bean
    public Queue pointsRedemptionRequestedQueue() {
        return QueueBuilder.durable(pointsRedemptionRequestedQueue)
                .withArgument("x-dead-letter-exchange", exchange)
                .withArgument("x-dead-letter-routing-key", pointsRedemptionRequestedDLQRoutingKey)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue pointsRedemptionRequestedDLQ() {
        return new Queue(pointsRedemptionRequestedDLQName, true);
    }

    @Bean
    public Queue pointsEarnedQueue() {
        return QueueBuilder.durable(pointsEarnedQueue)
                .withArgument("x-dead-letter-exchange", exchange)
                .withArgument("x-dead-letter-routing-key", pointsEarnedDLQRoutingKey)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue pointsEarnedDLQ() {
        return new Queue(pointsEarnedDLQName, true);
    }

    // --------- Bindings ----------
    @Bean
    public Binding bindingClientUserCreatedQueue() {
        return BindingBuilder
                .bind(clientUserCreatedQueue())
                .to(restaurantExchange())
                .with(clientUserCreatedRoutingKey);
    }

    @Bean
    public Binding bindingClientUserCreatedDLQ() {
        return BindingBuilder
                .bind(clientUserCreatedDLQ())
                .to(restaurantExchange())
                .with(clientUserCreatedDLQRoutingKey);
    }

    @Bean
    public Binding bindingPointsRedemptionRequestedQueue() {
        return BindingBuilder
                .bind(pointsRedemptionRequestedQueue())
                .to(restaurantExchange())
                .with(pointsRedemptionRequestedRoutingKey);
    }

    @Bean
    public Binding bindingPointsRedemptionRequestedDLQ() {
        return BindingBuilder
                .bind(pointsRedemptionRequestedDLQ())
                .to(restaurantExchange())
                .with(pointsRedemptionRequestedDLQRoutingKey);
    }

    @Bean
    public Binding bindingPointsEarnedQueue() {
        return BindingBuilder
                .bind(pointsEarnedQueue())
                .to(restaurantExchange())
                .with(pointsEarnedRoutingKey);
    }

    @Bean
    public Binding bindingPointsEarnedDLQ() {
        return BindingBuilder
                .bind(pointsEarnedDLQ())
                .to(restaurantExchange())
                .with(pointsEarnedDLQRoutingKey);
    }
}
