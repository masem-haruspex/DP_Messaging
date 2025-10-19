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

    @Bean public FanoutExchange userExchange() { return new FanoutExchange("user.exchange"); }
    @Bean public Exchange roomsExchange() { return ExchangeBuilder.topicExchange(roomsExchange).durable(true).build(); }

    @Bean public Queue messagingUserQueue() { return QueueBuilder.durable("messaging.user.queue").build(); }
    @Bean public Queue roomCreatedQueue() { return QueueBuilder.durable("messaging.room.created.queue").build(); }
    @Bean public Queue userJoinedQueue() { return QueueBuilder.durable("messaging.user.joined.queue").build(); }
    @Bean public Queue userLeftQueue() { return QueueBuilder.durable("messaging.user.left.queue").build(); }
    @Bean public Queue roomDeletedQueue() { return QueueBuilder.durable("messaging.room.deleted.queue").build(); }
    @Bean public Queue userKickedQueue() { return QueueBuilder.durable("messaging.user.kicked.queue").build(); }
    @Bean public Queue userMutedQueue() { return QueueBuilder.durable("messaging.user.muted.queue").build(); }
    @Bean public Queue messageSentQueue() { return QueueBuilder.durable("messaging.message.sent.queue").build(); }

    @Bean public Binding bindMessagingUserQueue(Queue messagingUserQueue, FanoutExchange userExchange) { return BindingBuilder.bind(messagingUserQueue).to(userExchange); }
    @Bean public Binding bindRoomCreated() { return BindingBuilder.bind(roomCreatedQueue()).to(roomsExchange()).with("room.created").noargs(); }
    @Bean public Binding bindUserJoined() { return BindingBuilder.bind(userJoinedQueue()).to(roomsExchange()).with("user.joined").noargs(); }
    @Bean public Binding bindUserLeft() { return BindingBuilder.bind(userLeftQueue()).to(roomsExchange()).with("user.left").noargs(); }
    @Bean public Binding bindRoomDeleted() { return BindingBuilder.bind(roomDeletedQueue()).to(roomsExchange()).with("room.deleted").noargs(); }
    @Bean public Binding bindUserKicked() { return BindingBuilder.bind(userKickedQueue()).to(roomsExchange()).with("user.kicked").noargs(); }
    @Bean public Binding bindUserMuted() { return BindingBuilder.bind(userMutedQueue()).to(roomsExchange()).with("user.muted").noargs(); }
    @Bean public Binding bindMessageSent() { return BindingBuilder.bind(messageSentQueue()).to(roomsExchange()).with("message.sent").noargs(); }

    @Bean public MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}