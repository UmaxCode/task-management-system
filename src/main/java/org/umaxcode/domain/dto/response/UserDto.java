package org.umaxcode.domain.dto.response;

import lombok.Builder;

@Builder
public record UserDto(
        String userId,
        String email
) {
}
