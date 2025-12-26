package com.example.elevator.repository;

import com.example.elevator.model.ElevatorLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElevatorLogRepository extends JpaRepository<ElevatorLog, Long> {
}