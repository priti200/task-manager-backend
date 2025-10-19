package com.kaiburr.taskmanager.controller;

import com.kaiburr.taskmanager.model.Task;
import com.kaiburr.taskmanager.model.TaskExecution;
import com.kaiburr.taskmanager.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    // GET /tasks or /tasks?id=123
    @GetMapping
    public ResponseEntity<?> getTasks(@RequestParam(required = false) String id) {
        if (id != null) {
            Optional<Task> task = taskRepository.findById(id);
            return task.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } else {
            return ResponseEntity.ok(taskRepository.findAll());
        }
    }

    // PUT /tasks - Create or update a task
    @PutMapping
    public ResponseEntity<?> saveTask(@RequestBody Task task) {
        // Simple validation to avoid dangerous commands
        if (task.getCommand().contains("rm") || task.getCommand().contains(";") || task.getCommand().contains("&&")) {
            return ResponseEntity.badRequest().body("Unsafe command detected!");
        }
        Task savedTask = taskRepository.save(task);
        return ResponseEntity.ok(savedTask);
    }

    // DELETE /tasks/{id} - Delete task by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable String id) {
        if (!taskRepository.existsById(id)) return ResponseEntity.notFound().build();
        taskRepository.deleteById(id);
        return ResponseEntity.ok("Task deleted successfully.");
    }

    // GET /tasks/search?name=xyz
    @GetMapping("/search")
    public ResponseEntity<?> searchTasks(@RequestParam String name) {
        List<Task> tasks = taskRepository.findByNameContainingIgnoreCase(name);
        if (tasks.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(tasks);
    }

    // PUT /tasks/{id}/execute - Execute command
    @PutMapping("/{id}/execute")
    public ResponseEntity<?> executeTask(@PathVariable String id) {
        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isEmpty()) return ResponseEntity.notFound().build();

        Task task = taskOpt.get();
        TaskExecution execution = new TaskExecution();
        execution.setStartTime(new Date());

        try {
            // Execute the shell command
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("bash", "-c", task.getCommand());
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");

            process.waitFor();
            execution.setEndTime(new Date());
            execution.setOutput(output.toString());

            // Add execution to task and save
            task.getTaskExecutions().add(execution);
            taskRepository.save(task);

            return ResponseEntity.ok(execution);

        } catch (Exception e) {
            execution.setEndTime(new Date());
            execution.setOutput("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(execution);
        }
    }
}
