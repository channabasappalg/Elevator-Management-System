package com.example.elevator.service;

import com.example.elevator.model.ElevatorRequest;
import com.example.elevator.repository.ElevatorRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Simulates an AI/ML model that predicts high-traffic floors based on past request patterns.
 * This service analyzes historical request data to identify peak hours and floors.
 */
@Service
public class AIPredictiveService {

    @Autowired
    private ElevatorRequestRepository requestRepository;

    /**
     * Predicts the next likely high-traffic floor (hotspot) based on historical data.
     *
     * @return An Optional containing the predicted hotspot floor, or empty if no prediction.
     */
    public Optional<Integer> predictHotspotFloor() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek day = now.getDayOfWeek();
        int hour = now.getHour();

        // Fetch historical requests for the same day of the week and hour
        List<ElevatorRequest> historicalRequests = requestRepository.findAll().stream()
                .filter(req -> req.getRequestTime().getDayOfWeek() == day && req.getRequestTime().getHour() == hour)
                .collect(Collectors.toList());

        if (historicalRequests.size() < 10) {
            // Not enough data to make a confident prediction
            return Optional.empty();
        }

        // Find the most requested source floor in this historical data slice
        return historicalRequests.stream()
                .collect(Collectors.groupingBy(ElevatorRequest::getSourceFloor, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }
}