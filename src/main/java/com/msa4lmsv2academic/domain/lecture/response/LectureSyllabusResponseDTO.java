package com.msa4lmsv2academic.domain.lecture.response;

import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강의계획서 조회·저장 결과")
public record LectureSyllabusResponseDTO(
        @Schema(description = "강의 ID", example = "101")
        Long classId,

        @Schema(description = "강의계획서 본문", nullable = true)
        String syllabus,

        @Schema(description = "강의 상태", example = "OPEN")
        LectureStatus status
) {

    public static LectureSyllabusResponseDTO from(Lecture lecture) {
        return new LectureSyllabusResponseDTO(
                lecture.getId(),
                lecture.getSyllabus(),
                lecture.getStatus()
        );
    }
}
