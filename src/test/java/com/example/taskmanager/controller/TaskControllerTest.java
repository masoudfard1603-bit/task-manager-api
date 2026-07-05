package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Security filters are disabled here; @WithMockUser injects the Authentication
// directly into the SecurityContext so we can test controller logic in isolation.
@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void create_withValidRequest_shouldReturn201() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Learn Spring Boot");
        request.setDescription("Build a REST API");
        request.setStatus(TaskStatus.TODO);

        TaskResponse response = TaskResponse.builder()
                .id(1L)
                .title("Learn Spring Boot")
                .description("Build a REST API")
                .status(TaskStatus.TODO)
                .owner("alice")
                .build();

        when(taskService.create(any(TaskRequest.class), eq("alice"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Learn Spring Boot"))
                .andExpect(jsonPath("$.owner").value("alice"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void create_withBlankTitle_shouldReturn400() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("");

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getById_whenExists_shouldReturn200() throws Exception {
        TaskResponse response = TaskResponse.builder()
                .id(5L)
                .title("Existing task")
                .status(TaskStatus.IN_PROGRESS)
                .owner("alice")
                .build();

        when(taskService.getById(eq(5L), eq("alice"), eq(false))).thenReturn(response);

        mockMvc.perform(get("/api/v1/tasks/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Existing task"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getAll_withStatusFilter_shouldReturnFilteredList() throws Exception {
        when(taskService.getAll(eq(TaskStatus.DONE), eq("alice"), eq(false)))
                .thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/tasks").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAll_asAdmin_shouldPassIsAdminTrue() throws Exception {
        when(taskService.getAll(eq(null), eq("admin"), eq(true))).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/tasks/1"))
                .andExpect(status().isNoContent());
    }
}
