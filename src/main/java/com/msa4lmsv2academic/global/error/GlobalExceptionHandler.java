package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentApplicationErrorResponseDTO;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EnrollmentApplicationRejectedException.class)
    public ResponseEntity<GlobalResponseDTO<EnrollmentApplicationErrorResponseDTO>> handleEnrollmentApplicationRejected(
            EnrollmentApplicationRejectedException exception
    ) {
        CustomResponseCode code = exception.getCode();
        log.warn("[{}] {}", code.getCode(), exception.getMessage());
        return ResponseEntity.status(code.getHttpStatus()).body(GlobalResponseDTO.fail(code,
                EnrollmentApplicationErrorResponseDTO.from(exception.getReasons())));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleBusinessException(BusinessException exception) {
        CustomResponseCode code = exception.getCode();
        if (code.getHttpStatus().is5xxServerError()) {
            log.error("[{}] {}", code.getCode(), exception.getMessage(), exception);
        } else {
            log.warn("[{}] {}", code.getCode(), exception.getMessage());
        }
        return fail(code);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(", "));
        log.warn("[{}] {}", CustomResponseCode.INVALID_PARAMETER.getCode(), message);
        return fail(CustomResponseCode.INVALID_PARAMETER);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .distinct()
                .collect(Collectors.joining(", "));
        log.warn("[{}] {}", CustomResponseCode.INVALID_PARAMETER.getCode(), message);
        return fail(CustomResponseCode.INVALID_PARAMETER);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<GlobalResponseDTO<Void>> handleMalformedRequest(Exception exception) {
        log.warn("[{}] {}", CustomResponseCode.INVALID_PARAMETER.getCode(), exception.getMessage());
        return fail(CustomResponseCode.INVALID_PARAMETER);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleAccessDenied(AccessDeniedException exception) {
        log.warn("[{}] {}", CustomResponseCode.ACCESS_DENIED.getCode(), exception.getMessage());
        return fail(CustomResponseCode.ACCESS_DENIED);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        log.warn("[{}] {}", CustomResponseCode.FILE_SIZE_EXCEEDED.getCode(), exception.getMessage());
        return fail(CustomResponseCode.FILE_SIZE_EXCEEDED);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException exception
    ) {
        log.warn("[{}] {}", CustomResponseCode.DUPLICATE_DATA.getCode(), exception.getMessage());
        return fail(CustomResponseCode.DUPLICATE_DATA);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleDataAccess(DataAccessException exception) {
        log.error("[{}] {}", CustomResponseCode.DATABASE_ERROR.getCode(), exception.getMessage(), exception);
        return fail(CustomResponseCode.DATABASE_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponseDTO<Void>> handleException(Exception exception) {
        log.error("[{}] {}", CustomResponseCode.SYSTEM_ERROR.getCode(), exception.getMessage(), exception);
        return fail(CustomResponseCode.SYSTEM_ERROR);
    }

    private ResponseEntity<GlobalResponseDTO<Void>> fail(CustomResponseCode code) {
        return ResponseEntity.status(code.getHttpStatus())
                .body(GlobalResponseDTO.fail(code, null));
    }
}
