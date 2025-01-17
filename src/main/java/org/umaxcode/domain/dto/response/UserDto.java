package org.umaxcode.domain.dto.response;

import lombok.Builder;

@Builder
public record UserDto(
        String userId,
        String username,
        String email,
        String role
) {
}
