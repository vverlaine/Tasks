package com.proyect.Tasks.controller;

import com.proyect.Tasks.model.Task;
import com.proyect.Tasks.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;


// Lo Definimos como Rest
// La clase se va a mapear con /task
@RestController
@RequestMapping("/task")
public class TaskController {

    //Definimos el tiempo del delay
    private static final int DELAY_PER_ITEM_MS = 1;
    //Traemos el repositorio
    @Autowired
    private final TaskRepository taskRepository;
    //Notificador de notificaciones
    private EmitterProcessor<Task> emitterProcessor;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @PostConstruct
    private void setEmitterProcessor() {
        emitterProcessor = EmitterProcessor.<Task>create();
    }

    //Metodo para devolver todas las tareas
    @GetMapping(value = "/getTask", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<Task> getTask() {
        return taskRepository.findAll().delayElements(Duration.ofMillis(DELAY_PER_ITEM_MS));
    }

    //Metodo para consultar una tarea en especifico
    @GetMapping(value = "/getTask/{id}", produces = TEXT_EVENT_STREAM_VALUE)
    public Mono<ResponseEntity<Task>> getTaskId(@PathVariable(value = "userid") String id) {
        return taskRepository.findById(id).map(saveId -> ResponseEntity.ok(saveId))
                .defaultIfEmpty(ResponseEntity.notFound().build()).delayElement(Duration.ofMillis(DELAY_PER_ITEM_MS));
    }

    //Metodo para actualizar una tarea
    @PutMapping(value = "/updateTask/{id}")
    public Mono<ResponseEntity<Task>> updateTask(@PathVariable(value = "id") String id
            , String userid, String title, boolean complete) {
        return taskRepository.findById(id)
                .flatMap(existingTask -> {
                    existingTask.setUserid(userid);
                    existingTask.setTitle(title);
                    existingTask.setComplete(complete);
                    return taskRepository.save(existingTask);
                }).map(updatedTask -> new ResponseEntity<>(updatedTask, HttpStatus.OK))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    //Metodo para insertar una nueva tarea
    @PostMapping(value = "/createTask")
    public Mono<Task> addTask(@Valid @RequestBody Task task) {
        return taskRepository.save(task);
    }

    //Metodo para eliminar una tarea
    @DeleteMapping(value = "/deleteTask/{id}")
    public Mono<ResponseEntity<Void>> deleteTask(@PathVariable(value = "id") String id) {
        return taskRepository.findById(id)
                .flatMap(existingTask ->
                        taskRepository.delete(existingTask))
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.ACCEPTED)))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /*
    FLUJO REACTIVO ENVIANDO DATOS DE LA TAREA
     */

    private Flux<ServerSentEvent<Task>> getTaskSSE() {
        return emitterProcessor.log()
                .map((task) -> {
                    System.out.println("Sending Task" + task.getId());
                    return ServerSentEvent.<Task>builder()
                            .id(UUID.randomUUID().toString())
                            .event("Task Result")
                            .data(task)
                            .build();
                }).concatWith(Flux.never());

    }


    private Flux<ServerSentEvent<Task>> getNotificationHertbeat() {
        return Flux.interval(Duration.ofSeconds(2))
                .map(i -> {
                    System.out.println(String.format("Sending heartbeat [%s] ...", i.toString()));
                    return ServerSentEvent.<Task>builder()
                            .id(String.valueOf(i))
                            .event("Heartbeat Result")
                            .data(null)
                            .build();
                });
    }

    @GetMapping("/notification/sse")
    public Flux<ServerSentEvent<Task>> getJobResultNotification() {
        return Flux.merge(getNotificationHertbeat(), getTaskSSE());
    }

}
