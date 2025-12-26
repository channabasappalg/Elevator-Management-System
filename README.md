# Elevator Management System

This is a Spring Boot based Elevator Management System designed as a microservice. It includes features for managing elevator state, moving elevators, and integrates with Redis for caching and Spring Security (JWT) for authentication.

## Features

*   **Microservice Architecture**: Built using Spring Boot.
*   **Data Persistence**: Uses H2 in-memory database (Dev) and PostgreSQL (Prod).
*   **Caching**: Integrates with Redis for caching elevator states to improve performance.
*   **Security**: JWT Authentication and Role-Based Access Control (RBAC).
*   **REST API**: Exposes endpoints to manage elevators.
*   **Real-time Scheduling**: Uses a scheduled task to assign pending requests to the best available elevator using a Min-Heap algorithm.
*   **Fault Tolerance**: Elevators can be marked as out of service, and the scheduler will ignore them.
*   **Health Monitoring**: Watchdog process monitors elevator heartbeats and marks them as OUT_OF_SERVICE if they stop responding.
*   **Auto Recovery**: Elevators can be repaired and brought back into service.
*   **Traffic Optimization**: Analyzes pending requests to identify hotspots and reposition idle elevators.
*   **Energy Optimization**: Puts idle elevators into "Eco Mode" during low traffic periods.
*   **AI-Based Prediction**: Simulates ML to predict peak hours and proactive dispatching.
*   **Asynchronous Processing**: Uses Kafka for elevator movement simulation.
*   **Real-Time UI**: Uses WebSockets to broadcast elevator status updates to a web client.
*   **Logging**: Tracks elevator movements and assignments in a database log.
*   **Pagination & Sorting**: Supported for logs and historical requests.
*   **API Documentation**: Integrated with Swagger UI (OpenAPI).
*   **Testing**: Includes unit and integration tests.
*   **Dockerized**: Includes Dockerfile and docker-compose.yml for easy deployment.
*   **CI/CD**: Includes GitHub Actions workflow for automated build and testing.

## Prerequisites

*   Java 17 or later
*   Maven
*   Docker & Docker Compose

## Project Structure

*   `elevator-service`: The main microservice module containing the logic.

## Getting Started (Local Development)

1.  **Start Redis & Kafka**: Ensure your Redis and Kafka servers are running locally.
2.  **Build the project**:
    ```bash
    mvn clean install
    ```
3.  **Run the application**:
    ```bash
    cd elevator-service
    mvn spring-boot:run
    ```
4.  **Access the Real-Time UI**:
    Open `http://localhost:8080/index.html` in your browser.

## Getting Started (Docker Deployment)

1.  **Build the project**:
    ```bash
    mvn clean install
    ```
2.  **Run with Docker Compose**:
    ```bash
    docker-compose up --build
    ```
    This will start:
    *   Elevator Service (on port 8080)
    *   PostgreSQL (on port 5432)
    *   Redis (on port 6379)
    *   Kafka & Zookeeper

## CI/CD Pipeline

The project uses **GitHub Actions** for Continuous Integration and Continuous Deployment.
The workflow is defined in `.github/workflows/maven-publish.yml`.

*   **Triggers**: Pushes and Pull Requests to the `main` branch.
*   **Environment**: Runs on `ubuntu-latest`.
*   **Services**: Automatically spins up Redis, Zookeeper, and Kafka containers to support integration tests.
*   **Steps**:
    1.  Checkout code.
    2.  Set up JDK 17.
    3.  Build with Maven (runs unit and integration tests).
    4.  Build Docker Image.
    5.  Push Docker Image to GitHub Container Registry (on push to main).

## API Documentation

The API documentation is available via Swagger UI:
*   URL: `http://localhost:8080/swagger-ui.html`

## Authentication

The API uses JWT for authentication.
1.  **Login** to get a token:
    *   `POST /api/auth/login`
    *   Body: `{"username": "admin", "password": "admin"}` (Role: ADMIN)
    *   Body: `{"username": "passenger", "password": "passenger"}` (Role: PASSENGER)
2.  Use the returned token in the `Authorization` header for subsequent requests:
    *   `Authorization: Bearer <token>`

## API Endpoints & Access Control

### Public
*   `POST /api/auth/login`: Login to get JWT token.

### Passenger (and Admin)
*   `POST /api/elevators/request`: Request an elevator.
    *   Body: `{"sourceFloor": 1, "destinationFloor": 5}`
*   `GET /api/elevators/status`: Get status of all elevators (simplified view).
*   `GET /api/elevators/{id}`: Get elevator by ID.

### Admin Only
*   `GET /api/elevators`: Get all elevators (full details).
*   `POST /api/elevators`: Create a new elevator.
*   `PUT /api/elevators/assign?requestId={requestId}&elevatorId={elevatorId}`: Manually assign an elevator to a request.
*   `POST /api/elevators/{id}/fault`: Report a fault for an elevator.
*   `PUT /api/elevators/{id}/repair`: Repair an elevator and bring it back to service.
*   `POST /api/elevators/{id}/simulate?targetFloor={floor}`: Simulate elevator movement step-by-step (Async via Kafka).
*   `POST /api/elevators/{id}/heartbeat`: Send a heartbeat signal for an elevator.
*   `GET /api/elevators/logs?page=0&size=10&sortBy=id&sortDir=desc`: Get paginated and sorted elevator logs.
*   `GET /api/elevators/requests/history?page=0&size=10&sortBy=requestTime&sortDir=desc`: Get paginated and sorted request history.
*   `GET /api/elevators/optimise`: Trigger traffic optimization logic.
*   `DELETE /api/elevators/{id}`: Delete an elevator.

## Configuration

*   **Database**: Configured in `application.properties`. Currently uses H2.
*   **Redis**: Configured in `application.properties`.
*   **Kafka**: Configured in `application.properties`.
*   **Security**: JWT secret and expiration configured in `application.properties`.

## Testing

Run tests using Maven:
```bash
mvn test
```
