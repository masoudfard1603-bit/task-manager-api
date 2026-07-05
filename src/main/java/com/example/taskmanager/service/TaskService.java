package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.model.TaskStatus;

import java.util.List;

public interface TaskService {

    TaskResponse create(TaskRequest request, String owner);

    TaskResponse getById(Long id, String requester, boolean isAdmin);

    List<TaskResponse> getAll(TaskStatus status, String requester, boolean isAdmin);

    TaskResponse update(Long id, TaskRequest request, String requester, boolean isAdmin);

    void delete(Long id, String requester, boolean isAdmin);
}
