package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCart;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "수강 장바구니 추가 결과")
public record EnrollmentCartCreateResponseDTO(
        @Schema(description = "생성된 장바구니 항목 ID", example = "301") Long cartItemId,
        @Schema(description = "본인 학생 ID (users.id와 구분)", example = "1") Long studentId,
        @Schema(description = "개설 강의 ID", example = "20") Long lectureId,
        @Schema(description = "장바구니 추가 시각", example = "2026-08-27T10:30:00") LocalDateTime createdAt
) {

    public static EnrollmentCartCreateResponseDTO from(EnrollmentCart cart) {
        return new EnrollmentCartCreateResponseDTO(
                cart.getId(),
                cart.getStudent().getId(),
                cart.getLecture().getId(),
                cart.getCreatedAt()
        );
    }
}
