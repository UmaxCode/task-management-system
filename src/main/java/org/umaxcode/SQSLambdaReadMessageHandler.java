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

import java.util.HashMap;
import java.util.Map;

public class SQSLambdaReadMessageHandler implements RequestHandler<SQSEvent, String> {

    private final SnsClient snsClient;
    private final SfnClient stepFunctionsClient;

    public SQSLambdaReadMessageHandler() {
        this.snsClient = SnsClient.create();
        this.stepFunctionsClient = SfnClient.create();
    }

    @Override
    public String handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            // Process each message
            String messageBody = message.getBody();
            String messageReason = message.getMessageAttributes().get("reason").getStringValue();

            System.out.println("Message body: " + messageBody);
            System.out.println("Message attributes: " + messageReason);

            String taskId = message.getMessageAttributes().get("taskId").getStringValue();
            String receiver = message.getMessageAttributes().get("receiver").getStringValue();
            String name = message.getMessageAttributes().get("name").getStringValue();
            String description = message.getMessageAttributes().get("description").getStringValue();
            String deadline = message.getMessageAttributes().get("deadline").getStringValue();
            String topicArn = message.getMessageAttributes().get("topicArn").getStringValue();

            if ("task-creation".equals(messageReason)) {
                System.out.println("Sending task creation notification");
                sendTaskNotification(receiver, name, description, deadline,
                        topicArn, "New Task Assignment");
            } else if ("task-hit-deadline".equals(messageReason)) {
                System.out.println("Sending task deadline notification");
                try {
                    triggerStepFunction(taskId, topicArn, name, description, receiver, deadline);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            } else if ("task-approach-deadline".equals(messageReason)) {
                System.out.println("Sending task approach deadline notification");
                sendTaskNotification(receiver, name, description, deadline,
                        topicArn, "Task Approach Deadline");
            } else {
                context.getLogger().log("Invalid message reason");
            }
        }
        return "Processed message successfully!";
    }

    private void sendTaskNotification(String receiver, String name, String description,
                                      String deadline, String topicArn, String title
    ) {
        // Create message attributes sns filtering
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("endpointEmail", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(receiver)
                .build());

        String messageContent = String.format("Name: %s\nDescription: %s\nDeadline: %s", name, description, deadline);

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
                                     String receiver, String deadline ) throws JsonProcessingException {
        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("taskId", taskId);
        jsonMap.put("workflowType", "task-deadline-hit");
        jsonMap.put("taskName", name);
        jsonMap.put("taskDescription", description);
        jsonMap.put("receiver", receiver);
        jsonMap.put("topicArn", topicArn);
        jsonMap.put("taskDeadline", deadline);

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
