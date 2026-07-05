package com.example.taskmanager.service.impl;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Override
    public TaskResponse create(TaskRequest request, String owner) {
        Task task = taskMapper.toEntity(request);
        task.setOwner(owner);
        Task saved = taskRepository.save(task);
        return taskMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getById(Long id, String requester, boolean isAdmin) {
        Task task = findTaskOrThrow(id);
        checkAccess(task, requester, isAdmin);
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getAll(TaskStatus status, String requester, boolean isAdmin) {
        List<Task> tasks;

        if (isAdmin) {
            tasks = (status == null)
                    ? taskRepository.findAll()
                    : taskRepository.findByStatus(status);
        } else {
            tasks = (status == null)
                    ? taskRepository.findByOwner(requester)
                    : taskRepository.findByOwnerAndStatus(requester, status);
        }

        return tasks.stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @Override
    public TaskResponse update(Long id, TaskRequest request, String requester, boolean isAdmin) {
        Task task = findTaskOrThrow(id);
        checkAccess(task, requester, isAdmin);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        Task updated = taskRepository.save(task);
        return taskMapper.toResponse(updated);
    }

    @Override
    public void delete(Long id, String requester, boolean isAdmin) {
        Task task = findTaskOrThrow(id);
        checkAccess(task, requester, isAdmin);
        taskRepository.delete(task);
    }

    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    private void checkAccess(Task task, String requester, boolean isAdmin) {
        if (!isAdmin && !task.getOwner().equals(requester)) {
            throw new AccessDeniedException("You do not have permission to access this task");
        }
    }
}
