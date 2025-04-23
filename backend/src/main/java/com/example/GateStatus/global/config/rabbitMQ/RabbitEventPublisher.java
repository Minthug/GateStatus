package com.example.GateStatus.global.config.rabbitMQ;

import com.example.GateStatus.global.config.EventListner.DomainEvent;
import com.example.GateStatus.global.config.EventListner.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(DomainEvent event) {
        rabbitTemplate.convertAndSend("domain.events", event.getClass().getSimpleName(), event);
    }
}
