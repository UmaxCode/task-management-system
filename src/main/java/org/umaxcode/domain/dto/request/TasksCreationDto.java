package org.umaxcode.domain.dto.request;

import java.time.LocalDateTime;

public record TasksCreationDto(
        String name,
        String description,
        LocalDateTime deadline,
        String responsibility
) {
}
