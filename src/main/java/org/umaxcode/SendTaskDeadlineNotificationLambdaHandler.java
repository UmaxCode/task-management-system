package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendTaskDeadlineNotificationLambdaHandler implements RequestHandler<Map<String, String>, Void> {

    private final SnsClient snsClient;

    public SendTaskDeadlineNotificationLambdaHandler() {
        this.snsClient = SnsClient.create();
    }

    @Override
    public Void handleRequest(Map<String, String> event, Context context) {

        String taskId = event.get("taskId");
        String taskName = event.get("taskName");
        String taskDescription = event.get("taskDescription");
        String receiver = event.get("receiver");
        String assignedBy = event.get("assignedBy");
        String topicArn = event.get("topicArn");
        String taskDeadline = event.get("taskDeadline");

        // Create message attributes sns filtering
        List<String> listOfRecipients = new ArrayList<>();
        listOfRecipients.add(receiver);
        listOfRecipients.add(assignedBy);

        try {

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonList = objectMapper.writeValueAsString(listOfRecipients);

            Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
            messageAttributes.put("endpointEmail", MessageAttributeValue.builder()
                    .dataType("String.Array")
                    .stringValue(jsonList)
                    .build());

            String messageContent = String.format("Id: %s\nName: %s\nDescription: %s\nDeadline: %s\nAssigned by: %s",
                    taskId, taskName, taskDescription, taskDeadline, assignedBy);

            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject("Task has been closed")
                    .message(messageContent)
                    .messageAttributes(messageAttributes)
                    .build();

            // Publish the message
            PublishResponse publishResponse = snsClient.publish(publishRequest);
            System.out.println(publishResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
