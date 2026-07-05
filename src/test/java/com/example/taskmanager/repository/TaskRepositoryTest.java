package com.example.taskmanager.repository;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void findByOwner_shouldReturnOnlyThatUsersTasks() {
        taskRepository.save(Task.builder().title("Alice task 1").status(TaskStatus.TODO).owner("alice").build());
        taskRepository.save(Task.builder().title("Alice task 2").status(TaskStatus.DONE).owner("alice").build());
        taskRepository.save(Task.builder().title("Bob task 1").status(TaskStatus.TODO).owner("bob").build());

        List<Task> aliceTasks = taskRepository.findByOwner("alice");

        assertThat(aliceTasks).hasSize(2);
        assertThat(aliceTasks).allMatch(task -> task.getOwner().equals("alice"));
    }

    @Test
    void findByOwnerAndStatus_shouldFilterByBothFields() {
        taskRepository.save(Task.builder().title("Alice todo").status(TaskStatus.TODO).owner("alice").build());
        taskRepository.save(Task.builder().title("Alice done").status(TaskStatus.DONE).owner("alice").build());
        taskRepository.save(Task.builder().title("Bob todo").status(TaskStatus.TODO).owner("bob").build());

        List<Task> result = taskRepository.findByOwnerAndStatus("alice", TaskStatus.TODO);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Alice todo");
    }

    @Test
    void save_shouldPopulateTimestampsAndDefaultStatus() {
        Task task = Task.builder().title("No status set").owner("alice").build();

        Task saved = taskRepository.save(task);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
