package org.umaxcode.domain.dto.request;

import org.umaxcode.domain.enums.TaskStatus;

import java.time.LocalDate;

public record TasksCreationDto(
        String name,
        String description,
        TaskStatus status,
        LocalDate deadline,
        String responsibility
) {
}
