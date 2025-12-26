package com.example.elevator.repository;

import com.example.elevator.model.Elevator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElevatorRepository extends JpaRepository<Elevator, Long> {
}