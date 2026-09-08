package com.msa4lmsv2academic.domain.evaluation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "강의평가 제출 요청")
public record LectureEvaluationSubmitRequestDTO(
        @Schema(description = "평가할 수강 내역 ID", example = "301")
        @NotNull(message = "enrollmentId는 필수입니다.")
        @Positive(message = "enrollmentId는 양수여야 합니다.")
        Long enrollmentId,

        @Schema(
                description = "문항 코드별 1~5점 평가. 문항은 최대 20개까지 제출할 수 있습니다.",
                example = "{\"CONTENT_QUALITY\":5,\"DELIVERY_CLARITY\":4,\"FAIR_GRADING\":5}"
        )
        @NotEmpty(message = "ratings는 한 문항 이상 입력해야 합니다.")
        @Size(max = 20, message = "ratings는 최대 20개 문항까지 입력할 수 있습니다.")
        Map<
                @NotBlank(message = "평가 문항 코드는 비어 있을 수 없습니다.")
                @Size(max = 100, message = "평가 문항 코드는 100자 이하여야 합니다.") String,
                @NotNull(message = "평가 점수는 필수입니다.")
                @Min(value = 1, message = "평가 점수는 1점 이상이어야 합니다.")
                @Max(value = 5, message = "평가 점수는 5점 이하여야 합니다.") Integer
                > ratings,

        @Schema(description = "서술형 의견", example = "실습 예제가 이해에 도움이 되었습니다.", maxLength = 2000)
        @Size(max = 2000, message = "comment는 2000자 이하여야 합니다.")
        String comment
) {
}
