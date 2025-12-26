package com.example.elevator.controller;

import com.example.elevator.dto.ElevatorStatusDTO;
import com.example.elevator.model.Elevator;
import com.example.elevator.model.ElevatorLog;
import com.example.elevator.model.ElevatorRequest;
import com.example.elevator.repository.ElevatorRequestRepository;
import com.example.elevator.service.ElevatorService;
import com.example.elevator.service.HealthMonitorService;
import com.example.elevator.service.KafkaProducerService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/elevators")
@RateLimiter(name = "default")
public class ElevatorController {

    @Autowired
    private ElevatorService elevatorService;

    @Autowired
    private ElevatorRequestRepository requestRepository;

    @Autowired
    private HealthMonitorService healthMonitorService;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @GetMapping
    public List<Elevator> getAllElevators() {
        return elevatorService.getAllElevators();
    }

    @GetMapping("/status")
    public List<ElevatorStatusDTO> getAllElevatorsStatus() {
        return elevatorService.getAllElevatorsStatus();
    }

    @GetMapping("/{id}")
    public Elevator getElevatorById(@PathVariable Long id) {
        return elevatorService.getElevatorById(id);
    }

    @PostMapping
    public Elevator createElevator(@RequestBody Elevator elevator) {
        return elevatorService.saveElevator(elevator);
    }

    @DeleteMapping("/{id}")
    public void deleteElevator(@PathVariable Long id) {
        elevatorService.deleteElevator(id);
    }

    @PostMapping("/request")
    public ElevatorRequest requestElevator(@RequestBody ElevatorRequest request) {
        return requestRepository.save(request);
    }

    @PostMapping("/{id}/fault")
    public Elevator reportFault(@PathVariable Long id) {
        return elevatorService.reportFault(id);
    }

    @PutMapping("/{id}/repair")
    public ResponseEntity<Elevator> repairElevator(@PathVariable Long id) {
        Elevator elevator = elevatorService.repairElevator(id);
        if (elevator != null) {
            return ResponseEntity.ok(elevator);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/assign")
    public ResponseEntity<ElevatorRequest> manualAssign(@RequestParam Long requestId, @RequestParam Long elevatorId) {
        ElevatorRequest request = elevatorService.manualAssign(requestId, elevatorId);
        if (request != null) {
            return ResponseEntity.ok(request);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/simulate")
    public ResponseEntity<String> simulateMovement(@PathVariable Long id, @RequestParam int targetFloor) {
        // Use Kafka for asynchronous processing
        kafkaProducerService.sendMovementEvent(id, targetFloor);
        return ResponseEntity.ok("Simulation request queued for elevator " + id + " to floor " + targetFloor);
    }

    @GetMapping("/logs")
    public Page<ElevatorLog> getLogs(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(defaultValue = "id") String sortBy,
                                     @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return elevatorService.getLogs(pageable);
    }

    @GetMapping("/requests/history")
    public Page<ElevatorRequest> getRequestHistory(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(defaultValue = "requestTime") String sortBy,
                                                   @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return requestRepository.findAll(pageable);
    }

    @GetMapping("/optimise")
    public ResponseEntity<String> optimizeRoutes() {
        String result = elevatorService.optimizeRoutes();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<Void> receiveHeartbeat(@PathVariable Long id) {
        healthMonitorService.receiveHeartbeat(id);
        return ResponseEntity.ok().build();
    }
}