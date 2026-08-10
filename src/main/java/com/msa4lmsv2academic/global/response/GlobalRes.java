package com.msa4lmsv2academic.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 응답 포맷")
public record GlobalRes<T>(
        @Schema(description = "응답 코드", example = "00") String code,
        @Schema(description = "응답 메시지") String message,
        @Schema(description = "실제 응답 데이터") T data
) {

    public static <T> GlobalRes<T> success(T data) {
        return new GlobalRes<>(CustomResponseCode.SUCCESS.getCode(), "정상 처리되었습니다.", data);
    }

    public static GlobalRes<Void> success() {
        return new GlobalRes<>(CustomResponseCode.SUCCESS.getCode(), "정상 처리되었습니다.", null);
    }

    public static <T> GlobalRes<T> fail(CustomResponseCode code, String message, T data) {
        return new GlobalRes<>(code.getCode(), message, data);
    }
}
