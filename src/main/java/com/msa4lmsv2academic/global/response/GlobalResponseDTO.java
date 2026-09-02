package com.msa4lmsv2academic.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 응답 포맷")
public record GlobalResponseDTO<T>(
        @Schema(description = "응답 코드", example = "00") String code,
        @Schema(description = "응답 메시지") String message,
        @Schema(description = "실제 응답 데이터") T data
) {

    public static <T> GlobalResponseDTO<T> success(T data) {
        return from(CustomResponseCode.SUCCESS, data);
    }

    public static GlobalResponseDTO<Void> success() {
        return from(CustomResponseCode.SUCCESS, null);
    }

    public static <T> GlobalResponseDTO<T> fail(CustomResponseCode code, T data) {
        return from(code, data);
    }

    public static <T> GlobalResponseDTO<T> fail(CustomResponseCode code, String message, T data) {
        return new GlobalResponseDTO<>(code.getCode(), message, data);
    }

    private static <T> GlobalResponseDTO<T> from(CustomResponseCode code, T data) {
        return new GlobalResponseDTO<>(code.getCode(), code.getMessage(), data);
    }
}
