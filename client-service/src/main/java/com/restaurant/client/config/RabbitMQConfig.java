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

    // --------- Converter ----------
    @Bean
    public MessageConverter converter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");

        java.util.Map<String, Class<?>> idClassMapping = new java.util.HashMap<>();
        idClassMapping.put("com.restaurant.auth.event.UserCreatedEvent", UserCreatedEvent.class);
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
}
