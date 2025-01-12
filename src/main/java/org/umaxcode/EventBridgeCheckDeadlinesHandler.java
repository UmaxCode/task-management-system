package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventBridgeCheckDeadlinesHandler implements RequestHandler<Object, Void> {

    private final DynamoDbClient dynamoDbClient;
    private final SqsClient sqsClient;
    private final String tasksTableName;
    private final String taskClosedTopicArn;
    private final String taskDeadlineTopicArn;
    private final String queueUrl;
    private final ExecutorService executor;

    public EventBridgeCheckDeadlinesHandler() {
        dynamoDbClient = DynamoDbClient.create();
        sqsClient = SqsClient.create();
        tasksTableName = System.getenv("TASKS_TABLE_NAME");
        taskClosedTopicArn = System.getenv("TASKS_CLOSED_NOTIFICATION_TOPIC_ARN");
        taskDeadlineTopicArn = System.getenv("TASKS_DEADLINE_NOTIFICATION_TOPIC_ARN");
        queueUrl = System.getenv("QUEUE_URL");
        executor = Executors.newFixedThreadPool(2);
    }

    @Override
    public Void handleRequest(Object o, Context context) {

        System.out.println("Running EventBridgeCheckDeadlinesHandler");
        CompletableFuture<Void> checkHitDeadline = CompletableFuture
                .runAsync(this::checkTasksDeadlineDueAndWriteToSQS, executor);

        CompletableFuture<Void> checkNearingDeadline = CompletableFuture
                .runAsync(this::checkTasksWithAnHourToDeadlineAndWriteToSQS, executor);

        CompletableFuture<Void> allTasks = CompletableFuture.allOf(checkHitDeadline, checkNearingDeadline);

        allTasks.join();

        return null;
    }

    private void checkTasksDeadlineDueAndWriteToSQS() {

        LocalDateTime now = LocalDateTime.now();

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tasksTableName)  // Main table name
                .indexName("statusIndex")   // GSI name
                .keyConditionExpression("#status = :open")
                .filterExpression("deadline <= :current")
                .expressionAttributeValues(Map.of(
                        ":current", AttributeValue.builder().s(now.toString()).build(),
                        ":open", AttributeValue.builder().s("open").build()
                )).expressionAttributeNames(Map.of(
                        "#status", "status"
                ))
                .build();

        QueryResponse queryResponse = dynamoDbClient.query(queryRequest);

        System.out.println("Info: checkTasksDeadlineDueAndWriteToSQS");

        if (queryResponse.hasItems()) {
            writeToQueue(queryResponse, "A task has reach deadline",
                    "task-hit-deadline", "Task has been closed",
                    taskClosedTopicArn);
        }
    }

    private void checkTasksWithAnHourToDeadlineAndWriteToSQS() {

        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime oneHourFromNow = currentTime.plusHours(1);

        // Query tasks nearing their deadlines
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tasksTableName)
                .indexName("statusIndex") // GSI on `deadline`
                .keyConditionExpression("#status = :open")
                .filterExpression("deadline > :current AND deadline <= :oneHourLater AND isNotifiedForApproachDeadline = :false")
                .expressionAttributeValues(Map.of(
                        ":current", AttributeValue.builder().s(currentTime.toString()).build(),
                        ":oneHourLater", AttributeValue.builder().s(oneHourFromNow.toString()).build(),
                        ":open", AttributeValue.builder().s("open").build(),
                        ":false", AttributeValue.builder().n("0").build()
                )).expressionAttributeNames(Map.of(
                        "#status", "status"
                ))
                .build();

        QueryResponse queryResponse = dynamoDbClient.query(queryRequest);

        System.out.println("Info: checkTasksApproachDeadlineAndWriteToSQS");


        if (queryResponse.hasItems()) {
            writeToQueue(queryResponse, "A task has approach deadline",
                    "task-approach-deadline", "Task Approach Deadline",
                    taskDeadlineTopicArn);
        }
    }

    private void writeToQueue(QueryResponse response, String sQsMessageBody, String reason, String subject, String topicArn) {

        for (Map<String, AttributeValue> item : response.items()) {

            Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

            messageAttributes.put("taskId", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(item.get("taskId").s())
                    .build());

            messageAttributes.put("name", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(item.get("name").s())
                    .build());

            messageAttributes.put("description", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(item.get("description").s())
                    .build());

            messageAttributes.put("receiver", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(item.get("responsibility").s())
                    .build());

            messageAttributes.put("assignedBy", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(item.get("assignedBy").s())
                    .build());

            messageAttributes.put("deadline", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(item.get("deadline").s())
                    .build());

            messageAttributes.put("topicArn", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(topicArn)
                    .build());

            messageAttributes.put("reason", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(reason)
                    .build());

            messageAttributes.put("messageSubject", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(subject)
                    .build());

            // Send the message to SQS with attributes
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(sQsMessageBody)
                    .messageAttributes(messageAttributes)
                    .build();

            sqsClient.sendMessage(sendMessageRequest);

            if ("task-approach-deadline".equals(reason)) {  // To ensure tasks are only notified once when approaching their deadline
                try {

                    String taskId = item.get("taskId").s();
                    UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                            .tableName(tasksTableName)
                            .key(Map.of("taskId", AttributeValue.builder().s(taskId).build()))
                            .updateExpression("SET isNotifiedForApproachDeadline = :true")
                            .expressionAttributeValues(Map.of(
                                    ":true", AttributeValue.builder().n("1").build()
                            ))
                            .build();

                    dynamoDbClient.updateItem(updateRequest);
                    System.out.println("Successfully updated task with ID: " + taskId + " to set isNotifiedForApproachDeadline = true");
                } catch (Exception e) {
                    System.err.println("Failed to update task: " + e.getMessage());
                }
            }
        }

    }

}
