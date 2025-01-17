package org.umaxcode.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TaskDetailsUpdateDto(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Description is required")
        String description
) {
}
