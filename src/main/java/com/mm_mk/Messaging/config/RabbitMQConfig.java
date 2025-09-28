// src/main/java/com/mm_mk/Messaging/config/RabbitMQConfig.java
package com.mm_mk.Messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.rooms}")
    private String roomsExchange;

    // Declare ALL queues
    @Bean
    public Queue roomCreatedQueue() {
        return QueueBuilder.durable("messaging.room.created.queue").build();
    }

    @Bean
    public Queue userJoinedQueue() {
        return QueueBuilder.durable("messaging.user.joined.queue").build();
    }

    @Bean
    public Queue userLeftQueue() {
        return QueueBuilder.durable("messaging.user.left.queue").build();
    }

    @Bean
    public Queue roomDeletedQueue() {
        return QueueBuilder.durable("messaging.room.deleted.queue").build();
    }

    // Declare exchange
    @Bean
    public Exchange roomsExchange() {
        return ExchangeBuilder.topicExchange(roomsExchange).durable(true).build();
    }

    // Bind queues to exchange
    @Bean
    public Binding bindRoomCreated() {
        return BindingBuilder.bind(roomCreatedQueue())
                .to(roomsExchange())
                .with("room.created")
                .noargs();
    }

    @Bean
    public Binding bindUserJoined() {
        return BindingBuilder.bind(userJoinedQueue())
                .to(roomsExchange())
                .with("user.joined")
                .noargs();
    }

    @Bean
    public Binding bindUserLeft() {
        return BindingBuilder.bind(userLeftQueue())
                .to(roomsExchange())
                .with("user.left")
                .noargs();
    }

    @Bean
    public Binding bindRoomDeleted() {
        return BindingBuilder.bind(roomDeletedQueue())
                .to(roomsExchange())
                .with("room.deleted")
                .noargs();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}