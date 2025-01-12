package org.umaxcode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.umaxcode.service.SQSService;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SQSServiceImpl implements SQSService {

    private final SqsClient sqsClient;

    @Override
    public void sendMessageToQueue(String messageBody, Map<String, MessageAttributeValue> messageAttributes, String queueUrl) {
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .messageAttributes(messageAttributes)
                .build();

        sqsClient.sendMessage(sendMessageRequest);
        System.out.println("Message sent to SQS: ");
    }


    public static Map<String, MessageAttributeValue> createQueueMessage(String reason, Map<String, AttributeValue> taskDetails, String subject, String topicArn) {
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

        messageAttributes.put("taskId", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(taskDetails.get("taskId").s())
                .build());

        messageAttributes.put("name", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(taskDetails.get("name").s())
                .build());

        messageAttributes.put("description", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(taskDetails.get("description").s())
                .build());

        messageAttributes.put("receiver", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(taskDetails.get("responsibility").s())
                .build());

        messageAttributes.put("assignedBy", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(taskDetails.get("assignedBy").s())
                .build());

        messageAttributes.put("deadline", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(taskDetails.get("deadline").s())
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

        return messageAttributes;
    }
}
