package com.example.elevator.dto;

import com.example.elevator.model.Direction;
import com.example.elevator.model.ElevatorStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElevatorStatusDTO {
    private Long id;
    private int currentFloor;
    private ElevatorStatus status;
    private Direction direction;
    private boolean isOperational;
}