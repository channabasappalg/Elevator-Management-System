package com.example.elevator.service;

import com.example.elevator.model.*;
import com.example.elevator.repository.ElevatorLogRepository;
import com.example.elevator.repository.ElevatorRepository;
import com.example.elevator.repository.ElevatorRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ElevatorServiceTest {

    @Mock
    private ElevatorRepository elevatorRepository;

    @Mock
    private ElevatorRequestRepository requestRepository;

    @Mock
    private ElevatorLogRepository logRepository;

    @InjectMocks
    private ElevatorService elevatorService;

    private Elevator elevator;

    @BeforeEach
    void setUp() {
        elevator = new Elevator();
        elevator.setId(1L);
        elevator.setCurrentFloor(0);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevator.setDirection(Direction.STOPPED);
        elevator.setOperational(true);
    }

    @Test
    void testGetElevatorById_Found() {
        when(elevatorRepository.findById(1L)).thenReturn(Optional.of(elevator));
        Elevator found = elevatorService.getElevatorById(1L);
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void testGetElevatorById_NotFound() {
        when(elevatorRepository.findById(1L)).thenReturn(Optional.empty());
        Elevator found = elevatorService.getElevatorById(1L);
        assertNull(found);
    }

    @Test
    void testMoveElevator_Up() {
        when(elevatorRepository.findById(1L)).thenReturn(Optional.of(elevator));
        when(elevatorRepository.save(any(Elevator.class))).thenReturn(elevator);

        elevatorService.moveElevator(1L, 5);

        assertEquals(5, elevator.getCurrentFloor());
        assertEquals(ElevatorStatus.IDLE, elevator.getStatus()); // Resets to IDLE after move
        assertEquals(Direction.STOPPED, elevator.getDirection());
        verify(logRepository, times(1)).save(any(ElevatorLog.class));
    }

    @Test
    void testMoveElevator_Down() {
        elevator.setCurrentFloor(10);
        when(elevatorRepository.findById(1L)).thenReturn(Optional.of(elevator));
        when(elevatorRepository.save(any(Elevator.class))).thenReturn(elevator);

        elevatorService.moveElevator(1L, 2);

        assertEquals(2, elevator.getCurrentFloor());
        verify(logRepository, times(1)).save(any(ElevatorLog.class));
    }

    @Test
    void testReportFault() {
        when(elevatorRepository.findById(1L)).thenReturn(Optional.of(elevator));
        when(elevatorRepository.save(any(Elevator.class))).thenReturn(elevator);

        Elevator result = elevatorService.reportFault(1L);

        assertFalse(result.isOperational());
        assertEquals(ElevatorStatus.OUT_OF_SERVICE, result.getStatus());
        verify(logRepository, times(1)).save(any(ElevatorLog.class));
    }

    @Test
    void testRepairElevator() {
        elevator.setOperational(false);
        elevator.setStatus(ElevatorStatus.OUT_OF_SERVICE);
        when(elevatorRepository.findById(1L)).thenReturn(Optional.of(elevator));
        when(elevatorRepository.save(any(Elevator.class))).thenReturn(elevator);

        Elevator result = elevatorService.repairElevator(1L);

        assertTrue(result.isOperational());
        assertEquals(ElevatorStatus.IDLE, result.getStatus());
        verify(logRepository, times(1)).save(any(ElevatorLog.class));
    }

    @Test
    void testManualAssign_Success() {
        ElevatorRequest request = new ElevatorRequest();
        request.setId(100L);
        request.setSourceFloor(2);
        request.setDestinationFloor(8);

        when(requestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(elevatorRepository.findById(1L)).thenReturn(Optional.of(elevator));

        ElevatorRequest result = elevatorService.manualAssign(100L, 1L);

        assertNotNull(result);
        assertEquals(RequestStatus.ASSIGNED, result.getStatus());
        assertEquals(1L, result.getAssignedElevatorId());
        verify(logRepository, times(3)).save(any(ElevatorLog.class)); // manualAssign + 2 moves
    }

    @Test
    void testManualAssign_Failure_InvalidRequest() {
        when(requestRepository.findById(100L)).thenReturn(Optional.empty());
        ElevatorRequest result = elevatorService.manualAssign(100L, 1L);
        assertNull(result);
    }

    @Test
    void testGetLogs() {
        Pageable pageable = PageRequest.of(0, 10);
        List<ElevatorLog> logs = Collections.singletonList(new ElevatorLog(1L, "Test log"));
        Page<ElevatorLog> pagedLogs = new PageImpl<>(logs, pageable, logs.size());

        when(logRepository.findAll(pageable)).thenReturn(pagedLogs);

        Page<ElevatorLog> result = elevatorService.getLogs(pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test log", result.getContent().get(0).getMessage());
    }
    
    @Test
    void testOptimizeRoutes_NoPendingRequests() {
        when(requestRepository.findByStatus(RequestStatus.PENDING)).thenReturn(Collections.emptyList());
        String result = elevatorService.optimizeRoutes();
        assertEquals("No pending requests to optimize.", result);
    }
}