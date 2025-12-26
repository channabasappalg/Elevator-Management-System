package com.example.elevator;

import com.example.elevator.model.Elevator;
import com.example.elevator.model.ElevatorStatus;
import com.example.elevator.service.ElevatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.when;

@SpringBootTest
class ElevatorServiceApplicationTests {

    @Autowired
    private ElevatorService elevatorService;

    @MockBean
    private ElevatorService mockElevatorService;

    @Test
    void contextLoads() {
    }

    @Test
    void testMoveElevator() {
        Elevator elevator = new Elevator();
        elevator.setId(1L);
        elevator.setCurrentFloor(0);
        elevator.setStatus(ElevatorStatus.IDLE);

        when(mockElevatorService.moveElevator(1L, 5)).thenReturn(elevator);
        
        // This is a basic test structure. In a real scenario, you would test the service logic directly
        // or use MockMvc for controller tests.
    }
}