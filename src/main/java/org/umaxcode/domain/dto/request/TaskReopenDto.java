package org.umaxcode.domain.dto.request;

import java.time.LocalDateTime;

public record TaskReopenDto(
        LocalDateTime deadline
) {
}
