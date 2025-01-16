package org.umaxcode.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.umaxcode.domain.dto.response.UserDto;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMapper {

    public static List<UserDto> toUserDto(List<UserType> users) {

        return users.stream().map(
                u -> UserDto.builder()
                        .userId(u.username())
                        .email(u.attributes().stream()
                                .filter(a -> "email".equalsIgnoreCase(a.name()))
                                .map(AttributeType::value)
                                .findFirst().orElse(null))
                        .username(u.attributes().stream()
                                .filter(a -> "name".equalsIgnoreCase(a.name()))
                                .map(AttributeType::value)
                                .findFirst().orElse(null))
                        .role(u.attributes().stream()
                                .filter(a -> "custom:role".equalsIgnoreCase(a.name()))
                                .map(AttributeType::value)
                                .findFirst().orElse(null))
                        .build()
        ).toList();
    }
}
