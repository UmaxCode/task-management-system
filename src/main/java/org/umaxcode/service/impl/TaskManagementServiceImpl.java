package org.umaxcode.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.umaxcode.domain.dto.request.TasksCreationDto;
import org.umaxcode.domain.dto.response.TaskDto;
import org.umaxcode.domain.enums.TaskStatus;
import org.umaxcode.exception.TaskManagementException;
import org.umaxcode.mapper.TaskMapper;
import org.umaxcode.service.TaskManagementService;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

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
    public void createItem(TasksCreationDto request) {

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("taskId", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        item.put("name", AttributeValue.builder().s(request.name()).build());
        item.put("description", AttributeValue.builder().s(request.description()).build());
        item.put("status", AttributeValue.builder().s(request.status().getName()).build());
        item.put("responsibility", AttributeValue.builder().s(request.responsibility()).build());
        item.put("deadline", AttributeValue.builder().s(request.deadline().toString()).build());


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
}
