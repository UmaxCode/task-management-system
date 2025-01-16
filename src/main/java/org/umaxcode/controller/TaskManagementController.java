package org.umaxcode.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.umaxcode.domain.dto.request.*;
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
    @PreAuthorize(value = "hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse createTask(@RequestBody TasksCreationDto request, @AuthenticationPrincipal Jwt jwt) {

        String adminEmail = jwt.getClaimAsString("email");
        TaskDto createdTask = taskManagementService.createItem(request, adminEmail);
        return SuccessResponse.builder()
                .message("Task created successfully")
                .data(createdTask)
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize(value = "hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse retrieveTask(@PathVariable("id") String taskId) {

        TaskDto taskDto = taskManagementService.readItem(taskId);
        return SuccessResponse.builder()
                .message("Task retrieved successfully")
                .data(taskDto)
                .build();
    }

    @GetMapping("/users/{email}")
    @PreAuthorize(value= "hasAnyRole('ADMIN', 'USER')")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse retrieveUserTasks(@PathVariable String email) {

        List<TaskDto> usersTasks = taskManagementService.getUsersTasks(email);
        return SuccessResponse.builder()
                .message("Tasks retrieved successfully")
                .data(usersTasks)
                .build();
    }

    @PatchMapping("/{id}/completed")
    @PreAuthorize(value = "hasRole('USER')")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse makeTaskAsCompleted(@PathVariable("id") String id, @AuthenticationPrincipal Jwt jwt) {

        TaskDto updatedTask = taskManagementService.makeTaskAsCompleted(id, jwt);
        return SuccessResponse.builder()
                .message("Task status updated successfully")
                .data(updatedTask)
                .build();
    }

    @PatchMapping("/{id}/reopen")
    @PreAuthorize(value = "hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse reopenTask(@PathVariable("id") String id, @RequestBody TaskReopenDto request) {

        TaskDto updatedTask = taskManagementService.reopenTask(id, request);
        return SuccessResponse.builder()
                .message("Task status updated successfully")
                .data(updatedTask)
                .build();
    }

    @PatchMapping("/{id}/comment")
    @PreAuthorize(value = "hasRole('USER')")
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

    @PatchMapping("/{id}/reassign")
    @PreAuthorize(value = "hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse reAssignTask(@PathVariable("id") String id, @RequestBody ReassignTaskDto request
    ) {

        TaskDto updatedTask = taskManagementService.reAssignTask(id, request);
        return SuccessResponse.builder()
                .message("Task reassigned successfully")
                .data(updatedTask)
                .build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize(value = "hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse updateTaskDetails(@PathVariable("id") String id, @RequestBody TaskDetailsUpdateDto request
    ) {

        TaskDto updatedTask = taskManagementService.updateTaskDetails(id, request);
        return SuccessResponse.builder()
                .message("Task details updated successfully")
                .data(updatedTask)
                .build();
    }

    @GetMapping
    @PreAuthorize(value = "hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse retrieveAllTasks() {

        List<TaskDto> response = taskManagementService.getAllTasks();
        return SuccessResponse.builder()
                .message("Tasks retrieved successfully")
                .data(response)
                .build();
    }
}
