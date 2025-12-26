package com.example.elevator.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class ElevatorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long elevatorId;
    private String message;
    private LocalDateTime timestamp;

    public ElevatorLog(Long elevatorId, String message) {
        this.elevatorId = elevatorId;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}