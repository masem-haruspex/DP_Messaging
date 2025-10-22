package com.mm_mk.Messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // --- Injected Exchange Names ---
    @Value("${rabbitmq.exchange.user}")
    private String userExchangeName;
    @Value("${rabbitmq.exchange.rooms}")
    private String roomsExchangeName;

    // --- Injected Queue Names ---
    @Value("${rabbitmq.queue.user.created}")
    private String userCreatedQueueName;
    @Value("${rabbitmq.queue.user.updated}")
    private String userUpdatedQueueName;
    @Value("${rabbitmq.queue.room.created}")
    private String roomCreatedQueueName;
    @Value("${rabbitmq.queue.room.deleted}")
    private String roomDeletedQueueName;
    @Value("${rabbitmq.queue.user.joined}")
    private String userJoinedQueueName;
    @Value("${rabbitmq.queue.user.left}")
    private String userLeftQueueName;
    @Value("${rabbitmq.queue.user.kicked}")
    private String userKickedQueueName;
    @Value("${rabbitmq.queue.user.muted}")
    private String userMutedQueueName;
    @Value("${rabbitmq.queue.message.sent}")
    private String messageSentQueueName;

    // --- Injected Routing Keys ---
    @Value("${rabbitmq.routingkey.user.created}")
    private String userCreatedRoutingKey;
    @Value("${rabbitmq.routingkey.user.updated}")
    private String userUpdatedRoutingKey;
    @Value("${rabbitmq.routingkey.room.created}")
    private String roomCreatedRoutingKey;
    @Value("${rabbitmq.routingkey.room.deleted}")
    private String roomDeletedRoutingKey;
    @Value("${rabbitmq.routingkey.user.joined}")
    private String userJoinedRoutingKey;
    @Value("${rabbitmq.routingkey.user.left}")
    private String userLeftRoutingKey;
    @Value("${rabbitmq.routingkey.user.kicked}")
    private String userKickedRoutingKey;
    @Value("${rabbitmq.routingkey.user.muted}")
    private String userMutedRoutingKey;
    @Value("${rabbitmq.routingkey.message.sent}")
    private String messageSentRoutingKey;

    // =================================================================
    // General Configuration
    // =================================================================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // =================================================================
    // Exchange Definitions
    // =================================================================

    @Bean
    public TopicExchange userExchange() {
        return ExchangeBuilder.topicExchange(userExchangeName).durable(true).build();
    }

    @Bean
    public TopicExchange roomsExchange() {
        return ExchangeBuilder.topicExchange(roomsExchangeName).durable(true).build();
    }

    // =================================================================
    // Queue Definitions
    // =================================================================

    // --- Queues for User Service Events ---
    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable(userCreatedQueueName).build();
    }

    @Bean
    public Queue userUpdatedQueue() {
        return QueueBuilder.durable(userUpdatedQueueName).build();
    }

    // --- Queues for Rooms Service Events ---
    @Bean
    public Queue roomCreatedQueue() {
        return QueueBuilder.durable(roomCreatedQueueName).build();
    }

    @Bean
    public Queue userJoinedQueue() {
        return QueueBuilder.durable(userJoinedQueueName).build();
    }

    @Bean
    public Queue userLeftQueue() {
        return QueueBuilder.durable(userLeftQueueName).build();
    }

    @Bean
    public Queue roomDeletedQueue() {
        return QueueBuilder.durable(roomDeletedQueueName).build();
    }

    @Bean
    public Queue userKickedQueue() {
        return QueueBuilder.durable(userKickedQueueName).build();
    }

    @Bean
    public Queue userMutedQueue() {
        return QueueBuilder.durable(userMutedQueueName).build();
    }

    @Bean
    public Queue messageSentQueue() {
        return QueueBuilder.durable(messageSentQueueName).build();
    }

    // =================================================================
    // Binding Definitions
    // =================================================================

    // --- Bindings to User Exchange ---
    @Bean
    public Binding bindingUserCreated(Queue userCreatedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userCreatedQueue)
                .to(userExchange)
                .with(userCreatedRoutingKey);
    }

    @Bean
    public Binding bindingUserUpdated(Queue userUpdatedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userUpdatedQueue)
                .to(userExchange)
                .with(userUpdatedRoutingKey);
    }

    // --- Bindings to Rooms Exchange ---
    @Bean
    public Binding bindRoomCreated(Queue roomCreatedQueue, TopicExchange roomsExchange) {
        return BindingBuilder.bind(roomCreatedQueue)
                .to(roomsExchange)
                .with(roomCreatedRoutingKey);
    }

    @Bean
    public Binding bindUserJoined(Queue userJoinedQueue, TopicExchange roomsExchange) {
        return BindingBuilder.bind(userJoinedQueue)
                .to(roomsExchange)
                .with(userJoinedRoutingKey);
    }

    @Bean
    public Binding bindUserLeft(Queue userLeftQueue, TopicExchange roomsExchange) {
        return BindingBuilder.bind(userLeftQueue)
                .to(roomsExchange)
                .with(userLeftRoutingKey);
    }

    @Bean
    public Binding bindRoomDeleted(Queue roomDeletedQueue, TopicExchange roomsExchange) {
        return BindingBuilder.bind(roomDeletedQueue)
                .to(roomsExchange)
                .with(roomDeletedRoutingKey);
    }

    @Bean
    public Binding bindUserKicked(Queue userKickedQueue, TopicExchange roomsExchange) {
        return BindingBuilder.bind(userKickedQueue)
                .to(roomsExchange)
                .with(userKickedRoutingKey);
    }

    @Bean
    public Binding bindUserMuted(Queue userMutedQueue, TopicExchange roomsExchange) {
        return BindingBuilder.bind(userMutedQueue)
                .to(roomsExchange)
                .with(userMutedRoutingKey);
    }

    @Bean
    public Binding bindMessageSent(Queue messageSentQueue, TopicExchange roomsExchange) {
        return BindingBuilder.bind(messageSentQueue)
                .to(roomsExchange)
                .with(messageSentRoutingKey);
    }
}