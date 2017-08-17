package ru.caramel.juniperbot.web.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import ru.caramel.juniperbot.model.exception.AccessDeniedException;
import ru.caramel.juniperbot.model.exception.NotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleExceptiond(NoHandlerFoundException ex) {
        return "error404";
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleExceptiond(NotFoundException ex) {
        return "error404";
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleExceptiond(AccessDeniedException ex) {
        return "error403";
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleExceptiond(Exception ex) {
        return "error500";
    }
}
