package com.example.elevator.repository;

import com.example.elevator.model.ElevatorRequest;
import com.example.elevator.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ElevatorRequestRepository extends JpaRepository<ElevatorRequest, Long> {
    List<ElevatorRequest> findByStatus(RequestStatus status);
}