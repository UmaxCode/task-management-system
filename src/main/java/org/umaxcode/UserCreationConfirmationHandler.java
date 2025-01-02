package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

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
        String stepFunctionArn = System.getenv("STEP_FUNCTION_ARN");
        StartExecutionRequest startExecutionRequest = StartExecutionRequest.builder()
                .stateMachineArn(stepFunctionArn)
                .input("{\"email\":\"" + email + "\"}")
                .build();

        StartExecutionResponse response = stepFunctionsClient.startExecution(startExecutionRequest);

        context.getLogger().log("Step Function execution started with ARN: " + response.executionArn());

        return "Successfully triggered the state machine";
    }
}
