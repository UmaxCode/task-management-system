package org.umaxcode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.umaxcode.domain.dto.request.UserCreationDto;
import org.umaxcode.exception.UserAuthException;
import org.umaxcode.service.UserAuthService;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final LambdaClient lambdaClient;

    @Value("${application.aws.userPoolId}")
    private String userPoolId;

    @Override
    public String register(UserCreationDto request) {

        try {
            AdminCreateUserRequest adminRequest = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(request.email())
                    .userAttributes(
                            AttributeType.builder().name("email").value(request.email()).build(),
//                            AttributeType.builder().name("custom:name").value(request.username()).build(),
//                            AttributeType.builder().name("custom:role").value(Role.USER.toString()).build(),
                            AttributeType.builder().name("email_verified").value("true").build()
                    )
                    .temporaryPassword("TemporaryPass@123") // Temporary password for first login
                    .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                    .build();

            AdminCreateUserResponse response = cognitoClient.adminCreateUser(adminRequest);
            invokePostConfirmationLambda(request.email());
            return "User created: " + response.user().username();
        } catch (CognitoIdentityProviderException e) {
            throw new UserAuthException("Failed to create user: " + e.getMessage());
        }
    }

    private void invokePostConfirmationLambda(String email) {
        try {
            // Trigger the Lambda function manually (passing username as input)

            String postLambdaArn = System.getenv("POST_CONFIRMATION_LAMBDA_ARN");
            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(postLambdaArn)
                    .payload(SdkBytes.fromUtf8String("{\"email\": \"" + email + "\"}"))
                    .build();

            lambdaClient.invoke(invokeRequest);

            System.out.println("Post-confirmation Lambda triggered for: " + email);

        } catch (Exception e) {
            System.err.println("Error invoking post-confirmation Lambda: " + e.getMessage());
        }
    }
}
