package org.umaxcode.service;

import org.umaxcode.domain.dto.request.TasksCreationDto;
import org.umaxcode.domain.dto.response.TaskDto;

import java.util.List;

public interface TaskManagementService {

    Object createItem(TasksCreationDto item);

    TaskDto readItem(String id);

    List<Object> getAllTasks();
}
