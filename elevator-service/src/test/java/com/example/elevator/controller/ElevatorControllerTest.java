package com.example.elevator.controller;

import com.example.elevator.model.Elevator;
import com.example.elevator.model.ElevatorRequest;
import com.example.elevator.model.ElevatorStatus;
import com.example.elevator.model.RequestStatus;
import com.example.elevator.repository.ElevatorRequestRepository;
import com.example.elevator.service.ElevatorService;
import com.example.elevator.service.HealthMonitorService;
import com.example.elevator.service.KafkaProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ElevatorController.class)
public class ElevatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ElevatorService elevatorService;

    @MockBean
    private ElevatorRequestRepository requestRepository;

    @MockBean
    private HealthMonitorService healthMonitorService;

    @MockBean
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetAllElevators_AsAdmin() throws Exception {
        Elevator elevator = new Elevator();
        elevator.setId(1L);
        elevator.setStatus(ElevatorStatus.IDLE);

        given(elevatorService.getAllElevators()).willReturn(Arrays.asList(elevator));

        mockMvc.perform(get("/api/elevators")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(username = "passenger", roles = {"PASSENGER"})
    public void testRequestElevator_AsPassenger() throws Exception {
        ElevatorRequest request = new ElevatorRequest();
        request.setSourceFloor(1);
        request.setDestinationFloor(5);

        given(requestRepository.save(any(ElevatorRequest.class))).willReturn(request);

        mockMvc.perform(post("/api/elevators/request")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "passenger", roles = {"PASSENGER"})
    public void testAdminEndpoint_ForbiddenForPassenger() throws Exception {
        mockMvc.perform(put("/api/elevators/1/repair").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testRepairElevator_AsAdmin() throws Exception {
        Elevator elevator = new Elevator();
        elevator.setId(1L);
        elevator.setOperational(true);

        given(elevatorService.repairElevator(1L)).willReturn(elevator);

        mockMvc.perform(put("/api/elevators/1/repair")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operational").value(true));
    }
    
    @Test
    public void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/elevators/status"))
                .andExpect(status().isUnauthorized());
    }
}