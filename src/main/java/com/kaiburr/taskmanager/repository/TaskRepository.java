package com.kaiburr.taskmanager.repository;

import com.kaiburr.taskmanager.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    // Find tasks whose name contains the given string
    List<Task> findByNameContainingIgnoreCase(String name);
}
