package org.umaxcode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.umaxcode.domain.enums.Role;
import org.umaxcode.utils.PasswordGenerator;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.sns.SnsClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.umaxcode.service.impl.SNSServiceImpl.subscribeToTopic;

public class UpdateInviteMessageAdminCreationLambdaHandler implements RequestHandler<CloudFormationCustomResourceEvent, Void> {

    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
    private final SnsClient snsClient;
    private final ExecutorService executor;

    public UpdateInviteMessageAdminCreationLambdaHandler() {
        this.cognitoIdentityProviderClient = CognitoIdentityProviderClient.create();
        this.snsClient = SnsClient.create();
        this.executor = Executors.newFixedThreadPool(2);
    }

    @Override
    public Void handleRequest(CloudFormationCustomResourceEvent event, Context context) {
        String status = "SUCCESS";
        Map<String, Object> properties = event.getResourceProperties();
        Map<String, Object> responseData = new HashMap<>();

        // Extract properties from the event
        String requestType = event.getRequestType();
        String responseUrl = event.getResponseUrl();
        String userPoolId = properties.get("UserPoolId").toString();
        String adminEmail = properties.get("AdminEmail").toString();
        String adminUsername = properties.get("AdminUsername").toString();
        String loginUrl = properties.get("FrontendLoginUrl").toString();
        String taskCompleteTopicArn = properties.get("TaskCompleteTopicArn").toString();
        String closedTaskTopicArn = properties.get("ClosedTaskTopicArn").toString();

        try {

            if ("Delete".equalsIgnoreCase(requestType)) {
                sendResponse(responseUrl, event, context, status, null);
                return null;
            }

            updateInviteMessageTemplate(loginUrl, userPoolId, responseData, context);

            createAdminUser(adminEmail, adminUsername, userPoolId, responseData, context);

            CompletableFuture<Void> taskCompletionTopicSubscription = CompletableFuture
                    .runAsync(() -> subscribeToTopic(snsClient, context, taskCompleteTopicArn, adminEmail), executor);

            CompletableFuture<Void> taskClosedTopicSubscription = CompletableFuture
                    .runAsync(() -> subscribeToTopic(snsClient, context, closedTaskTopicArn, adminEmail), executor);

            CompletableFuture<Void> topicSubscriptionTasks = CompletableFuture.allOf(taskCompletionTopicSubscription,
                    taskClosedTopicSubscription);

            topicSubscriptionTasks.join();

            // Send success response to CloudFormation
            sendResponse(responseUrl, event, context, status, responseData);

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            responseData.put("Error", e.getMessage());
        }

        return null;
    }

    private void updateInviteMessageTemplate(String loginUrl, String userPoolId, Map<String, Object> responseData, Context context) {

        MessageTemplateType inviteMessageTemplate = MessageTemplateType.builder()
                .emailMessage(String.format("""
                        <html>
                            <body>
                                <p> Hello Sir/Madam </p>
                                <p>Welcome to our Task Management System!</p>
                                <p>Your email is: <strong>{username}</strong> and your temporary password is: <strong>{####}</strong></p>
                                <p>Click <a href="%s">here</a> to sign in.</p>
                            </body>
                        </html>
                        """, loginUrl))
                .emailSubject("Task Management System")
                .build();

        cognitoIdentityProviderClient.updateUserPool(UpdateUserPoolRequest.builder()
                .userPoolId(userPoolId)
                .adminCreateUserConfig(AdminCreateUserConfigType.builder()
                        .inviteMessageTemplate(inviteMessageTemplate)
                        .build())
                .build());

        context.getLogger().log("UserPool updated successfully");
        responseData.put("Message", "UserPool updated successfully");
    }


    private void createAdminUser(String adminEmail, String adminUsername, String userPoolId, Map<String, Object> responseData, Context context) {

        if (adminEmail != null && !adminEmail.trim().equals("None") && !adminEmail.isEmpty()) {
            try {
                cognitoIdentityProviderClient.adminGetUser(AdminGetUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(adminEmail)
                        .build());

                context.getLogger().log("User already exists: " + adminEmail);
                responseData.put("Message", "User already exists: " + adminEmail);
            } catch (UserNotFoundException e) {
                List<AttributeType> userAttributes = new ArrayList<>();
                userAttributes.add(AttributeType.builder().name("email").value(adminEmail).build());
                userAttributes.add(AttributeType.builder().name("name").value(adminUsername).build());
                userAttributes.add(AttributeType.builder().name("custom:role").value(Role.ADMIN.toString()).build());
                userAttributes.add(AttributeType.builder().name("email_verified").value("true").build());

                AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(adminEmail)
                        .userAttributes(userAttributes)
                        .temporaryPassword(PasswordGenerator.generatePassword())
                        .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                        .build();

                cognitoIdentityProviderClient.adminCreateUser(createUserRequest);

            }
        }


    }

    private void configureNotificationTopicsSubscription(String snsTopic, String adminEmail, Context context) {
        subscribeToTopic(snsClient, context, snsTopic, adminEmail);
    }

    private void sendResponse(String url, CloudFormationCustomResourceEvent event, Context context, String status, Map<String, Object> data) {
        LambdaLogger logger = context.getLogger();
        ObjectMapper objectMapper = new ObjectMapper();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            Map<String, Object> responseBody = Map.of(
                    "Status", status,
                    "Reason", "See the details in CloudWatch Log Stream: " + context.getLogStreamName(),
                    "PhysicalResourceId", event.getPhysicalResourceId() != null ? event.getPhysicalResourceId() : context.getLogStreamName(),
                    "StackId", event.getStackId(),
                    "RequestId", event.getRequestId(),
                    "LogicalResourceId", event.getLogicalResourceId(),
                    "Data", data
            );

            try {
                StringEntity entity = new StringEntity(objectMapper.writeValueAsString(responseBody));
                HttpPut request = new HttpPut(url);
                request.setEntity(entity);
                request.setHeader("Content-Type", "application/json");

                httpClient.execute(request, response -> {
                    EntityUtils.consume(response.getEntity());
                    logger.log("Response sent to CloudFormation successfully.");
                    return null;
                });
                logger.log("Response sent to CloudFormation successfully.");
            } catch (IOException e) {
                logger.log("Failed to send response to CloudFormation: " + e.getMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
