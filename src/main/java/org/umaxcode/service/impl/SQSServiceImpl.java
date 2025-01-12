package org.umaxcode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.umaxcode.service.SQSService;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

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
}
