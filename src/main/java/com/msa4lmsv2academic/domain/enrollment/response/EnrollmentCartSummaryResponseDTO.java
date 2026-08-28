package com.msa4lmsv2academic.domain.enrollment.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "수강 장바구니 조회 결과와 예상 신청학점 합계")
public record EnrollmentCartSummaryResponseDTO(
        @Schema(description = "조회된 장바구니 강의의 예상 신청학점 합계", example = "12") int totalCredits,
        @Schema(description = "장바구니 항목. 없으면 빈 목록") List<EnrollmentCartItemResponseDTO> items
) {

    public static EnrollmentCartSummaryResponseDTO from(List<EnrollmentCartItemResponseDTO> items) {
        int totalCredits = items.stream().mapToInt(item -> item.credits()).sum();
        return new EnrollmentCartSummaryResponseDTO(totalCredits, List.copyOf(items));
    }
}
