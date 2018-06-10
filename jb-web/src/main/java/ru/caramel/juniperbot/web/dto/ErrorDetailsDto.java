package ru.caramel.juniperbot.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Serializable;

@Getter
public class ErrorDetailsDto implements Serializable {

    private final String error;

    @JsonProperty("error_description")
    private final String description;

    @JsonProperty("stack_trace")
    private final String stackTrace;

    public ErrorDetailsDto(Exception e) {
        this.error = e.getClass().getName();
        this.description = e.getMessage();
        this.stackTrace = ExceptionUtils.getStackTrace(e);
    }
}
