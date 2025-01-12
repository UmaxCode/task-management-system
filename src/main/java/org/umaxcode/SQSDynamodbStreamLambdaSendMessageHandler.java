package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.HashMap;
import java.util.Map;

public class SQSDynamodbStreamLambdaSendMessageHandler implements RequestHandler<DynamodbEvent, String> {

    private final SqsClient sqsClient;

    public SQSDynamodbStreamLambdaSendMessageHandler() {
        sqsClient = SqsClient.create();
    }

    @Override
    public String handleRequest(DynamodbEvent event, Context context) {

        String queueUrl = System.getenv("QUEUE_URL");
        String topicArn = System.getenv("TASKS_ASSIGNMENT_NOTIFICATION_TOPIC_ARN");

        System.out.println("SQS lambda function is triggered and to : " + queueUrl);
        for (DynamodbEvent.DynamodbStreamRecord record : event.getRecords()) {
            if ("INSERT".equals(record.getEventName())) {
                Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();// Get the new image (data)

                // Add attributes to the message
                Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

                messageAttributes.put("taskId", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(newImage.get("taskId").getS())
                        .build());

                messageAttributes.put("name", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(newImage.get("name").getS())
                        .build());

                messageAttributes.put("description", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(newImage.get("description").getS())
                        .build());

                messageAttributes.put("receiver", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(newImage.get("responsibility").getS())
                        .build());

                messageAttributes.put("assignedBy", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(newImage.get("assignedBy").getS())
                        .build());

                messageAttributes.put("deadline", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(newImage.get("deadline").getS())
                        .build());

                messageAttributes.put("topicArn", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(topicArn)
                        .build());

                messageAttributes.put("reason", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("task-creation")
                        .build());

                messageAttributes.put("messageSubject", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("New Task Assignment")
                        .build());

                // Send the message to SQS with attributes
                SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody("A new task has been created")
                        .messageAttributes(messageAttributes)
                        .build();

                sendMessageToQueue(sendMessageRequest);
            }
        }
        return "success";
    }

    private void sendMessageToQueue(SendMessageRequest sendMessageRequest) {

        SendMessageResponse sendMessageResponse = sqsClient.sendMessage(sendMessageRequest);
        System.out.println(sendMessageResponse);
        System.out.println("Message sent to SQS: " + sendMessageResponse.messageId());
    }
}
