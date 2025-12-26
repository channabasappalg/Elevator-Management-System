package com.example.elevator.service;

import com.example.elevator.model.Direction;
import com.example.elevator.model.Elevator;
import com.example.elevator.model.ElevatorLog;
import com.example.elevator.model.ElevatorStatus;
import com.example.elevator.repository.ElevatorLogRepository;
import com.example.elevator.repository.ElevatorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class HealthMonitorService {

    @Autowired
    private ElevatorRepository elevatorRepository;

    @Autowired
    private ElevatorLogRepository logRepository;

    // Threshold in seconds to consider an elevator "down" if no heartbeat is received
    private static final long HEARTBEAT_THRESHOLD_SECONDS = 60;
    
    // Threshold to attempt a restart (e.g., 2 minutes after going down)
    private static final long RESTART_THRESHOLD_SECONDS = 120;

    @Scheduled(fixedRate = 10000) // Run every 10 seconds
    public void checkElevatorHealth() {
        List<Elevator> elevators = elevatorRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Elevator elevator : elevators) {
            if (elevator.isOperational()) {
                // Check if heartbeat is stale
                if (elevator.getLastHeartbeat() != null && 
                    ChronoUnit.SECONDS.between(elevator.getLastHeartbeat(), now) > HEARTBEAT_THRESHOLD_SECONDS) {
                    
                    // Mark as non-operational (Watchdog detects failure)
                    elevator.setOperational(false);
                    elevator.setStatus(ElevatorStatus.OUT_OF_SERVICE);
                    elevatorRepository.save(elevator);
                    
                    logRepository.save(new ElevatorLog(elevator.getId(), 
                        "Watchdog: Elevator marked OUT_OF_SERVICE due to missing heartbeat. Last heartbeat: " + elevator.getLastHeartbeat()));
                }
            } else {
                // Watchdog Restart Logic: Attempt to restart non-responding elevators
                if (elevator.getLastHeartbeat() != null && 
                    ChronoUnit.SECONDS.between(elevator.getLastHeartbeat(), now) > RESTART_THRESHOLD_SECONDS) {
                    
                    attemptRestart(elevator);
                }
            }
        }
    }
    
    private void attemptRestart(Elevator elevator) {
        logRepository.save(new ElevatorLog(elevator.getId(), "Watchdog: Attempting to restart elevator..."));
        
        // Simulate restart logic (e.g., sending a reset command to hardware)
        // For simulation, we assume the restart is successful and reset the state.
        elevator.setOperational(true);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevator.setDirection(Direction.STOPPED);
        elevator.setLastHeartbeat(LocalDateTime.now()); // Reset heartbeat
        
        elevatorRepository.save(elevator);
        logRepository.save(new ElevatorLog(elevator.getId(), "Watchdog: Elevator successfully restarted and is now IDLE."));
    }
    
    // Method to be called by elevator hardware/simulation to send a heartbeat
    public void receiveHeartbeat(Long elevatorId) {
        elevatorRepository.findById(elevatorId).ifPresent(elevator -> {
            elevator.setLastHeartbeat(LocalDateTime.now());
            if (!elevator.isOperational()) {
                // Auto-recover if it comes back online naturally
                elevator.setOperational(true);
                elevator.setStatus(ElevatorStatus.IDLE);
                logRepository.save(new ElevatorLog(elevator.getId(), "Health Monitor: Elevator recovered. Back online."));
            }
            elevatorRepository.save(elevator);
        });
    }
}