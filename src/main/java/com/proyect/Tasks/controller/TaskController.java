package com.proyect.Tasks.controller;

import com.proyect.Tasks.model.Task;
import com.proyect.Tasks.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@RestController
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private final TaskRepository taskRepository;

    private static final int DELAY_PER_ITEM_MS = 1;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping(value = "/getTask",produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<Task> getTask(){
        return taskRepository.findAll().delayElements(Duration.ofMillis(DELAY_PER_ITEM_MS));
    }



}
