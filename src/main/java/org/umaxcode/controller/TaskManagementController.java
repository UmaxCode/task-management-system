package org.umaxcode.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.umaxcode.domain.dto.request.TaskCommentUpdateDto;
import org.umaxcode.domain.dto.request.TaskStatusUpdateDto;
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

    @GetMapping("/users/{email}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse retrieveUserTasks(@PathVariable String email) {

        List<TaskDto> usersTasks = taskManagementService.getUsersTasks(email);
        return SuccessResponse.builder()
                .message("Tasks retrieved successfully")
                .data(usersTasks)
                .build();
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse updateTaskStatus(@PathVariable("id") String id,
                                            @RequestBody TaskStatusUpdateDto request
    ) {

        TaskDto updatedTask = taskManagementService.updateTaskStatus(id, request);
        return SuccessResponse.builder()
                .message("Task status updated successfully")
                .data(updatedTask)
                .build();
    }

    @PatchMapping("/{id}/comment")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse updateTaskComment(@PathVariable("id") String id,
                                             @RequestBody TaskCommentUpdateDto request
    ) {

        TaskDto updatedTask = taskManagementService.updateTaskComment(id, request);
        return SuccessResponse.builder()
                .message("Task comment updated successfully")
                .data(updatedTask)
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
