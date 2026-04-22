package com.politicanegocio.core.repository;

import com.politicanegocio.core.model.TaskInstance;
import com.politicanegocio.core.model.TaskInstanceStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskInstanceRepository extends MongoRepository<TaskInstance, String> {
    List<TaskInstance> findByLaneId(String laneId);
    List<TaskInstance> findByLaneIdAndStatus(String laneId, TaskInstanceStatus status);
    List<TaskInstance> findByAssignedTo(String assignedTo);
    boolean existsByProcessInstanceIdAndTaskIdAndStatus(String processInstanceId, String taskId, TaskInstanceStatus status);
    boolean existsByProcessInstanceIdAndTaskIdAndStatusNot(String processInstanceId, String taskId, TaskInstanceStatus status);
    
}
