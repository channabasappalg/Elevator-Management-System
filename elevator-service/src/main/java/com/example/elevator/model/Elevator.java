package com.example.elevator.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
public class Elevator implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int currentFloor;
    
    @Enumerated(EnumType.STRING)
    private ElevatorStatus status; 

    @Enumerated(EnumType.STRING)
    private Direction direction;
    
    private int capacity;
    private int currentLoad;
    private boolean isOperational; // For fault tolerance
    private LocalDateTime lastMaintenanceDate;
    private LocalDateTime lastHeartbeat; // For health monitoring
    private boolean ecoMode = false; // For energy optimization

    public Elevator() {
        this.currentFloor = 0;
        this.status = ElevatorStatus.IDLE;
        this.direction = Direction.STOPPED;
        this.capacity = 10; // Default capacity
        this.currentLoad = 0;
        this.isOperational = true;
        this.lastHeartbeat = LocalDateTime.now();
    }
}