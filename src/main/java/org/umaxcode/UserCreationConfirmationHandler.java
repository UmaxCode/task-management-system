package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

import java.util.HashMap;
import java.util.Map;

public class UserCreationConfirmationHandler implements RequestHandler<Map<String, String>, String> {

    private final SfnClient stepFunctionsClient;

    public UserCreationConfirmationHandler() {
        this.stepFunctionsClient = SfnClient.create();
    }

    @Override
    public String handleRequest(Map<String, String> event, Context context) {

        System.out.println(event);
        String email = event.get("email");

        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("email", email);
        jsonMap.put("workflowType", "post-confirmation-sns");

        // Convert to JSON string
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonString = objectMapper.writeValueAsString(jsonMap);

            String stepFunctionArn = System.getenv("STEP_FUNCTION_ARN");
            StartExecutionRequest startExecutionRequest = StartExecutionRequest.builder()
                    .stateMachineArn(stepFunctionArn)
                    .input(jsonString)
                    .build();

            StartExecutionResponse response = stepFunctionsClient.startExecution(startExecutionRequest);

            context.getLogger().log("Step Function execution started with ARN: " + response.executionArn());

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return "Successfully triggered the state machine";
    }
}
