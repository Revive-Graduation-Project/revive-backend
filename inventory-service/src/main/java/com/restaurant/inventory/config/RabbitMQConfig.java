package com.restaurant.inventory.config;

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

    @Value("${app.rabbitmq.queues.menu-nutrition.name}")
    private String menuNutritionQueue;

    @Value("${app.rabbitmq.queues.menu-nutrition.routing-key}")
    private String menuNutritionRoutingKey;

    @Value("${app.rabbitmq.queues.menu-nutrition.dlq-name}")
    private String menuNutritionDLQName;

    @Value("${app.rabbitmq.queues.menu-nutrition.dlq-routing-key}")
    private String menuNutritionDLQRoutingKey;

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
    public Queue menuNutritionQueue() {
        return QueueBuilder.durable(menuNutritionQueue)
                .withArgument("x-dead-letter-exchange", exchange)
                .withArgument("x-dead-letter-routing-key", menuNutritionDLQRoutingKey)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue menuNutritionDLQ() {
        return new Queue(menuNutritionDLQName, true);
    }

    // --------- Bindings ----------
    @Bean
    public Binding bindingMenuNutritionQueue() {
        return BindingBuilder
                .bind(menuNutritionQueue())
                .to(restaurantExchange())
                .with(menuNutritionRoutingKey);
    }

    @Bean
    public Binding bindingMenuNutritionDLQ() {
        return BindingBuilder
                .bind(menuNutritionDLQ())
                .to(restaurantExchange())
                .with(menuNutritionDLQRoutingKey);
    }
}
