package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Endpoints for managing tasks (requires authentication)")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Create a new task, owned by the current user")
    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody TaskRequest request,
            Authentication authentication) {
        TaskResponse created = taskService.create(request, authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + created.getId())).body(created);
    }

    @Operation(summary = "Get a task by id (owner or admin only)")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(taskService.getById(id, authentication.getName(), isAdmin(authentication)));
    }

    @Operation(summary = "List tasks: own tasks for regular users, all tasks for admins")
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAll(
            @RequestParam(required = false) TaskStatus status,
            Authentication authentication) {
        return ResponseEntity.ok(taskService.getAll(status, authentication.getName(), isAdmin(authentication)));
    }

    @Operation(summary = "Update an existing task (owner or admin only)")
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                taskService.update(id, request, authentication.getName(), isAdmin(authentication)));
    }

    @Operation(summary = "Delete a task (owner or admin only)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        taskService.delete(id, authentication.getName(), isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
