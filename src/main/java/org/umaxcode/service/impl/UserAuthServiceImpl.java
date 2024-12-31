package org.umaxcode.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.umaxcode.domain.dto.request.UserCreationDto;
import org.umaxcode.exception.UserAuthException;
import org.umaxcode.service.UserAuthService;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final CognitoIdentityProviderClient cognitoClient;
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
                            AttributeType.builder().name("name").value(request.username()).build(),
//                            AttributeType.builder().name("role").value(Role.USER.toString()).build(),
                            AttributeType.builder().name("email_verified").value("true").build()
                    )
                    .temporaryPassword("TemporaryPass@123") // Temporary password for first login
                    .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                    .build();

            AdminCreateUserResponse response = cognitoClient.adminCreateUser(adminRequest);
            return "User created: " + response.user().username();
        } catch (CognitoIdentityProviderException e) {
            throw new UserAuthException("Failed to create user: " + e.getMessage());
        }
    }
}
