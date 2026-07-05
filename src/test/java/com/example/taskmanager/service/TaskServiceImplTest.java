package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task task;
    private TaskRequest request;

    private static final String OWNER = "alice";
    private static final String OTHER_USER = "bob";

    @BeforeEach
    void setUp() {
        task = Task.builder()
                .id(1L)
                .title("Write unit tests")
                .description("Cover the service layer")
                .status(TaskStatus.TODO)
                .owner(OWNER)
                .build();

        request = new TaskRequest();
        request.setTitle("Write unit tests");
        request.setDescription("Cover the service layer");
        request.setStatus(TaskStatus.TODO);
    }

    @Test
    void create_shouldSaveTaskWithOwnerAndReturnIt() {
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(sampleResponse());

        TaskResponse result = taskService.create(request, OWNER);

        assertThat(result.getTitle()).isEqualTo("Write unit tests");
        verify(taskRepository).save(task);
    }

    @Test
    void getById_whenOwnerRequests_shouldReturnTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task)).thenReturn(sampleResponse());

        TaskResponse result = taskService.getById(1L, OWNER, false);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getById_whenOtherUserRequests_shouldThrowAccessDenied() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.getById(1L, OTHER_USER, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_whenAdminRequests_shouldReturnAnyTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task)).thenReturn(sampleResponse());

        TaskResponse result = taskService.getById(1L, OTHER_USER, true);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getById_whenNotFound_shouldThrow() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(99L, OWNER, false))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAll_forRegularUser_shouldReturnOnlyOwnTasks() {
        when(taskRepository.findByOwner(OWNER)).thenReturn(List.of(task));
        when(taskMapper.toResponse(any(Task.class))).thenReturn(sampleResponse());

        List<TaskResponse> result = taskService.getAll(null, OWNER, false);

        assertThat(result).hasSize(1);
        verify(taskRepository).findByOwner(OWNER);
        verify(taskRepository, never()).findAll();
    }

    @Test
    void getAll_forAdmin_shouldReturnAllTasks() {
        when(taskRepository.findAll()).thenReturn(List.of(task));
        when(taskMapper.toResponse(any(Task.class))).thenReturn(sampleResponse());

        List<TaskResponse> result = taskService.getAll(null, OTHER_USER, true);

        assertThat(result).hasSize(1);
        verify(taskRepository).findAll();
        verify(taskRepository, never()).findByOwner(any());
    }

    @Test
    void getAll_withStatusFilter_forOwner_shouldUseOwnerAndStatus() {
        when(taskRepository.findByOwnerAndStatus(OWNER, TaskStatus.DONE)).thenReturn(List.of());

        List<TaskResponse> result = taskService.getAll(TaskStatus.DONE, OWNER, false);

        assertThat(result).isEmpty();
        verify(taskRepository).findByOwnerAndStatus(OWNER, TaskStatus.DONE);
    }

    @Test
    void delete_whenOwner_shouldRemoveTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.delete(1L, OWNER, false);

        verify(taskRepository).delete(task);
    }

    @Test
    void delete_whenNotOwnerAndNotAdmin_shouldThrowAccessDenied() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.delete(1L, OTHER_USER, false))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).delete(any());
    }

    @Test
    void delete_whenNotFound_shouldThrow() {
        when(taskRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(42L, OWNER, false))
                .isInstanceOf(TaskNotFoundException.class);
    }

    private TaskResponse sampleResponse() {
        return TaskResponse.builder()
                .id(1L)
                .title("Write unit tests")
                .description("Cover the service layer")
                .status(TaskStatus.TODO)
                .owner(OWNER)
                .build();
    }
}
