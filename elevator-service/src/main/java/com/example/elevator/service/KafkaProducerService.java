package com.example.elevator.service;

import com.example.elevator.dto.ElevatorMovementDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final String TOPIC = "elevator-movement";

    @Autowired
    private KafkaTemplate<String, ElevatorMovementDTO> kafkaTemplate;

    public void sendMovementEvent(Long elevatorId, int targetFloor) {
        ElevatorMovementDTO movementDTO = new ElevatorMovementDTO(elevatorId, targetFloor);
        kafkaTemplate.send(TOPIC, movementDTO);
    }
}