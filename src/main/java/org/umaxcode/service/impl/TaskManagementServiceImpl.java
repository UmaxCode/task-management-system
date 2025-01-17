package org.umaxcode.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.umaxcode.domain.dto.request.*;
import org.umaxcode.domain.dto.response.TaskDto;
import org.umaxcode.domain.enums.TaskStatus;
import org.umaxcode.exception.TaskManagementException;
import org.umaxcode.mapper.TaskMapper;
import org.umaxcode.service.SQSService;
import org.umaxcode.service.TaskManagementService;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TaskManagementServiceImpl implements TaskManagementService {

    private final DynamoDbClient dynamoDbClient;
    private final String tasksTableName;
    private final SQSService sqsService;
    private final String queueUrl;
    private final String taskCompleteTopicArn;
    private final String taskReopenTopicArn;
    private final String taskAssignTopicArn;

    @Value("${application.aws.userPoolId}")
    private String userPoolId;

    public TaskManagementServiceImpl(DynamoDbClient dynamoDbClient, CognitoIdentityProviderClient cognitoClient, SQSService sqsService) {
        this.dynamoDbClient = dynamoDbClient;
        this.tasksTableName = System.getenv("TASKS_TABLE_NAME");
        this.queueUrl = System.getenv("QUEUE_URL");
        this.taskCompleteTopicArn = System.getenv("TASKS_COMPLETE_NOTIFICATION_TOPIC_ARN");
        this.taskReopenTopicArn = System.getenv("TASKS_REOPEN_NOTIFICATION_TOPIC_ARN");
        this.taskAssignTopicArn = System.getenv("TASKS_ASSIGNMENT_NOTIFICATION_TOPIC_ARN");
        this.sqsService = sqsService;
    }

    @Override
    public TaskDto createAndAssignTask(TasksCreationDto request, String email) {

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("taskId", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        item.put("name", AttributeValue.builder().s(request.name()).build());
        item.put("description", AttributeValue.builder().s(request.description()).build());
        item.put("status", AttributeValue.builder().s(TaskStatus.OPEN.getName()).build());
        item.put("responsibility", AttributeValue.builder().s(request.responsibility()).build());
        item.put("deadline", AttributeValue.builder().s(request.deadline().toString()).build());
        item.put("assignedBy", AttributeValue.builder().s(email).build());
        item.put("isNotifiedForApproachDeadline", AttributeValue.builder().n("0").build());


        PutItemRequest putRequest = PutItemRequest.builder()
                .tableName(tasksTableName)
                .item(item)
                .returnValues("ALL_OLD")
                .build();

        dynamoDbClient.putItem(putRequest);
        return TaskDto.builder()
                .id(item.get("taskId").s())
                .name(request.name())
                .description(request.description())
                .status(TaskStatus.OPEN)
                .responsibility(request.responsibility())
                .deadline(request.deadline().toString())
                .assignedBy(email)
                .build();
    }

    @Override
    public TaskDto fetchTask(String id) {

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

        String email = jwt.getClaimAsString("email");

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
                    .conditionExpression("#status = :open AND responsibility = :email")
                    .expressionAttributeValues(Map.of(
                            ":status", AttributeValue.builder().s("completed").build(),
                            ":open", AttributeValue.builder().s("open").build(),
                            ":email", AttributeValue.builder().s(email).build()
                    ))
                    .expressionAttributeNames(Map.of(
                            "#status", "status"
                    ))
                    .returnValues("ALL_NEW")
                    .build();

            // Execute the update
            UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);
            createMessageAndSendToQueue("Task has been completed", "task-complete",
                    updateItemResponse.attributes(), "Task Completed",
                    taskCompleteTopicArn);
            return TaskMapper.mapToTaskDto(updateItemResponse.attributes());
        } catch (ConditionalCheckFailedException ex) {
            throw new TaskManagementException("Invalid task status update: [completed, expired] -> completed or" +
                    "unauthorized modification");
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
            createMessageAndSendToQueue("Task has been reassigned", "task-reassign", updateItemResponse.attributes(),
                    "You have been Assigned A Task", taskAssignTopicArn);
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

            // Calculate the current time + 1 hour
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime oneHourFromNow = currentTime.plusHours(1);

            if(request.deadline().isBefore(oneHourFromNow)) {
                throw new TaskManagementException("Deadline must be greater or equal to 1 hour.");
            }

            // Create the UpdateItemRequest
            UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(tasksTableName)
                    .key(key)
                    .updateExpression("SET #status = :status, deadline = :deadline, isNotifiedForApproachDeadline = :false")
                    .conditionExpression("#status = :expired")
                    .expressionAttributeValues(Map.of(
                            ":status", AttributeValue.builder().s("open").build(),
                            ":deadline", AttributeValue.builder().s(request.deadline().toString()).build(),
                            ":expired", AttributeValue.builder().s("expired").build(),
                            ":false", AttributeValue.builder().n("0").build()
                    ))
                    .expressionAttributeNames(Map.of(
                            "#status", "status"
                    ))
                    .returnValues("ALL_NEW")
                    .build();

            // Execute the update
            UpdateItemResponse updateItemResponse = dynamoDbClient.updateItem(updateItemRequest);
            createMessageAndSendToQueue("Task has been reopened", "task-reopen",
                    updateItemResponse.attributes(), "Task Reopened",
                    taskReopenTopicArn);
            return TaskMapper.mapToTaskDto(updateItemResponse.attributes());
        } catch (ConditionalCheckFailedException ex) {
            throw new TaskManagementException("Invalid task status update: [open, completed] -> open");
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
                    .conditionExpression("#status = :status")
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

    private void createMessageAndSendToQueue(String messageBody, String reason, Map<String, AttributeValue> taskDetails, String subject, String topicArn) {
        Map<String, MessageAttributeValue> messageAttributes = SQSServiceImpl.createQueueMessage(reason, taskDetails, subject, topicArn);
        sqsService.sendMessageToQueue(messageBody, messageAttributes, queueUrl);
    }
}
