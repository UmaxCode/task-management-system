package org.umaxcode.service;

import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;

public interface SQSService {

    void sendMessageToQueue(String messageBody, Map<String, MessageAttributeValue> messageAttributes, String queueUrl);
}
