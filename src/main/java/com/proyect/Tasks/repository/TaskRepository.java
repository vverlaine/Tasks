package com.proyect.Tasks.repository;

import com.proyect.Tasks.model.Task;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/*Repositorio del collection
    Hereda de ReactiveMongoRepository
 */
public interface TaskRepository extends ReactiveMongoRepository<Task, String> {


}
