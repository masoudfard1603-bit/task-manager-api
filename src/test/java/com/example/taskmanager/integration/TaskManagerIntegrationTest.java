package com.example.taskmanager.integration;

import com.example.taskmanager.dto.AuthResponse;
import com.example.taskmanager.dto.RegisterRequest;
import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test running against a real PostgreSQL instance in a Docker
 * container, exercising the full stack: registration, JWT login, and
 * authenticated task creation.
 *
 * Requires a Docker daemon to be available (this is what CI provides).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskManagerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fullFlow_registerLoginAndCreateOwnTask() {
        RegisterRequest register = new RegisterRequest("alice", "alice@example.com", "password123");

        ResponseEntity<AuthResponse> registerResponse =
                restTemplate.postForEntity("/api/v1/auth/register", register, AuthResponse.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = registerResponse.getBody().getToken();
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        TaskRequest taskRequest = new TaskRequest("Integration test task", "Created via Testcontainers", TaskStatus.TODO);
        HttpEntity<TaskRequest> entity = new HttpEntity<>(taskRequest, headers);

        ResponseEntity<TaskResponse> taskResponse =
                restTemplate.postForEntity("/api/v1/tasks", entity, TaskResponse.class);

        assertThat(taskResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(taskResponse.getBody().getOwner()).isEqualTo("alice");
        assertThat(taskResponse.getBody().getTitle()).isEqualTo("Integration test task");
    }

    @Test
    void unauthenticatedRequest_shouldBeRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/tasks", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anotherUsersTask_shouldNotBeAccessible() {
        // Register two users
        restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest("carol", "carol@example.com", "password123"), AuthResponse.class);
        ResponseEntity<AuthResponse> daveRegister = restTemplate.postForEntity("/api/v1/auth/register",
                new RegisterRequest("dave", "dave@example.com", "password123"), AuthResponse.class);

        String carolToken = restTemplate.postForEntity("/api/v1/auth/login",
                        new com.example.taskmanager.dto.LoginRequest("carol", "password123"), AuthResponse.class)
                .getBody().getToken();
        String daveToken = daveRegister.getBody().getToken();

        HttpHeaders carolHeaders = new HttpHeaders();
        carolHeaders.setBearerAuth(carolToken);
        TaskRequest taskRequest = new TaskRequest("Carol's private task", null, TaskStatus.TODO);
        ResponseEntity<TaskResponse> created = restTemplate.postForEntity(
                "/api/v1/tasks", new HttpEntity<>(taskRequest, carolHeaders), TaskResponse.class);
        Long taskId = created.getBody().getId();

        HttpHeaders daveHeaders = new HttpHeaders();
        daveHeaders.setBearerAuth(daveToken);
        ResponseEntity<String> daveAttempt = restTemplate.exchange(
                "/api/v1/tasks/" + taskId,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(daveHeaders),
                String.class);

        assertThat(daveAttempt.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
