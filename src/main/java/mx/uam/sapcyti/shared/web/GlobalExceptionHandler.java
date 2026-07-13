package mx.uam.sapcyti.shared.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import mx.uam.sapcyti.academic.domain.exception.DuplicateEmployeeNumberException;
import mx.uam.sapcyti.academic.domain.exception.DuplicateEnrollmentIdException;
import mx.uam.sapcyti.academic.domain.exception.DuplicateProfessorEmailException;
import mx.uam.sapcyti.academic.domain.exception.DuplicateStudentEmailException;
import mx.uam.sapcyti.academic.domain.exception.EmployeeNumberImmutableException;
import mx.uam.sapcyti.academic.domain.exception.InvalidTypeChangeException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorAlreadyActiveException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorAlreadyInactiveException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorHasActiveAssignmentsException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.exception.StudentProgramNotFoundException;
import mx.uam.sapcyti.offering.domain.exception.ClaveInvalidFormatException;
import mx.uam.sapcyti.offering.domain.exception.FileFormatInvalidException;
import mx.uam.sapcyti.offering.domain.exception.UeaAlreadyExistsException;
import mx.uam.sapcyti.offering.domain.exception.UeaAlreadyActiveException;
import mx.uam.sapcyti.offering.domain.exception.UeaAlreadyInactiveException;
import mx.uam.sapcyti.offering.domain.exception.UeaInActiveSurveyException;
import mx.uam.sapcyti.offering.domain.exception.UeaNotFoundException;
import mx.uam.sapcyti.planning.domain.exception.AnnualPlanAlreadyExistsException;
import mx.uam.sapcyti.planning.domain.exception.AnnualPlanEntryNotFoundException;
import mx.uam.sapcyti.planning.domain.exception.AnnualPlanNotFoundException;
import mx.uam.sapcyti.planning.domain.exception.InvalidStatusTransitionException;
import mx.uam.sapcyti.planning.domain.exception.PlanNotEditableException;
import mx.uam.sapcyti.configuration.domain.exception.ConfigurationParameterNotFoundException;
import mx.uam.sapcyti.configuration.domain.exception.DuplicateGraduateProgramNameException;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.identity.domain.exception.ExpiredResetTokenException;
import mx.uam.sapcyti.identity.domain.exception.IncorrectCurrentPasswordException;
import mx.uam.sapcyti.identity.domain.exception.InvalidCredentialsException;
import mx.uam.sapcyti.identity.domain.exception.InvalidRefreshTokenException;
import mx.uam.sapcyti.identity.domain.exception.InvalidResetTokenException;
import mx.uam.sapcyti.identity.domain.exception.PasswordChangeForbiddenException;
import mx.uam.sapcyti.identity.domain.exception.UsedResetTokenException;
import mx.uam.sapcyti.identity.domain.exception.UserNotFoundException;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.survey.domain.exception.BlankWithUeasConflictException;
import mx.uam.sapcyti.survey.domain.exception.StudentSurveyResponseNotFoundException;
import mx.uam.sapcyti.survey.domain.exception.SurveyAlreadyExistsForTermException;
import mx.uam.sapcyti.survey.domain.exception.SurveyNoActiveUeasException;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotActiveException;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotDeletableException;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotFoundException;
import mx.uam.sapcyti.survey.domain.exception.SurveyWindowOverlapException;
import mx.uam.sapcyti.survey.domain.exception.UeaNotAvailableException;

/**
 * Unified JSON error responses for REST APIs (SPEC-007).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", "Invalid request body"));
    }

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

    @ExceptionHandler(UeaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUeaNotFound(UeaNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", UeaNotFoundException.MESSAGE));
    }

    @ExceptionHandler(UeaAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUeaAlreadyExists(UeaAlreadyExistsException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(UeaAlreadyExistsException.ERROR_CODE, UeaAlreadyExistsException.MESSAGE));
    }

    @ExceptionHandler(UeaAlreadyInactiveException.class)
    public ResponseEntity<ErrorResponse> handleUeaAlreadyInactive(UeaAlreadyInactiveException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                UeaAlreadyInactiveException.ERROR_CODE,
                UeaAlreadyInactiveException.MESSAGE));
    }

    @ExceptionHandler(UeaAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleUeaAlreadyActive(UeaAlreadyActiveException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                UeaAlreadyActiveException.ERROR_CODE,
                UeaAlreadyActiveException.MESSAGE));
    }

    @ExceptionHandler(ClaveInvalidFormatException.class)
    public ResponseEntity<ErrorResponse> handleClaveInvalidFormat(ClaveInvalidFormatException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ClaveInvalidFormatException.ERROR_CODE, ClaveInvalidFormatException.MESSAGE));
    }

    @ExceptionHandler(FileFormatInvalidException.class)
    public ResponseEntity<ErrorResponse> handleFileFormatInvalid(FileFormatInvalidException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(FileFormatInvalidException.ERROR_CODE, ex.getMessage()));
    }

    @ExceptionHandler(AnnualPlanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAnnualPlanNotFound(AnnualPlanNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", AnnualPlanNotFoundException.MESSAGE));
    }

    @ExceptionHandler(AnnualPlanEntryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAnnualPlanEntryNotFound(AnnualPlanEntryNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", AnnualPlanEntryNotFoundException.MESSAGE));
    }

    @ExceptionHandler(AnnualPlanAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAnnualPlanAlreadyExists(AnnualPlanAlreadyExistsException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                AnnualPlanAlreadyExistsException.ERROR_CODE,
                AnnualPlanAlreadyExistsException.MESSAGE));
    }

    @ExceptionHandler(PlanNotEditableException.class)
    public ResponseEntity<ErrorResponse> handlePlanNotEditable(PlanNotEditableException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(PlanNotEditableException.ERROR_CODE, PlanNotEditableException.MESSAGE));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                InvalidStatusTransitionException.ERROR_CODE,
                InvalidStatusTransitionException.MESSAGE));
    }

    @ExceptionHandler(UeaInActiveSurveyException.class)
    public ResponseEntity<ErrorResponse> handleUeaInActiveSurvey(UeaInActiveSurveyException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(UeaInActiveSurveyException.ERROR_CODE, ex.getMessage()));
    }

    @ExceptionHandler(SurveyAlreadyExistsForTermException.class)
    public ResponseEntity<ErrorResponse> handleSurveyAlreadyExists(SurveyAlreadyExistsForTermException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                SurveyAlreadyExistsForTermException.ERROR_CODE,
                SurveyAlreadyExistsForTermException.MESSAGE));
    }

    @ExceptionHandler(SurveyNoActiveUeasException.class)
    public ResponseEntity<ErrorResponse> handleSurveyNoActiveUeas(SurveyNoActiveUeasException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                SurveyNoActiveUeasException.ERROR_CODE,
                SurveyNoActiveUeasException.MESSAGE));
    }

    @ExceptionHandler(SurveyWindowOverlapException.class)
    public ResponseEntity<ErrorResponse> handleSurveyWindowOverlap(SurveyWindowOverlapException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                SurveyWindowOverlapException.ERROR_CODE,
                SurveyWindowOverlapException.MESSAGE));
    }

    @ExceptionHandler(SurveyNotDeletableException.class)
    public ResponseEntity<ErrorResponse> handleSurveyNotDeletable(SurveyNotDeletableException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                SurveyNotDeletableException.ERROR_CODE,
                SurveyNotDeletableException.MESSAGE));
    }

    @ExceptionHandler(SurveyNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleSurveyNotActive(SurveyNotActiveException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                SurveyNotActiveException.ERROR_CODE,
                SurveyNotActiveException.MESSAGE));
    }

    @ExceptionHandler(SurveyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSurveyNotFound(SurveyNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(
                SurveyNotFoundException.ERROR_CODE,
                SurveyNotFoundException.MESSAGE));
    }

    @ExceptionHandler(UeaNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleUeaNotAvailable(UeaNotAvailableException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                UeaNotAvailableException.ERROR_CODE,
                UeaNotAvailableException.MESSAGE));
    }

    @ExceptionHandler(BlankWithUeasConflictException.class)
    public ResponseEntity<ErrorResponse> handleBlankWithUeasConflict(BlankWithUeasConflictException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                BlankWithUeasConflictException.ERROR_CODE,
                BlankWithUeasConflictException.MESSAGE));
    }

    @ExceptionHandler(StudentSurveyResponseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStudentSurveyResponseNotFound(
            StudentSurveyResponseNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", StudentSurveyResponseNotFoundException.MESSAGE));
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

    @ExceptionHandler(DuplicateProfessorEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProfessorEmail(
            DuplicateProfessorEmailException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("CONFLICT", DuplicateProfessorEmailException.MESSAGE));
    }

    @ExceptionHandler(DuplicateEmployeeNumberException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmployeeNumber(
            DuplicateEmployeeNumberException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                DuplicateEmployeeNumberException.ERROR_CODE,
                DuplicateEmployeeNumberException.MESSAGE));
    }

    @ExceptionHandler(EmployeeNumberImmutableException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNumberImmutable(
            EmployeeNumberImmutableException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                EmployeeNumberImmutableException.ERROR_CODE,
                EmployeeNumberImmutableException.MESSAGE));
    }

    @ExceptionHandler(InvalidTypeChangeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTypeChange(
            InvalidTypeChangeException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                InvalidTypeChangeException.ERROR_CODE,
                InvalidTypeChangeException.MESSAGE));
    }

    @ExceptionHandler(DuplicateStudentEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateStudentEmail(
            DuplicateStudentEmailException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("CONFLICT", DuplicateStudentEmailException.MESSAGE));
    }

    @ExceptionHandler(DuplicateEnrollmentIdException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEnrollmentId(
            DuplicateEnrollmentIdException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("CONFLICT", DuplicateEnrollmentIdException.MESSAGE));
    }

    @ExceptionHandler(ProfessorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProfessorNotFound(
            ProfessorNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", ProfessorNotFoundException.MESSAGE));
    }

    @ExceptionHandler(ProfessorAlreadyInactiveException.class)
    public ResponseEntity<ErrorResponse> handleProfessorAlreadyInactive(
            ProfessorAlreadyInactiveException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("CONFLICT", ProfessorAlreadyInactiveException.MESSAGE));
    }

    @ExceptionHandler(ProfessorAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleProfessorAlreadyActive(
            ProfessorAlreadyActiveException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                ProfessorAlreadyActiveException.ERROR_CODE,
                ProfessorAlreadyActiveException.MESSAGE));
    }

    @ExceptionHandler(ProfessorHasActiveAssignmentsException.class)
    public ResponseEntity<ErrorResponse> handleProfessorHasActiveAssignments(
            ProfessorHasActiveAssignmentsException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("CONFLICT", ProfessorHasActiveAssignmentsException.MESSAGE));
    }

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStudentNotFound(
            StudentNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", StudentNotFoundException.MESSAGE));
    }

    @ExceptionHandler(StudentProgramNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStudentProgramNotFound(
            StudentProgramNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", StudentProgramNotFoundException.MESSAGE));
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

    @ExceptionHandler(IncorrectCurrentPasswordException.class)
    public ResponseEntity<ErrorResponse> handleIncorrectCurrentPassword(
            IncorrectCurrentPasswordException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", IncorrectCurrentPasswordException.MESSAGE));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", UserNotFoundException.MESSAGE));
    }

    @ExceptionHandler(PasswordChangeForbiddenException.class)
    public ResponseEntity<ErrorResponse> handlePasswordChangeForbidden(
            PasswordChangeForbiddenException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("FORBIDDEN", PasswordChangeForbiddenException.MESSAGE));
    }

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTenantAccessDenied(
            TenantAccessDeniedException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("FORBIDDEN", ex.getMessage()));
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
