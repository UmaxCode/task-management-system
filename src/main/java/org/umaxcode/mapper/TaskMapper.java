package org.umaxcode.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.umaxcode.domain.dto.response.TaskDto;
import org.umaxcode.domain.enums.TaskStatus;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskMapper {


    public static TaskDto mapToTaskDto(Map<String, AttributeValue> item) {

        AttributeValue comment = item.get("comment");
        return TaskDto.builder()
                .id(item.get("taskId").s())
                .name(item.get("name").s())
                .description(item.get("description").s())
                .status(TaskStatus.fromValue(item.get("status").s()))
                .deadline(item.get("deadline").s())
                .responsibility(item.get("responsibility").s())
                .comment(comment != null ? comment.s() : null)
                .build();
    }

    public static List<TaskDto> mapToListTaskDto(List<Map<String, AttributeValue>> items) {

        return items.stream()
                .map(TaskMapper::mapToTaskDto)
                .toList();
    }
}
