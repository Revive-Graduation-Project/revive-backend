package com.restaurant.auth.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Exchange names & routing keys (constants for easy reuse) ─────────────
    public static final String EXCHANGE_NAME = "restaurant.events.exchange";
    public static final String ROUTING_KEY_CREATED = "user.created";
    public static final String COMPENSATION_QUEUE = "auth.compensation.queue";
    public static final String COMPENSATION_KEY = "chef.profile.failed";

    public static final String CLIENT_COMPENSATION_QUEUE = "client.profile.failed.queue";
    public static final String CLIENT_COMPENSATION_KEY = "client.profile.failed";

    // ── Exchange ──────────────────────────────────────────────────────────────

    /**
     * Single durable Topic Exchange shared across all microservices.
     * Declared here so auth-service can assert it on startup.
     */
    @Bean
    public TopicExchange restaurantEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    // ── Queue ─────────────────────────────────────────────────────────────────

    /**
     * Only queue this service owns: receives saga compensation events
     * when the downstream chef-profile creation fails.
     */
    @Bean
    public Queue authCompensationQueue() {
        return new Queue(COMPENSATION_QUEUE, true); // durable=true
    }

    @Bean
    public Queue clientCompensationQueue() {
        return new Queue(CLIENT_COMPENSATION_QUEUE, true);
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    @Bean
    public Binding compensationBinding(Queue authCompensationQueue,
            TopicExchange restaurantEventsExchange) {
        return BindingBuilder.bind(authCompensationQueue)
                 .to(restaurantEventsExchange)
                 .with(COMPENSATION_KEY);
    }

    @Bean
    public Binding clientCompensationBinding(Queue clientCompensationQueue,
            TopicExchange restaurantEventsExchange) {
        return BindingBuilder.bind(clientCompensationQueue)
                 .to(restaurantEventsExchange)
                 .with(CLIENT_COMPENSATION_KEY);
    }


    // ── Message converter (JSON via Jackson) ──────────────────────────────────

    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper typeMapper = new org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        
        java.util.Map<String, Class<?>> idClassMapping = new java.util.HashMap<>();
        idClassMapping.put("com.restaurant.client.event.ProfileCreationFailedEvent", com.restaurant.auth.event.ProfileCreationFailedEvent.class);
        idClassMapping.put("com.restaurant.kitchen.event.ProfileCreationFailedEvent", com.restaurant.auth.event.ProfileCreationFailedEvent.class);
        typeMapper.setIdClassMapping(idClassMapping);
        
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
