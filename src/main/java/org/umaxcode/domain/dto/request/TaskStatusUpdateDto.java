package org.umaxcode.domain.dto.request;

import org.umaxcode.domain.enums.TaskStatus;

public record TaskStatusUpdateDto(
        TaskStatus status
) {
}
