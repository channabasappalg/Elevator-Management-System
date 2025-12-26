package com.example.elevator.service;

import com.example.elevator.dto.ElevatorMovementDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    @Autowired
    private ElevatorService elevatorService;

    @KafkaListener(topics = "elevator-movement", groupId = "elevator-group")
    public void consumeMovementEvent(ElevatorMovementDTO movementDTO) {
        System.out.println("Received movement event for elevator: " + movementDTO.getElevatorId() + " to floor: " + movementDTO.getTargetFloor());
        elevatorService.simulateMovement(movementDTO.getElevatorId(), movementDTO.getTargetFloor());
    }
}