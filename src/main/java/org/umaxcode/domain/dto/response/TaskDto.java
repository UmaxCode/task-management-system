package org.umaxcode.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.umaxcode.domain.enums.TaskStatus;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskDto {
    String id;
    String name;
    String description;
    TaskStatus status;
    String deadline;
    String responsibility;
}
