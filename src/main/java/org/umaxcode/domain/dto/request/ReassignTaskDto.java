package org.umaxcode.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ReassignTaskDto(

        @NotBlank(message = "User email is required")
        @Email(message = "Invalid email")
        String userEmail
) {
}
