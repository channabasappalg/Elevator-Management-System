package com.example.elevator.service;

import com.example.elevator.model.Elevator;
import com.example.elevator.model.ElevatorLog;
import com.example.elevator.model.ElevatorStatus;
import com.example.elevator.repository.ElevatorLogRepository;
import com.example.elevator.repository.ElevatorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HealthMonitorServiceTest {

    @Mock
    private ElevatorRepository elevatorRepository;

    @Mock
    private ElevatorLogRepository logRepository;

    @InjectMocks
    private HealthMonitorService healthMonitorService;

    @Test
    public void testCheckElevatorHealth_MarksDown() {
        Elevator elevator = new Elevator();
        elevator.setId(1L);
        elevator.setOperational(true);
        // Last heartbeat was 70 seconds ago (threshold is 60)
        elevator.setLastHeartbeat(LocalDateTime.now().minusSeconds(70));

        when(elevatorRepository.findAll()).thenReturn(Collections.singletonList(elevator));

        healthMonitorService.checkElevatorHealth();

        assertFalse(elevator.isOperational());
        verify(elevatorRepository).save(elevator);
        verify(logRepository).save(any(ElevatorLog.class));
    }

    @Test
    public void testCheckElevatorHealth_RestartsWatchdog() {
        Elevator elevator = new Elevator();
        elevator.setId(1L);
        elevator.setOperational(false);
        // Down for 130 seconds (restart threshold is 120)
        elevator.setLastHeartbeat(LocalDateTime.now().minusSeconds(130));

        when(elevatorRepository.findAll()).thenReturn(Collections.singletonList(elevator));

        healthMonitorService.checkElevatorHealth();

        assertTrue(elevator.isOperational());
        verify(elevatorRepository).save(elevator);
        // Should log restart attempt and success
        verify(logRepository, times(2)).save(any(ElevatorLog.class));
    }

    @Test
    public void testReceiveHeartbeat_RecoversElevator() {
        Elevator elevator = new Elevator();
        elevator.setId(1L);
        elevator.setOperational(false);

        when(elevatorRepository.findById(1L)).thenReturn(Optional.of(elevator));

        healthMonitorService.receiveHeartbeat(1L);

        assertTrue(elevator.isOperational());
        verify(elevatorRepository).save(elevator);
        verify(logRepository).save(any(ElevatorLog.class));
    }
}