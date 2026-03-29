package com.restaurant.order.config;

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

    // ticket.created — response from kitchen-service
    @Value("${app.rabbitmq.queues.ticket-created.name}")
    private String ticketCreatedQueue;

    @Value("${app.rabbitmq.queues.ticket-created.routing-key}")
    private String ticketCreatedRoutingKey;

    // ticket.failed — response from kitchen-service
    @Value("${app.rabbitmq.queues.ticket-failed.name}")
    private String ticketFailedQueue;

    @Value("${app.rabbitmq.queues.ticket-failed.routing-key}")
    private String ticketFailedRoutingKey;

    // ticket.ready — kitchen marks order ready
    @Value("${app.rabbitmq.queues.ticket-ready.name}")
    private String ticketReadyQueue;

    @Value("${app.rabbitmq.queues.ticket-ready.routing-key}")
    private String ticketReadyRoutingKey;

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
    public Queue ticketCreatedQueue() {
        return QueueBuilder.durable(ticketCreatedQueue).build();
    }

    @Bean
    public Queue ticketFailedQueue() {
        return QueueBuilder.durable(ticketFailedQueue).build();
    }

    @Bean
    public Queue ticketReadyQueue() {
        return QueueBuilder.durable(ticketReadyQueue).build();
    }

    // --------- Bindings ----------
    @Bean
    public Binding bindingTicketCreatedQueue() {
        return BindingBuilder
                .bind(ticketCreatedQueue())
                .to(restaurantExchange())
                .with(ticketCreatedRoutingKey);
    }

    @Bean
    public Binding bindingTicketFailedQueue() {
        return BindingBuilder
                .bind(ticketFailedQueue())
                .to(restaurantExchange())
                .with(ticketFailedRoutingKey);
    }

    @Bean
    public Binding bindingTicketReadyQueue() {
        return BindingBuilder
                .bind(ticketReadyQueue())
                .to(restaurantExchange())
                .with(ticketReadyRoutingKey);
    }
}
