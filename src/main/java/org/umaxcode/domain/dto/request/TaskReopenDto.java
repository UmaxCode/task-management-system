package org.umaxcode.domain.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record TaskReopenDto(

        @NotBlank(message = "Deadline is required")
        @Future(message = "Deadline must be in the future")
        LocalDateTime deadline
) {
}
