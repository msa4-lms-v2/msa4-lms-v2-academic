package com.msa4lmsv2academic.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CustomResponseCode {

    SUCCESS("00", HttpStatus.OK),

    UNAUTHENTICATED("E02", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("E03", HttpStatus.FORBIDDEN),
    INVALID_TOKEN("E04", HttpStatus.UNAUTHORIZED),

    NOT_FOUND_DATA("E10", HttpStatus.NOT_FOUND),
    DUPLICATE_DATA("E11", HttpStatus.CONFLICT),

    INVALID_PARAMETER("E21", HttpStatus.BAD_REQUEST),

    DATABASE_ERROR("E80", HttpStatus.INTERNAL_SERVER_ERROR),
    SYSTEM_ERROR("E99", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final HttpStatus httpStatus;

    CustomResponseCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
