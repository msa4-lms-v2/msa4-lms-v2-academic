package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;
import com.msa4lmsv2academic.global.response.GlobalRes;
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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GlobalRes<Void>> handleBusinessException(BusinessException exception) {
        CustomResponseCode code = exception.getCode();
        if (code.getHttpStatus().is5xxServerError()) {
            log.error("[{}] {}", code.getCode(), exception.getMessage(), exception);
        } else {
            log.warn("[{}] {}", code.getCode(), exception.getMessage());
        }
        return fail(code, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalRes<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(", "));
        return invalidParameter(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GlobalRes<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .distinct()
                .collect(Collectors.joining(", "));
        return invalidParameter(message);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<GlobalRes<Void>> handleMalformedRequest(Exception exception) {
        log.warn("[{}] {}", CustomResponseCode.INVALID_PARAMETER.getCode(), exception.getMessage());
        return fail(CustomResponseCode.INVALID_PARAMETER, "요청 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalRes<Void>> handleAccessDenied(AccessDeniedException exception) {
        log.warn("[{}] {}", CustomResponseCode.ACCESS_DENIED.getCode(), exception.getMessage());
        return fail(CustomResponseCode.ACCESS_DENIED, "접근 권한이 없습니다.");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<GlobalRes<Void>> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException exception
    ) {
        log.warn("[{}] {}", CustomResponseCode.DUPLICATE_DATA.getCode(), exception.getMessage());
        return fail(CustomResponseCode.DUPLICATE_DATA, "다른 요청에서 먼저 수정했습니다. 최신 정보를 조회한 뒤 다시 시도해 주세요.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<GlobalRes<Void>> handleDataAccess(DataAccessException exception) {
        log.error("[{}] {}", CustomResponseCode.DATABASE_ERROR.getCode(), exception.getMessage(), exception);
        return fail(CustomResponseCode.DATABASE_ERROR, "데이터 처리 중 오류가 발생했습니다.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalRes<Void>> handleException(Exception exception) {
        log.error("[{}] {}", CustomResponseCode.SYSTEM_ERROR.getCode(), exception.getMessage(), exception);
        return fail(CustomResponseCode.SYSTEM_ERROR, "일시적인 오류가 발생했습니다.");
    }

    private ResponseEntity<GlobalRes<Void>> invalidParameter(String message) {
        String responseMessage = message == null || message.isBlank()
                ? "요청 값이 올바르지 않습니다."
                : message;
        log.warn("[{}] {}", CustomResponseCode.INVALID_PARAMETER.getCode(), responseMessage);
        return fail(CustomResponseCode.INVALID_PARAMETER, responseMessage);
    }

    private ResponseEntity<GlobalRes<Void>> fail(CustomResponseCode code, String message) {
        return ResponseEntity.status(code.getHttpStatus())
                .body(GlobalRes.fail(code, message, null));
    }
}
