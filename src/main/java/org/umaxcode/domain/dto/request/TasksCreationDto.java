package org.umaxcode.domain.dto.request;

import org.umaxcode.domain.enums.TaskStatus;

import java.time.LocalDateTime;

public record TasksCreationDto(
        String name,
        String description,
        TaskStatus status,
        LocalDateTime deadline,
        String responsibility
) {
}
