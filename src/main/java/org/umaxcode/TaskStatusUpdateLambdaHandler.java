package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.Map;

public class TaskStatusUpdateLambdaHandler implements RequestHandler<Map<String, String>, Void> {

    private final DynamoDbClient dynamoDbClient;
    private final String tasksTableName;

    public TaskStatusUpdateLambdaHandler() {
        this.dynamoDbClient = DynamoDbClient.create();
        this.tasksTableName = System.getenv("TASKS_TABLE_NAME");
    }

    @Override
    public Void handleRequest(Map<String, String> event, Context context) {

        // Define the primary key
        Map<String, AttributeValue> key = Map.of(
                "taskId", AttributeValue.builder().s(event.get("id")).build()
        );

        // Create the UpdateItemRequest
        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                .tableName(tasksTableName)
                .key(key)
                .updateExpression("SET #status = :status")
                .expressionAttributeValues(Map.of(
                        ":status", AttributeValue.builder().s("expired").build()
                )).expressionAttributeNames(Map.of(
                        "#status", "status"
                ))
                .build();

        // Execute the update
        dynamoDbClient.updateItem(updateItemRequest);
        return null;
    }
}
