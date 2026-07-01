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

    // payment.succeeded — payment marks transaction success
    @Value("${app.rabbitmq.queues.payment.succeeded.name}")
    private String paymentSucceededQueue;

    @Value("${app.rabbitmq.queues.payment-succeeded.routing-key}")
    private String paymentSucceededRoutingKey;

    // payment.failed — payment mark order transaction failure
    @Value("${app.rabbitmq.queues.payment-failed.name}")
    private String paymentFailedQueue;

    @Value("${app.rabbitmq.queues.payment-failed.routing-key}")
    private String paymentFailedRoutingKey;

    // point-redemption.succeeded — response from client-service
    @Value("${app.rabbitmq.queues.point-redemption-succeeded.name}")
    private String pointRedemptionSucceededQueue;

    @Value("${app.rabbitmq.queues.point-redemption-succeeded.routing-key}")
    private String pointRedemptionSucceededRoutingKey;

    // point-redemption.failed — response from client-service
    @Value("${app.rabbitmq.queues.point-redemption-failed.name}")
    private String pointRedemptionFailedQueue;

    @Value("${app.rabbitmq.queues.point-redemption-failed.routing-key}")
    private String pointRedemptionFailedRoutingKey;

    // ticket.started — kitchen marks order as preparing
    @Value("${app.rabbitmq.queues.ticket-started.name}")
    private String ticketStartedQueue;

    @Value("${app.rabbitmq.queues.ticket-started.routing-key}")
    private String ticketStartedRoutingKey;

    // ticket.canceled — kitchen confirms ticket cancellation succeeded
    @Value("${app.rabbitmq.queues.ticket-canceled.name}")
    private String ticketCanceledQueue;

    @Value("${app.rabbitmq.queues.ticket-canceled.routing-key}")
    private String ticketCanceledRoutingKey;

    // ticket.cancellation.failed — kitchen refuses ticket cancellation
    @Value("${app.rabbitmq.queues.ticket-cancellation-failed.name}")
    private String ticketCancellationFailedQueue;

    @Value("${app.rabbitmq.queues.ticket-cancellation-failed.routing-key}")
    private String ticketCancellationFailedRoutingKey;

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

    @Bean
    public Queue paymentSucceededQueue() {return QueueBuilder.durable(paymentSucceededQueue).build();}

    @Bean
    public Queue paymentFailedQueue() {return QueueBuilder.durable(paymentFailedQueue).build();}

    @Bean
    public Queue pointRedemptionSucceededQueue() {
        return QueueBuilder.durable(pointRedemptionSucceededQueue).build();
    }

    @Bean
    public Queue pointRedemptionFailedQueue() {
        return QueueBuilder.durable(pointRedemptionFailedQueue).build();
    }

    @Bean
    public Queue ticketStartedQueue() {
        return QueueBuilder.durable(ticketStartedQueue).build();
    }

    @Bean
    public Queue ticketCanceledQueue() {
        return QueueBuilder.durable(ticketCanceledQueue).build();
    }

    @Bean
    public Queue ticketCancellationFailedQueue() {
        return QueueBuilder.durable(ticketCancellationFailedQueue).build();
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

    @Bean
    public Binding bindingPaymentSucceededQueue() {
        return BindingBuilder
                .bind(paymentSucceededQueue())
                .to(restaurantExchange())
                .with(paymentSucceededRoutingKey);
    }

    @Bean
    public Binding bindingPaymentFailedQueue() {
        return BindingBuilder
                .bind(paymentFailedQueue())
                .to(restaurantExchange())
                .with(paymentFailedRoutingKey);
    }

    @Bean
    public Binding bindingPointRedemptionSucceededQueue() {
        return BindingBuilder
                .bind(pointRedemptionSucceededQueue())
                .to(restaurantExchange())
                .with(pointRedemptionSucceededRoutingKey);
    }

    @Bean
    public Binding bindingPointRedemptionFailedQueue() {
        return BindingBuilder
                .bind(pointRedemptionFailedQueue())
                .to(restaurantExchange())
                .with(pointRedemptionFailedRoutingKey);
    }

    @Bean
    public Binding bindingTicketStartedQueue() {
        return BindingBuilder
                .bind(ticketStartedQueue())
                .to(restaurantExchange())
                .with(ticketStartedRoutingKey);
    }

    @Bean
    public Binding bindingTicketCanceledQueue() {
        return BindingBuilder
                .bind(ticketCanceledQueue())
                .to(restaurantExchange())
                .with(ticketCanceledRoutingKey);
    }

    @Bean
    public Binding bindingTicketCancellationFailedQueue() {
        return BindingBuilder
                .bind(ticketCancellationFailedQueue())
                .to(restaurantExchange())
                .with(ticketCancellationFailedRoutingKey);
    }
}
