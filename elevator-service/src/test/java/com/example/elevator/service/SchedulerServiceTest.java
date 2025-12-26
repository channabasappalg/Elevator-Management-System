package com.example.elevator.service;

import com.example.elevator.model.Elevator;
import com.example.elevator.model.ElevatorRequest;
import com.example.elevator.model.ElevatorStatus;
import com.example.elevator.model.RequestStatus;
import com.example.elevator.repository.ElevatorRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SchedulerServiceTest {

    @Mock
    private ElevatorService elevatorService;

    @Mock
    private ElevatorRequestRepository requestRepository;

    @InjectMocks
    private SchedulerService schedulerService;

    @Test
    public void testScheduleElevators_NoRequests() {
        when(requestRepository.findByStatus(RequestStatus.PENDING)).thenReturn(Collections.emptyList());
        when(elevatorService.getAvailableElevators()).thenReturn(Collections.emptyList());

        schedulerService.scheduleElevators();

        verify(elevatorService, never()).assignRequestToElevator(any(), any());
    }

    @Test
    public void testScheduleElevators_AssignsBestElevator() {
        ElevatorRequest request = new ElevatorRequest();
        request.setSourceFloor(5);
        request.setDestinationFloor(10);
        request.setStatus(RequestStatus.PENDING);

        // Elevator 1: Floor 0, IDLE (Distance 5)
        Elevator e1 = new Elevator();
        e1.setId(1L);
        e1.setCurrentFloor(0);
        e1.setStatus(ElevatorStatus.IDLE);
        e1.setOperational(true);

        // Elevator 2: Floor 4, MOVING_UP (Distance 1, Perfect Match)
        Elevator e2 = new Elevator();
        e2.setId(2L);
        e2.setCurrentFloor(4);
        e2.setStatus(ElevatorStatus.MOVING_UP);
        e2.setOperational(true);

        when(requestRepository.findByStatus(RequestStatus.PENDING)).thenReturn(Collections.singletonList(request));
        when(elevatorService.getAvailableElevators()).thenReturn(Arrays.asList(e1, e2));

        schedulerService.scheduleElevators();

        // Should pick e2 because it's closer and moving in the right direction
        verify(elevatorService).assignRequestToElevator(e2, request);
        verify(requestRepository).save(request);
    }

    @Test
    public void testScheduleElevators_AvoidsFullElevator() {
        ElevatorRequest request = new ElevatorRequest();
        request.setSourceFloor(5);

        Elevator e1 = new Elevator();
        e1.setId(1L);
        e1.setCurrentFloor(5); // Right there
        e1.setCapacity(10);
        e1.setCurrentLoad(10); // But full
        e1.setOperational(true);

        Elevator e2 = new Elevator();
        e2.setId(2L);
        e2.setCurrentFloor(0); // Far away
        e2.setCapacity(10);
        e2.setCurrentLoad(0); // But empty
        e2.setOperational(true);

        when(requestRepository.findByStatus(RequestStatus.PENDING)).thenReturn(Collections.singletonList(request));
        when(elevatorService.getAvailableElevators()).thenReturn(Arrays.asList(e1, e2));

        schedulerService.scheduleElevators();

        // Should pick e2 because e1 is full
        verify(elevatorService).assignRequestToElevator(e2, request);
    }
}