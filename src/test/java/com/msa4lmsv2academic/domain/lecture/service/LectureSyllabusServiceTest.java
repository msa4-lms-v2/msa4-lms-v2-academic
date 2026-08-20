package com.msa4lmsv2academic.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.lecture.repository.LectureRepository;
import com.msa4lmsv2academic.domain.lecture.request.LectureSyllabusUpdateRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.LectureSyllabusResponseDTO;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.global.error.LectureSyllabusAccessDeniedException;
import com.msa4lmsv2academic.global.error.LectureSyllabusConflictException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LectureSyllabusServiceTest {

    private static final Long PROFESSOR_USER_ID = 9101L;
    private static final CurrentUser PROFESSOR = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");

    private LectureRepository lectureRepository;
    private AuditLogService auditLogService;
    private LectureSyllabusService service;
    private Lecture openLecture;

    @BeforeEach
    void setUp() {
        lectureRepository = mock(LectureRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new LectureSyllabusService(lectureRepository, auditLogService);
        openLecture = lecture(LectureStatus.OPEN, null, "01", 101L);
    }

    @Test
    void professorUpdatesOwnSyllabusAndRecordsAudit() {
        when(lectureRepository.findSyllabusByIdForUpdate(101L)).thenReturn(Optional.of(openLecture));
        when(lectureRepository.saveAndFlush(openLecture)).thenReturn(openLecture);

        LectureSyllabusResponseDTO response = service.update(
                101L,
                new LectureSyllabusUpdateRequestDTO("  주차별 강의계획  "),
                PROFESSOR,
                "request-1",
                "127.0.0.1"
        );

        assertThat(response.classId()).isEqualTo(101L);
        assertThat(response.syllabus()).isEqualTo("주차별 강의계획");
        verify(auditLogService).record(
                eq(PROFESSOR_USER_ID),
                eq("LECTURE_SYLLABUS_CREATED"),
                eq("LECTURE"),
                eq(101L),
                any(),
                any(),
                isNull(),
                eq("request-1"),
                eq("127.0.0.1")
        );
    }

    @Test
    void identicalPutIsIdempotent() {
        Lecture lecture = lecture(LectureStatus.OPEN, "동일한 계획서", "01", 101L);
        when(lectureRepository.findSyllabusByIdForUpdate(101L)).thenReturn(Optional.of(lecture));

        LectureSyllabusResponseDTO response = service.update(
                101L,
                new LectureSyllabusUpdateRequestDTO("동일한 계획서"),
                PROFESSOR,
                null,
                null
        );

        assertThat(response.syllabus()).isEqualTo("동일한 계획서");
        verify(lectureRepository, never()).saveAndFlush(any(Lecture.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void adminCanReadButCannotUpdateSyllabus() {
        when(lectureRepository.findSyllabusById(101L)).thenReturn(Optional.of(openLecture));
        CurrentUser admin = new CurrentUser(9201L, "ADMIN");

        assertThat(service.get(101L, admin).classId()).isEqualTo(101L);
        assertThatThrownBy(() -> service.update(
                101L,
                new LectureSyllabusUpdateRequestDTO("관리자 수정"),
                admin,
                null,
                null
        )).isInstanceOf(LectureSyllabusAccessDeniedException.class);
    }

    @Test
    void professorCannotReadAnotherProfessorsSyllabus() {
        when(lectureRepository.findSyllabusById(101L)).thenReturn(Optional.of(openLecture));

        assertThatThrownBy(() -> service.get(101L, new CurrentUser(9999L, "PROFESSOR")))
                .isInstanceOf(LectureSyllabusAccessDeniedException.class);
    }

    @Test
    void closedLectureCannotBeUpdated() {
        Lecture closedLecture = lecture(LectureStatus.CLOSED, "마감된 계획서", "02", 102L);
        when(lectureRepository.findSyllabusByIdForUpdate(102L)).thenReturn(Optional.of(closedLecture));

        assertThatThrownBy(() -> service.update(
                102L,
                new LectureSyllabusUpdateRequestDTO("변경 시도"),
                PROFESSOR,
                null,
                null
        )).isInstanceOf(LectureSyllabusConflictException.class);
    }

    private Lecture lecture(LectureStatus status, String syllabus, String sectionNo, Long lectureId) {
        Department department = Department.create("SYL-DEPT", null, "컴퓨터공학과", true);
        ReflectionTestUtils.setField(department, "id", 11L);
        User professorUser = User.synchronize(
                PROFESSOR_USER_ID,
                "담당교수",
                null,
                null,
                null,
                UserRole.PROFESSOR,
                UserStatus.ACTIVE
        );
        Professor professor = Professor.create(professorUser, (short) 2020, department);
        ReflectionTestUtils.setField(professor, "id", 21L);
        Course course = Course.create(
                department,
                "SYL101",
                "강의계획서 실습",
                (byte) 3,
                (byte) 1,
                CompletionType.MAJOR_REQUIRED
        );
        Semester semester = Semester.create(
                (short) 2026,
                SemesterTerm.FIRST,
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 6, 19),
                LocalDateTime.of(2026, 2, 1, 9, 0),
                LocalDateTime.of(2026, 2, 7, 18, 0),
                false
        );
        Lecture lecture = Lecture.create(
                semester,
                course,
                professor,
                sectionNo,
                40,
                "공학관 301호",
                status,
                30,
                30,
                30,
                10,
                syllabus
        );
        ReflectionTestUtils.setField(lecture, "id", lectureId);
        return lecture;
    }
}
