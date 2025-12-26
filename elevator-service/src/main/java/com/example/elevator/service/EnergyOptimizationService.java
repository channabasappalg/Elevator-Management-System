package com.example.elevator.service;

import com.example.elevator.model.Elevator;
import com.example.elevator.model.ElevatorLog;
import com.example.elevator.model.ElevatorStatus;
import com.example.elevator.model.RequestStatus;
import com.example.elevator.repository.ElevatorLogRepository;
import com.example.elevator.repository.ElevatorRepository;
import com.example.elevator.repository.ElevatorRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnergyOptimizationService {

    @Autowired
    private ElevatorRepository elevatorRepository;

    @Autowired
    private ElevatorRequestRepository requestRepository;

    @Autowired
    private ElevatorLogRepository logRepository;

    // Threshold for low traffic (e.g., fewer than 5 pending requests)
    private static final int LOW_TRAFFIC_THRESHOLD = 5;

    @Scheduled(fixedRate = 60000) // Run every minute
    public void optimizeEnergy() {
        long pendingRequestsCount = requestRepository.findByStatus(RequestStatus.PENDING).size();
        List<Elevator> elevators = elevatorRepository.findAll();
        
        long operationalElevators = elevators.stream().filter(Elevator::isOperational).count();
        
        if (pendingRequestsCount < LOW_TRAFFIC_THRESHOLD && operationalElevators > 1) {
            // Low traffic: Enable Eco Mode for some elevators
            int elevatorsToPark = (int) (operationalElevators / 2); // Park half of them
            int parkedCount = 0;

            for (Elevator elevator : elevators) {
                if (elevator.isOperational() && elevator.getStatus() == ElevatorStatus.IDLE && !elevator.isEcoMode()) {
                    if (parkedCount < elevatorsToPark) {
                        elevator.setEcoMode(true);
                        elevatorRepository.save(elevator);
                        logRepository.save(new ElevatorLog(elevator.getId(), "Energy Optimization: Enabled Eco Mode (Parked)."));
                        parkedCount++;
                    }
                }
            }
        } else {
            // High traffic: Disable Eco Mode for all elevators
            for (Elevator elevator : elevators) {
                if (elevator.isEcoMode()) {
                    elevator.setEcoMode(false);
                    elevatorRepository.save(elevator);
                    logRepository.save(new ElevatorLog(elevator.getId(), "Energy Optimization: Disabled Eco Mode (Active)."));
                }
            }
        }
    }
}