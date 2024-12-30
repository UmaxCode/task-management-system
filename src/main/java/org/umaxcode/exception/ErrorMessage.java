package org.umaxcode.exception;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class ErrorMessage {

    private String path;
    private String message;
    private LocalDateTime timestamp;
}
