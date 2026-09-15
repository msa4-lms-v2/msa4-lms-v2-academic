package com.msa4lmsv2academic.domain;

import com.msa4lmsv2academic.domain.counseling.entity.Counseling;
import com.msa4lmsv2academic.domain.counseling.response.CounselingResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.grade.response.GradeItemResponseDTO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class WorkflowIdentityResponseTest {
    @Test void 상담과_성적은_PK와_다른_저장된_학번을_반환한다() {
        var counseling = mock(Counseling.class, RETURNS_DEEP_STUBS);
        when(counseling.getStudent().getId()).thenReturn(2200198L);
        when(counseling.getStudent().getStudentNumber()).thenReturn("25010198");
        assertEquals("25010198", CounselingResponseDTO.from(counseling).studentNumber());
        var enrollment = mock(Enrollment.class, RETURNS_DEEP_STUBS);
        when(enrollment.getStudent().getId()).thenReturn(2200198L);
        when(enrollment.getStudent().getStudentNumber()).thenReturn("25010198");
        assertEquals("25010198", GradeItemResponseDTO.from(enrollment).studentNumber());
    }
}
