package ru.caramel.juniperbot.web.controller.api.base;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.caramel.juniperbot.web.dto.ErrorDetailsDto;

@RestControllerAdvice
public class ResponseEntityExceptionHandlerEx extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ErrorDetailsDto> handleAllExceptions(Exception e) {
        return new ResponseEntity<>(new ErrorDetailsDto(e), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
