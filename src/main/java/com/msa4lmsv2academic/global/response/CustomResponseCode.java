package com.msa4lmsv2academic.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CustomResponseCode {

    SUCCESS("00", HttpStatus.OK, "정상 처리되었습니다."),

    UNAUTHENTICATED("E02", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED("E03", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_TOKEN("E04", HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."),

    NOT_FOUND_DATA("E10", HttpStatus.NOT_FOUND, "요청한 데이터를 찾을 수 없습니다."),
    DUPLICATE_DATA("E11", HttpStatus.CONFLICT, "이미 처리되었거나 현재 상태와 충돌합니다."),

    INVALID_PARAMETER("E21", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),

    DATABASE_ERROR("E80", HttpStatus.INTERNAL_SERVER_ERROR, "데이터 처리 중 오류가 발생했습니다."),
    SYSTEM_ERROR("E99", HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    CustomResponseCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
