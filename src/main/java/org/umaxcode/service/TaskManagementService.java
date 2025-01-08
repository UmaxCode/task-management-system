package org.umaxcode.service;

import org.springframework.security.oauth2.jwt.Jwt;
import org.umaxcode.domain.dto.request.*;
import org.umaxcode.domain.dto.response.TaskDto;

import java.util.List;

public interface TaskManagementService {

    void createItem(TasksCreationDto item, String email);

    TaskDto readItem(String id);

    List<TaskDto> getAllTasks();

    List<TaskDto> getUsersTasks(String email);

    TaskDto makeTaskAsCompleted(String id, Jwt jwt);

    TaskDto reopenTask(String id, TaskReopenDto request);

    TaskDto updateTaskComment(String id, TaskCommentUpdateDto request);

    TaskDto reAssignTask(String id, ReassignTaskDto request);

    TaskDto updateTaskDetails(String id, TaskDetailsUpdateDto request);
}
