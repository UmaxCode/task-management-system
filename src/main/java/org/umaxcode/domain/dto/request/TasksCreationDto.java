package org.umaxcode.domain.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record TasksCreationDto(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Deadline is required")
        @Future(message = "Deadline must be in the future")
        LocalDateTime deadline,

        @NotBlank(message = "Deadline is required")
        @Email(message = "Invalid email")
        String responsibility
) {
}
