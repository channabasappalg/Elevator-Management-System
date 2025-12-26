package com.example.elevator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElevatorMovementDTO {
    private Long elevatorId;
    private int targetFloor;
}