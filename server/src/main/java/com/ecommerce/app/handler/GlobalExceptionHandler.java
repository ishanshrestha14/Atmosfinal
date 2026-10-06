package com.ecommerce.app.handler;

import com.ecommerce.app.handler.exceptions.InsufficientStockException;
import com.ecommerce.app.handler.exceptions.InvalidTokenException;
import com.ecommerce.app.handler.exceptions.NotMatchingPasswordsException;
import com.ecommerce.app.handler.exceptions.ResourceNotFoundException;
import com.ecommerce.app.handler.exceptions.WrongOldPasswordException;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Objects;

/**
 * Maps exceptions to RFC 7807 problem details. Extending {@link ResponseEntityExceptionHandler}
 * gives Spring MVC's own errors (malformed JSON, unsupported method, ...) the same shape.
 * <p>
 * Extension properties: {@code errors} lists validation messages, {@code errorCode} carries the
 * stable {@link ErrorCodes} value for authentication and account errors.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        return validationProblem(e.getBindingResult().getAllErrors(), headers, status, request, e);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException e,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        return validationProblem(e.getAllErrors(), headers, status, request, e);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials() {
        return coded(ErrorCodes.BAD_CREDENTIALS);
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail handleLocked() {
        return coded(ErrorCodes.ACCOUNT_LOCKED);
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled() {
        return coded(ErrorCodes.ACCOUNT_DISABLED);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException e) {
        ProblemDetail problem = coded(ErrorCodes.INVALID_TOKEN);
        problem.setDetail(e.getMessage());
        return problem;
    }

    @ExceptionHandler(NotMatchingPasswordsException.class)
    public ProblemDetail handleNotMatchingPasswords() {
        return coded(ErrorCodes.NOT_MATCHING_PASSWORDS);
    }

    @ExceptionHandler(WrongOldPasswordException.class)
    public ProblemDetail handleWrongOldPassword() {
        return coded(ErrorCodes.WRONG_OLD_PASSWORD);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String cause = Objects.toString(e.getMostSpecificCause().getMessage(), "");
        if (cause.contains("username")) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The username you provided is already in use.");
        }
        if (cause.contains("email")) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The email address you provided is already registered.");
        }
        log.warn("Data integrity violation", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The request conflicts with existing data.");
    }

    @ExceptionHandler(MessagingException.class)
    public ProblemDetail handleMessaging(MessagingException e) {
        log.error("Failed to send email", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "The email could not be sent.");
    }

    private static ProblemDetail coded(ErrorCodes code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.getHttpStatus(), code.getDescription());
        problem.setProperty("errorCode", code.getCode());
        return problem;
    }

    private ResponseEntity<Object> validationProblem(List<? extends MessageSourceResolvable> violations, HttpHeaders headers,
                                                     HttpStatusCode status, WebRequest request, Exception e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "The request is invalid.");
        problem.setProperty("errors", violations.stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .distinct()
                .sorted()
                .toList());
        return handleExceptionInternal(e, problem, headers, status, request);
    }
}
