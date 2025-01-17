package org.umaxcode.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record TasksCreationDto(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Description is required")
        String description,

        @NotBlank(message = "Deadline is required")
        @Future(message = "Deadline must be in the future")
        LocalDateTime deadline,

        @NotBlank(message = "Deadline is required")
        @Email(message = "Invalid email")
        String responsibility
) {
}
