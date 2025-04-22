package com.example.GateStatus.global.config.rabbitMQ;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitEventPublisher implements Event {


    private final RabbitTemplate rabbitTemplate;


}
