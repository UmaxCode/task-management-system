package org.umaxcode.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.umaxcode.domain.dto.request.TasksCreationDto;
import org.umaxcode.domain.dto.response.SuccessResponse;
import org.umaxcode.domain.dto.response.TaskDto;
import org.umaxcode.service.TaskManagementService;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskManagementController {

    private final TaskManagementService taskManagementService;

    @PostMapping
    @PreAuthorize(value = "hasRole('apiAdmins')")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse createTask(@RequestBody TasksCreationDto request) {

        taskManagementService.createItem(request);
        return SuccessResponse.builder()
                .message("Task created successfully")
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize(value = "hasRole('apiAdmins')")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse retrieveTask(@PathVariable("id") String taskId) {

        TaskDto taskDto = taskManagementService.readItem(taskId);
        return SuccessResponse.builder()
                .message("Task retrieved successfully")
                .data(taskDto)
                .build();
    }

    @GetMapping
    @PreAuthorize(value = "hasRole('apiAdmins')")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse retrieveAllTasks() {

        List<TaskDto> response = taskManagementService.getAllTasks();
        return SuccessResponse.builder()
                .message("Tasks retrieved successfully")
                .data(response)
                .build();
    }
}
