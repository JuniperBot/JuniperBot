/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.web.controller.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.juniperbot.common.model.exception.AccessDeniedException;
import ru.juniperbot.common.model.exception.NotFoundException;
import ru.caramel.juniperbot.web.dto.ErrorDetailsDto;
import ru.caramel.juniperbot.web.dto.validation.ValidationErrorDto;

@Slf4j
@RestControllerAdvice
public class ResponseEntityExceptionHandlerEx extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public final ResponseEntity handleNotFound(NotFoundException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public final ResponseEntity handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ErrorDetailsDto> handleAllExceptions(Exception e) {
        return errorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {
        ValidationErrorDto dto = new ValidationErrorDto();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String localizedErrorMessage = fieldError.getDefaultMessage();
            String[] fieldErrorCodes = fieldError.getCodes();
            if (fieldErrorCodes != null) {
                localizedErrorMessage = fieldErrorCodes[0];
            }
            dto.addFieldError(fieldError.getField(), localizedErrorMessage);
        }
        return ResponseEntity.badRequest().body(dto);
    }

    protected ResponseEntity<ErrorDetailsDto> errorResponse(Exception e, HttpStatus status) {
        if (e != null) {
            log.error("API error caught: " + e.getMessage(), e);
            return response(new ErrorDetailsDto(e), status);
        } else {
            log.error("Unknown API error caught, {}", status);
            return response(null, status);
        }
    }

    private <T> ResponseEntity<T> response(T body, HttpStatus status) {
        return new ResponseEntity<>(body, new HttpHeaders(), status);
    }
}
