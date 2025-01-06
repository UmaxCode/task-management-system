package org.umaxcode.service;

import org.umaxcode.domain.dto.request.TaskCommentUpdateDto;
import org.umaxcode.domain.dto.request.TaskStatusUpdateDto;
import org.umaxcode.domain.dto.request.TasksCreationDto;
import org.umaxcode.domain.dto.response.TaskDto;

import java.util.List;

public interface TaskManagementService {

    void createItem(TasksCreationDto item);

    TaskDto readItem(String id);

    List<TaskDto> getAllTasks();

    List<TaskDto> getUsersTasks(String email);

    TaskDto updateTaskStatus(String id, TaskStatusUpdateDto request);

    TaskDto updateTaskComment(String id, TaskCommentUpdateDto request);
}
