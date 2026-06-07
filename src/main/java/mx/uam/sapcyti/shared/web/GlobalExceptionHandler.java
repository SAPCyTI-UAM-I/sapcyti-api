package mx.uam.sapcyti.shared.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import mx.uam.sapcyti.configuration.domain.exception.ConfigurationParameterNotFoundException;
import mx.uam.sapcyti.configuration.domain.exception.DuplicateGraduateProgramNameException;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.identity.domain.exception.ExpiredResetTokenException;
import mx.uam.sapcyti.identity.domain.exception.InvalidCredentialsException;
import mx.uam.sapcyti.identity.domain.exception.InvalidRefreshTokenException;
import mx.uam.sapcyti.identity.domain.exception.InvalidResetTokenException;
import mx.uam.sapcyti.identity.domain.exception.UsedResetTokenException;

/**
 * Unified JSON error responses for REST APIs (SPEC-007).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse("Validation failed");
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
            .findFirst()
            .map(v -> v.getMessage())
            .orElse("Validation failed");
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(GraduateProgramNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProgramNotFound(
            GraduateProgramNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(
                "NOT_FOUND",
                GraduateProgramNotFoundException.MESSAGE));
    }

    @ExceptionHandler(ConfigurationParameterNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleParameterNotFound(
            ConfigurationParameterNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(
                "NOT_FOUND",
                ConfigurationParameterNotFoundException.MESSAGE));
    }

    @ExceptionHandler(DuplicateGraduateProgramNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateName(
            DuplicateGraduateProgramNameException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                "CONFLICT",
                DuplicateGraduateProgramNameException.MESSAGE));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("UNAUTHORIZED", InvalidCredentialsException.MESSAGE));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("UNAUTHORIZED", InvalidRefreshTokenException.MESSAGE));
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidResetToken(
            InvalidResetTokenException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_TOKEN", InvalidResetTokenException.MESSAGE));
    }

    @ExceptionHandler(ExpiredResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleExpiredResetToken(
            ExpiredResetTokenException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("EXPIRED_TOKEN", ExpiredResetTokenException.MESSAGE));
    }

    @ExceptionHandler(UsedResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleUsedResetToken(
            UsedResetTokenException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("TOKEN_USED", UsedResetTokenException.MESSAGE));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("FORBIDDEN", "Access denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        LOG.error("Unhandled exception", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred"));
    }
}
