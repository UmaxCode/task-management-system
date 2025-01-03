package org.umaxcode.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TaskStatus {

    COMPLETED("completed"),
    OPEN("open"),
    EXPIRED("expired");

    @JsonValue
    private final String name;

    @JsonCreator
    public static TaskStatus fromValue(String value) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.name.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
