package com.example.elevator.service;

import com.example.elevator.model.Elevator;
import com.example.elevator.model.ElevatorRequest;
import com.example.elevator.model.ElevatorStatus;
import com.example.elevator.model.RequestStatus;
import com.example.elevator.repository.ElevatorRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SchedulerService {

    @Autowired
    private ElevatorService elevatorService;

    @Autowired
    private ElevatorRequestRepository requestRepository;

    @Autowired
    private AIPredictiveService aiPredictiveService;

    @Scheduled(fixedRate = 5000) // Run every 5 seconds
    public void scheduleElevators() {
        // AI-based proactive dispatching
        proactiveDispatch();
        
        // Reactive dispatching for current requests
        List<ElevatorRequest> pendingRequests = requestRepository.findByStatus(RequestStatus.PENDING);
        List<Elevator> availableElevators = elevatorService.getAvailableElevators();

        for (ElevatorRequest request : pendingRequests) {
            Elevator bestElevator = findBestElevator(request, availableElevators);
            if (bestElevator != null) {
                elevatorService.assignRequestToElevator(bestElevator, request);
                request.setStatus(RequestStatus.ASSIGNED);
                requestRepository.save(request);
            }
        }
    }
    
    private void proactiveDispatch() {
        aiPredictiveService.predictHotspotFloor().ifPresent(hotspotFloor -> {
            // AI predicts a hotspot. Move idle elevators there.
            List<Elevator> idleElevators = elevatorService.getIdleElevators();
            for (Elevator elevator : idleElevators) {
                // Only move if not already at or near the hotspot AND not in Eco Mode
                if (!elevator.isEcoMode() && Math.abs(elevator.getCurrentFloor() - hotspotFloor) > 1) {
                    elevatorService.moveElevator(elevator.getId(), hotspotFloor);
                }
            }
        });
    }

    private Elevator findBestElevator(ElevatorRequest request, List<Elevator> elevators) {
        if (elevators.isEmpty()) {
            return null;
        }

        // PriorityQueue (Min-Heap) to store elevators based on a calculated "suitability cost"
        // Lower cost is better.
        PriorityQueue<Elevator> minHeap = new PriorityQueue<>(Comparator.comparingInt(e -> 
            calculateCost(e, request)
        ));

        // Add only operational elevators to the heap
        for (Elevator elevator : elevators) {
            if (elevator.isOperational()) {
                // Check if elevator is at full capacity OR in Eco Mode
                if (elevator.getCurrentLoad() >= elevator.getCapacity() || elevator.isEcoMode()) {
                    continue; // Skip full or parked elevators
                }
                minHeap.offer(elevator);
            }
        }

        // The top of the heap is the best suited elevator based on cost (Min-Heap)
        return minHeap.poll();
    }

    private int calculateCost(Elevator elevator, ElevatorRequest request) {
        int currentFloor = elevator.getCurrentFloor();
        int requestFloor = request.getSourceFloor();
        int targetFloor = request.getDestinationFloor();
        
        // BFS-based distance calculation (Linear graph: floors 0, 1, 2...)
        int distance = bfsDistance(currentFloor, requestFloor);
        int cost = distance;

        // Load Balancing Factor: Add penalty based on current load
        if (elevator.getCapacity() > 0) {
            int loadPenalty = (int) (((double) elevator.getCurrentLoad() / elevator.getCapacity()) * 10);
            cost += loadPenalty;
        }

        boolean requestGoingUp = targetFloor > requestFloor;
        boolean requestGoingDown = targetFloor < requestFloor;

        if (elevator.getStatus() == ElevatorStatus.MOVING_UP) {
            if (requestFloor < currentFloor) {
                // Elevator has passed the request floor. High penalty.
                cost += 20;
            } else {
                // Elevator is below or at request floor (moving towards it)
                if (requestGoingDown) {
                    // Passenger wants to go DOWN, but elevator is going UP.
                    cost += 10;
                }
            }
        } else if (elevator.getStatus() == ElevatorStatus.MOVING_DOWN) {
            if (requestFloor > currentFloor) {
                // Elevator has passed the request floor (is below). High penalty.
                cost += 20;
            } else {
                // Elevator is above or at request floor (moving towards it)
                if (requestGoingUp) {
                    // Passenger wants to go UP, but elevator is going DOWN.
                    cost += 10;
                }
            }
        }
        // IDLE elevators have no penalty, just distance.

        return cost;
    }

    private int bfsDistance(int startFloor, int endFloor) {
        if (startFloor == endFloor) return 0;
        return Math.abs(startFloor - endFloor); // Simplified for linear building
    }
}