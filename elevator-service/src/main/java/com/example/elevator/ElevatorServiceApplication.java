package com.example.elevator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ElevatorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElevatorServiceApplication.class, args);
    }

}