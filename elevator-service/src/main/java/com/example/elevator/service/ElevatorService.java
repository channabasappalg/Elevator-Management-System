package com.example.elevator.service;

import com.example.elevator.dto.ElevatorStatusDTO;
import com.example.elevator.exception.ResourceNotFoundException;
import com.example.elevator.model.Direction;
import com.example.elevator.model.Elevator;
import com.example.elevator.model.ElevatorLog;
import com.example.elevator.model.ElevatorRequest;
import com.example.elevator.model.ElevatorStatus;
import com.example.elevator.model.RequestStatus;
import com.example.elevator.repository.ElevatorLogRepository;
import com.example.elevator.repository.ElevatorRepository;
import com.example.elevator.repository.ElevatorRequestRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ElevatorService {

    @Autowired
    private ElevatorRepository elevatorRepository;

    @Autowired
    private ElevatorRequestRepository requestRepository;

    @Autowired
    private ElevatorLogRepository logRepository;

    @Autowired
    private WebSocketUpdateService webSocketUpdateService;

    public List<Elevator> getAllElevators() {
        return elevatorRepository.findAll();
    }

    @Cacheable(value = "elevatorStatus")
    public List<ElevatorStatusDTO> getAllElevatorsStatus() {
        return elevatorRepository.findAll().stream()
                .map(e -> new ElevatorStatusDTO(e.getId(), e.getCurrentFloor(), e.getStatus(), e.getDirection(), e.isOperational()))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "elevators", key = "#id")
    public Elevator getElevatorById(Long id) {
        return elevatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Elevator", "id", id));
    }

    @Transactional
    @CacheEvict(value = {"elevators", "elevatorStatus"}, allEntries = true)
    public Elevator saveElevator(Elevator elevator) {
        Elevator savedElevator = elevatorRepository.save(elevator);
        // After saving, broadcast the new status to all clients
        broadcastStatusUpdate();
        return savedElevator;
    }

    @Transactional
    @CacheEvict(value = {"elevators", "elevatorStatus"}, allEntries = true)
    public void deleteElevator(Long id) {
        Elevator elevator = elevatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Elevator", "id", id));
        elevatorRepository.delete(elevator);
        broadcastStatusUpdate();
    }

    public List<Elevator> getAvailableElevators() {
        return elevatorRepository.findAll().stream()
                .filter(Elevator::isOperational)
                .collect(Collectors.toList());
    }

    public List<Elevator> getIdleElevators() {
        return elevatorRepository.findAll().stream()
                .filter(e -> e.isOperational() && e.getStatus() == ElevatorStatus.IDLE)
                .collect(Collectors.toList());
    }

    @CircuitBreaker(name = "elevatorService", fallbackMethod = "assignRequestFallback")
    public void assignRequestToElevator(Elevator elevator, ElevatorRequest request) {
        logRepository.save(new ElevatorLog(elevator.getId(), "Assigned request ID: " + request.getId()));
        
        // Logic to move elevator to source then destination
        // This is a simplified synchronous simulation
        moveElevator(elevator.getId(), request.getSourceFloor());
        moveElevator(elevator.getId(), request.getDestinationFloor());
        
        request.setAssignedElevatorId(elevator.getId());
        request.setStatus(RequestStatus.ASSIGNED);
        requestRepository.save(request);
    }

    public void assignRequestFallback(Elevator elevator, ElevatorRequest request, Throwable t) {
        logRepository.save(new ElevatorLog(elevator.getId(), "Failed to assign request ID: " + request.getId() + ". Circuit Breaker Open. Error: " + t.getMessage()));
        // Optionally, mark elevator as faulty or retry later
        // For now, just log it. The request remains PENDING.
    }
    
    public Elevator moveElevator(Long id, int targetFloor) {
        Elevator elevator = getElevatorById(id);
        if (elevator != null && elevator.isOperational()) {
            logRepository.save(new ElevatorLog(id, "Moving to floor " + targetFloor));
            if (targetFloor > elevator.getCurrentFloor()) {
                elevator.setStatus(ElevatorStatus.MOVING_UP);
                elevator.setDirection(Direction.UP);
            } else if (targetFloor < elevator.getCurrentFloor()) {
                elevator.setStatus(ElevatorStatus.MOVING_DOWN);
                elevator.setDirection(Direction.DOWN);
            } else {
                elevator.setStatus(ElevatorStatus.IDLE);
                elevator.setDirection(Direction.STOPPED);
            }
            
            // Simulate movement
            elevator.setCurrentFloor(targetFloor);
            elevator.setStatus(ElevatorStatus.IDLE);
            elevator.setDirection(Direction.STOPPED);
            return saveElevator(elevator);
        }
        return null;
    }

    public Elevator reportFault(Long id) {
        Elevator elevator = getElevatorById(id);
        if (elevator != null) {
            logRepository.save(new ElevatorLog(id, "Reported fault. Status: OUT_OF_SERVICE"));
            elevator.setOperational(false);
            elevator.setStatus(ElevatorStatus.OUT_OF_SERVICE);
            elevator.setDirection(Direction.STOPPED);
            return saveElevator(elevator);
        }
        return null;
    }

    public Elevator repairElevator(Long id) {
        Elevator elevator = getElevatorById(id);
        if (elevator != null) {
            logRepository.save(new ElevatorLog(id, "Elevator repaired. Status: IDLE"));
            elevator.setOperational(true);
            elevator.setStatus(ElevatorStatus.IDLE);
            elevator.setDirection(Direction.STOPPED);
            return saveElevator(elevator);
        }
        return null;
    }

    public ElevatorRequest manualAssign(Long requestId, Long elevatorId) {
        ElevatorRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", requestId));
        Elevator elevator = getElevatorById(elevatorId);

        if (elevator.isOperational()) {
            logRepository.save(new ElevatorLog(elevatorId, "Manually assigned request ID: " + requestId));
            assignRequestToElevator(elevator, request);
            return request;
        }
        return null; // Or throw exception
    }

    public void simulateMovement(Long elevatorId, int targetFloor) {
        Elevator elevator = getElevatorById(elevatorId);
        if (elevator == null || !elevator.isOperational()) return;

        logRepository.save(new ElevatorLog(elevatorId, "Simulation started to floor " + targetFloor));

        int currentFloor = elevator.getCurrentFloor();
        if (targetFloor > currentFloor) {
            elevator.setStatus(ElevatorStatus.MOVING_UP);
            elevator.setDirection(Direction.UP);
        } else if (targetFloor < currentFloor) {
            elevator.setStatus(ElevatorStatus.MOVING_DOWN);
            elevator.setDirection(Direction.DOWN);
        } else {
            elevator.setStatus(ElevatorStatus.IDLE);
            elevator.setDirection(Direction.STOPPED);
        }
        saveElevator(elevator);

        // Simulate step-by-step movement
        new Thread(() -> {
            try {
                int floor = currentFloor;
                while (floor != targetFloor) {
                    Thread.sleep(1000); // Simulate time to move between floors
                    if (targetFloor > floor) {
                        floor++;
                    } else {
                        floor--;
                    }
                    elevator.setCurrentFloor(floor);
                    saveElevator(elevator);
                    logRepository.save(new ElevatorLog(elevatorId, "Reached floor " + floor));
                }
                elevator.setStatus(ElevatorStatus.IDLE);
                elevator.setDirection(Direction.STOPPED);
                saveElevator(elevator);
                logRepository.save(new ElevatorLog(elevatorId, "Simulation completed. Idle at floor " + targetFloor));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public Page<ElevatorLog> getLogs(Pageable pageable) {
        return logRepository.findAll(pageable);
    }

    public String optimizeRoutes() {
        List<ElevatorRequest> pendingRequests = requestRepository.findByStatus(RequestStatus.PENDING);
        if (pendingRequests.isEmpty()) {
            return "No pending requests to optimize.";
        }

        // Batch Optimization: Group requests by source floor
        Map<Integer, List<ElevatorRequest>> requestsByFloor = pendingRequests.stream()
                .collect(Collectors.groupingBy(ElevatorRequest::getSourceFloor));

        // Find the floor with the most requests (Hotspot)
        Integer busiestFloor = requestsByFloor.entrySet().stream()
                .max((e1, e2) -> Integer.compare(e1.getValue().size(), e2.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(0);

        List<ElevatorRequest> batchRequests = requestsByFloor.get(busiestFloor);
        int batchSize = batchRequests.size();

        // Move idle elevators closer to the busiest floor
        List<Elevator> idleElevators = elevatorRepository.findAll().stream()
                .filter(e -> e.getStatus() == ElevatorStatus.IDLE && e.isOperational())
                .collect(Collectors.toList());

        int movedCount = 0;
        for (Elevator elevator : idleElevators) {
            if (Math.abs(elevator.getCurrentFloor() - busiestFloor) > 2) { // Only move if far away
                moveElevator(elevator.getId(), busiestFloor);
                movedCount++;
                
                // Assign the batch of requests to this elevator (up to capacity)
                // In a real scenario, we would assign specific requests.
                // Here we just log the optimization intent.
                logRepository.save(new ElevatorLog(elevator.getId(), "Traffic Optimization: Moved to hotspot floor " + busiestFloor + " to serve " + batchSize + " pending requests."));
                
                // Break if we have enough elevators for the batch (assuming 1 elevator can take 10 people)
                if (movedCount * 10 >= batchSize) break;
            }
        }

        return "Optimization complete. Identified busiest floor: " + busiestFloor + " with " + batchSize + " requests. Repositioned " + movedCount + " idle elevators.";
    }

    private void broadcastStatusUpdate() {
        List<ElevatorStatusDTO> statuses = getAllElevatorsStatus();
        webSocketUpdateService.sendElevatorStatusUpdate(statuses);
    }
}