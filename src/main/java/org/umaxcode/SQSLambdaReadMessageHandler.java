package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQSLambdaReadMessageHandler implements RequestHandler<SQSEvent, Void> {

    private final SnsClient snsClient;
    private final SfnClient stepFunctionsClient;

    public SQSLambdaReadMessageHandler() {
        this.snsClient = SnsClient.create();
        this.stepFunctionsClient = SfnClient.create();
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            // Process each message
            String messageReason = message.getMessageAttributes().get("reason").getStringValue();

            String taskId = message.getMessageAttributes().get("taskId").getStringValue();
            String receiver = message.getMessageAttributes().get("receiver").getStringValue();
            String assignedBy = message.getMessageAttributes().get("assignedBy").getStringValue();
            String name = message.getMessageAttributes().get("name").getStringValue();
            String description = message.getMessageAttributes().get("description").getStringValue();
            String deadline = message.getMessageAttributes().get("deadline").getStringValue();
            String topicArn = message.getMessageAttributes().get("topicArn").getStringValue();
            String subject = message.getMessageAttributes().get("messageSubject").getStringValue();

            if ("task-hit-deadline".equals(messageReason)) {
                System.out.println("Sending task deadline notification");
                try {
                    triggerStepFunction(taskId, topicArn, name, description, receiver, subject, deadline, assignedBy);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                break;
            }

            sendTaskNotification(receiver, assignedBy, name, description, deadline,
                    topicArn, subject);
        }
        return null;
    }

    private void sendTaskNotification(String receiver, String assignedBy, String name, String description,
                                      String deadline, String topicArn, String title
    ) {

        // Create message attributes sns filtering
        List<String> listOfRecipients = new ArrayList<>();
        listOfRecipients.add(receiver);
        listOfRecipients.add(assignedBy);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonList;
        try {
            jsonList = objectMapper.writeValueAsString(listOfRecipients);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("endpointEmail", MessageAttributeValue.builder()
                .dataType("String.Array")
                .stringValue(jsonList)
                .build());


        String messageContent = String.format("Task name: %s\nTask description: %s\nTask deadline: %s\nAssigned to: %s\nAssigned by: %s",
                name, description, deadline, receiver, assignedBy);

        PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(topicArn)
                .subject(title)
                .message(messageContent)
                .messageAttributes(messageAttributes)
                .build();

        // Publish the message
        try {
            PublishResponse publishResponse = snsClient.publish(publishRequest);
            System.out.println(publishResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void triggerStepFunction(String taskId, String topicArn, String name, String description,
                                     String receiver, String snsSubject, String deadline, String assignedBy) throws JsonProcessingException {
        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("taskId", taskId);
        jsonMap.put("workflowType", "task-deadline-hit");
        jsonMap.put("taskName", name);
        jsonMap.put("taskDescription", description);
        jsonMap.put("receiver", receiver);
        jsonMap.put("assignedBy", assignedBy);
        jsonMap.put("topicArn", topicArn);
        jsonMap.put("taskDeadline", deadline);
        jsonMap.put("snsSubject", snsSubject);

        ObjectMapper objectMapper = new ObjectMapper();

        String jsonString = objectMapper.writeValueAsString(jsonMap);

        String stepFunctionArn = System.getenv("STEP_FUNCTION_ARN");
        StartExecutionRequest startExecutionRequest = StartExecutionRequest.builder()
                .stateMachineArn(stepFunctionArn)
                .input(jsonString)
                .build();

        stepFunctionsClient.startExecution(startExecutionRequest);

        System.out.println("Step function triggered : task hit deadline");
    }
}
