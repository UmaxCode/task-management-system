package org.umaxcode.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.umaxcode.domain.dto.request.*;
import org.umaxcode.domain.dto.response.TaskDto;
import org.umaxcode.domain.enums.TaskStatus;
import org.umaxcode.exception.TaskManagementException;
import org.umaxcode.mapper.TaskMapper;
import org.umaxcode.service.TaskManagementService;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TaskManagementServiceImpl implements TaskManagementService {

    private final DynamoDbClient dynamoDbClient;
    private final String tasksTableName;

    @Value("${application.aws.userPoolId}")
    private String userPoolId;

    public TaskManagementServiceImpl(DynamoDbClient dynamoDbClient, CognitoIdentityProviderClient cognitoClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tasksTableName = System.getenv("TASKS_TABLE_NAME");
    }

    @Override
    public void createItem(TasksCreationDto request, String email) {

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("taskId", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        item.put("name", AttributeValue.builder().s(request.name()).build());
        item.put("description", AttributeValue.builder().s(request.description()).build());
        item.put("status", AttributeValue.builder().s(TaskStatus.OPEN.getName()).build());
        item.put("responsibility", AttributeValue.builder().s(request.responsibility()).build());
        item.put("deadline", AttributeValue.builder().s(request.deadline().toString()).build());
        item.put("assignedBy", AttributeValue.builder().s(email).build());


        PutItemRequest putRequest = PutItemRequest.builder()
                .tableName(tasksTableName)
                .item(item)
                .build();

        PutItemResponse putItemResponse = dynamoDbClient.putItem(putRequest);
        System.out.println("results " + putItemResponse.attributes());
    }

    @Override
    public TaskDto readItem(String id) {

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("taskId", AttributeValue.builder().s(id).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(tasksTableName)
                .key(key)
                .build();

        Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();

        if (item != null) {
            return TaskDto.builder()
                    .id(item.get("taskId").s())
                    .name(item.get("name").s())
                    .description(item.get("description").s())
                    .status(TaskStatus.fromValue(item.get("status").s()))
                    .deadline(item.get("deadline").s())
                    .responsibility(item.get("responsibility").s())
                    .build();
        }

        throw new TaskManagementException("Task with the Id : " + id + "not found");
    }

    @Override
    public List<TaskDto> getUsersTasks(String email) {

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tasksTableName)  // Main table name
                .indexName("responsibilityIndex")   // GSI name
                .keyConditionExpression("responsibility = :email")
                .expressionAttributeValues(Map.of(
                        ":email", AttributeValue.builder().s(email).build()
                ))
                .build();

        QueryResponse queryResponse = dynamoDbClient.query(queryRequest);

        return TaskMapper.mapToListTaskDto(queryResponse.items());
    }

    @Override
    public List<TaskDto> getAllTasks() {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tasksTableName)
                .build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

        return TaskMapper.mapToListTaskDto(scanResponse.items());
    }

    @Override
    public TaskDto makeTaskAsCompleted(String id, Jwt jwt) {

        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        boolean isAdmin = groups != null && groups.contains("apiAdmins");

        if (isAdmin) {
            throw new TaskManagementException("Admin is not allowed to mark a task as completed");
        }

        try {
            // Define the primary key
            Map<String, AttributeValue> key = Map.of(
                    "taskId", AttributeValue.builder().s(id).build()
            );

            // Create the UpdateItemRequest
            UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(tasksTableName)
                    .key(key)
                    .updateExpression("SET #status = :status")
                    .conditionExpression("#status = :open")
                    .expressionAttributeValues(Map.of(
                            ":status", AttributeValue.builder().s("completed").build(),
                            ":open", AttributeValue.builder().s("open").build()
                    ))
                    .expressionAttributeNames(Map.of(
                            "#status", "status"
                    ))
                    .returnValues("ALL_NEW")
                    .build();

            // Execute the update
            UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);

            return TaskMapper.mapToTaskDto(updateItemResponse.attributes());
        } catch (ConditionalCheckFailedException ex) {
            throw new TaskManagementException("Invalid task status update: [completed, expired] -> completed");
        }
    }

    @Override
    public TaskDto reAssignTask(String id, ReassignTaskDto request) {

        try {
            // Define the primary key
            Map<String, AttributeValue> key = Map.of(
                    "taskId", AttributeValue.builder().s(id).build()
            );

            // Create the UpdateItemRequest
            UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(tasksTableName)
                    .key(key)
                    .updateExpression("SET responsibility = :responsibility")
                    .conditionExpression("#status = :open")
                    .expressionAttributeValues(Map.of(
                            ":responsibility", AttributeValue.builder().s(request.userEmail()).build(),
                            ":open", AttributeValue.builder().s("open").build()
                    ))
                    .expressionAttributeNames(Map.of(
                            "#status", "status"
                    ))
                    .returnValues("ALL_NEW")
                    .build();

            // Execute the update
            UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);

            return TaskMapper.mapToTaskDto(updateItemResponse.attributes());
        } catch (ConditionalCheckFailedException ex) {
            throw new TaskManagementException("Invalid task status [expired, completed] during reassignment");
        }
    }

    @Override
    public TaskDto reopenTask(String id, TaskReopenDto request) {

        try {
            // Define the primary key
            Map<String, AttributeValue> key = Map.of(
                    "taskId", AttributeValue.builder().s(id).build()
            );

            // Calculate the current time + 1 hour in seconds since the epoch
            long currentTimeInSeconds = Instant.now().getEpochSecond();
            long minimumDeadline = currentTimeInSeconds + 3600;

            // Create the UpdateItemRequest
            UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(tasksTableName)
                    .key(key)
                    .updateExpression("SET #status = :status, deadline = :deadline")
                    .conditionExpression("#status = :expired AND deadline >= :minimumDeadline")
                    .expressionAttributeValues(Map.of(
                            ":status", AttributeValue.builder().s("open").build(),
                            ":deadline", AttributeValue.builder().s(request.deadline().toString()).build(),
                            ":expired", AttributeValue.builder().s("expired").build(),
                            ":minimumDeadline", AttributeValue.builder().n(String.valueOf(minimumDeadline)).build()
                    ))
                    .expressionAttributeNames(Map.of(
                            "#status", "status"
                    ))
                    .returnValues("ALL_NEW")
                    .build();

            // Execute the update
            UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);

            return TaskMapper.mapToTaskDto(updateItemResponse.attributes());
        } catch (ConditionalCheckFailedException ex) {
            throw new TaskManagementException("Invalid task status update: [open, completed] -> open" +
                    " or or the deadline is less than 1 hour in the future.");
        }
    }

    @Override
    public TaskDto updateTaskDetails(String id, TaskDetailsUpdateDto request) {

        Map<String, AttributeValue> key = Map.of(
                "taskId", AttributeValue.builder().s(id).build()
        );

        try {
            UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(tasksTableName)
                    .key(key)
                    .updateExpression("SET #name = :name, description = :description")
                    .conditionExpression("#status = :open")
                    .expressionAttributeValues(Map.of(
                            ":status", AttributeValue.builder().s("open").build(),
                            ":name", AttributeValue.builder().s(request.name()).build(),
                            ":description", AttributeValue.builder().s(request.description()).build()
                    ))
                    .expressionAttributeNames(Map.of(
                            "#status", "status",
                            "#name", "name"
                    ))
                    .returnValues("ALL_NEW")
                    .build();

            // Execute the update
            UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);

            return TaskMapper.mapToTaskDto(updateItemResponse.attributes());
        } catch (ConditionalCheckFailedException ex) {
            throw new TaskManagementException("Only open tasks can be updated");
        }
    }

    @Override
    public TaskDto updateTaskComment(String id, TaskCommentUpdateDto request) {

        // Define the primary key
        Map<String, AttributeValue> key = Map.of(
                "taskId", AttributeValue.builder().s(id).build()
        );

        // Create the UpdateItemRequest
        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                .tableName(tasksTableName)
                .key(key)
                .updateExpression("SET #comment = :comment")
                .expressionAttributeValues(Map.of(
                        ":comment", AttributeValue.builder().s(request.comment()).build()
                ))
                .expressionAttributeNames(Map.of(
                        "#comment", "comment"
                ))
                .returnValues("ALL_NEW")
                .build();

        // Execute the update
        UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);

        return TaskMapper.mapToTaskDto(updateItemResponse.attributes());
    }
}
