package com.msa4lmsv2academic.domain.lecture.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 강의 개설 승인·반려")
public record LectureOpeningReviewRequestDTO(
        @Schema(description = "강의 개설 신청 ID", example = "101")
        @NotNull(message = "강의 개설 신청 ID는 필수입니다.")
        @Positive(message = "강의 개설 신청 ID는 양수여야 합니다.")
        Long openingRequestId,

        @Schema(description = "승인 여부", example = "true")
        @NotNull(message = "승인 여부는 필수입니다.")
        Boolean approved,

        @Schema(description = "반려 사유. 반려 시 필수입니다.", example = "강의 시간과 강의실을 다시 확인해 주세요.")
        @Size(max = 500, message = "반려 사유는 500자 이하여야 합니다.")
        String rejectReason,

        @Schema(description = "승인 전 보정 정보. 보정하지 않으면 생략합니다.")
        @Valid
        LectureOpeningCorrectionRequestDTO correction
) {

    @Schema(hidden = true)
    @AssertTrue(message = "반려 시 반려 사유는 필수입니다.")
    public boolean isRejectReasonValid() {
        return approved == null || approved || (rejectReason != null && !rejectReason.isBlank());
    }

    @Schema(hidden = true)
    @AssertTrue(message = "개설 정보 보정은 승인 요청에서만 사용할 수 있습니다.")
    public boolean isCorrectionUsageValid() {
        return correction == null || Boolean.TRUE.equals(approved);
    }
}
