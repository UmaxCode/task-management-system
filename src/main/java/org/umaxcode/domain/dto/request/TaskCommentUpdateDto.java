package org.umaxcode.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TaskCommentUpdateDto(

        @NotBlank(message = "Comment is required")
        String comment
) {
}
