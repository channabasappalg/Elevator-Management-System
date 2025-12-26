package com.example.elevator.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class ElevatorRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int sourceFloor;
    private int destinationFloor;
    private LocalDateTime requestTime;
    
    @Enumerated(EnumType.STRING)
    private RequestStatus status; 
    
    private Long assignedElevatorId; // Track which elevator is assigned

    public ElevatorRequest() {
        this.requestTime = LocalDateTime.now();
        this.status = RequestStatus.PENDING;
    }
}