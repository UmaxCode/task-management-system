package org.umaxcode.service.impl;

import org.springframework.stereotype.Service;
import org.umaxcode.domain.dto.request.TasksCreationDto;
import org.umaxcode.domain.dto.response.TaskDto;
import org.umaxcode.domain.enums.TaskStatus;
import org.umaxcode.exception.TaskManagementException;
import org.umaxcode.service.TaskManagementService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

@Service
public class TaskManagementServiceImpl implements TaskManagementService {

    private final DynamoDbClient dynamoDbClient;
    private final String tasksTableName;

    public TaskManagementServiceImpl(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tasksTableName = System.getenv("TASKS_TABLE_NAME");
    }

    @Override
    public Object createItem(TasksCreationDto request) {

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
        return putItemResponse.attributes();
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
    public List<Object> getAllTasks() {
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tasksTableName)
                .build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

        return Arrays.asList(scanResponse.items().toArray());
    }
}
