package org.umaxcode.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.umaxcode.domain.dto.request.UserCreationDto;
import org.umaxcode.domain.dto.response.UserDto;
import org.umaxcode.domain.enums.Role;
import org.umaxcode.exception.UserAuthException;
import org.umaxcode.mapper.UserMapper;
import org.umaxcode.service.UserAuthService;
import org.umaxcode.utils.PasswordGenerator;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final SfnClient stepFunctionsClient;

    @Value("${application.aws.userPoolId}")
    private String userPoolId;

    @Override
    public UserDto register(UserCreationDto request) {

        try {
            AdminCreateUserRequest adminRequest = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(request.email())
                    .userAttributes(
                            AttributeType.builder().name("email").value(request.email()).build(),
                            AttributeType.builder().name("name").value(request.username()).build(),
                            AttributeType.builder().name("custom:role").value(Role.USER.toString()).build(),
                            AttributeType.builder().name("email_verified").value("true").build()
                    )
                    .temporaryPassword(PasswordGenerator.generatePassword()) // Temporary password for first login
                    .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                    .build();

            AdminCreateUserResponse response = cognitoClient.adminCreateUser(adminRequest);
            startStateMachineForSNSSub(request.email());
            System.out.println("User" + response.user());

            return UserDto.builder()
                    .userId(request.username())
                    .email(request.email())
                    .username(request.username())
                    .role(Role.USER.toString())
                    .build();

        } catch (CognitoIdentityProviderException e) {
            throw new UserAuthException("Failed to create user: " + e.getMessage());
        }
    }

    private void startStateMachineForSNSSub(String email) {

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

            stepFunctionsClient.startExecution(startExecutionRequest);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<UserDto> fetchAllUsers() {

        ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                .userPoolId(userPoolId)
                .build();

        ListUsersResponse response = cognitoClient.listUsers(listUsersRequest);

        // Handle pagination if needed
        List<UserType> allUsers = response.users();
        String paginationToken = response.paginationToken();

        while (paginationToken != null) {
            response = cognitoClient.listUsers(
                    ListUsersRequest.builder()
                            .userPoolId(userPoolId)
                            .paginationToken(paginationToken)
                            .build()
            );
            allUsers.addAll(response.users());
            paginationToken = response.paginationToken();
        }

        return UserMapper.toUserDto(allUsers);
    }
}
