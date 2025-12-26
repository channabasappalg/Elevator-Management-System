package com.example.elevator.service;

import com.example.elevator.dto.ElevatorStatusDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebSocketUpdateService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcasts the status of all elevators to the /topic/elevator-status WebSocket topic.
     * @param statuses The list of elevator statuses to broadcast.
     */
    public void sendElevatorStatusUpdate(List<ElevatorStatusDTO> statuses) {
        messagingTemplate.convertAndSend("/topic/elevator-status", statuses);
    }
}