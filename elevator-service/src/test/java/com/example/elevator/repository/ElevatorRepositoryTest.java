package com.example.elevator.repository;

import com.example.elevator.model.Elevator;
import com.example.elevator.model.ElevatorStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class ElevatorRepositoryTest {

    @Autowired
    private ElevatorRepository elevatorRepository;

    @Test
    public void testSaveAndFindElevator() {
        Elevator elevator = new Elevator();
        elevator.setCurrentFloor(0);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevator.setOperational(true);

        Elevator savedElevator = elevatorRepository.save(elevator);

        assertNotNull(savedElevator.getId());

        Optional<Elevator> foundElevator = elevatorRepository.findById(savedElevator.getId());
        assertTrue(foundElevator.isPresent());
        assertEquals(0, foundElevator.get().getCurrentFloor());
    }
}