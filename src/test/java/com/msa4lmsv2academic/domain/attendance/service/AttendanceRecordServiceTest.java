package com.msa4lmsv2academic.domain.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.attendance.entity.Attendance;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSession;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceStatus;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceRecordQueryRepository;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceRecordQueryResult;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceRecordSearchResult;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceRepository;
import com.msa4lmsv2academic.domain.attendance.request.AttendanceRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.attendance.request.AttendanceRecordUpdateRequestDTO;
import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.global.error.AttendanceRecordAccessDeniedException;
import com.msa4lmsv2academic.global.error.AttendanceStateConflictException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AttendanceRecordServiceTest {

    @Test
    void returnsRoleScopedAttendancePage() {
        AttendanceRecordQueryRepository queryRepository = mock(AttendanceRecordQueryRepository.class);
        AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        AttendanceRecordService service = new AttendanceRecordService(
                queryRepository, attendanceRepository, auditLogService
        );
        AttendanceRecordSearchRequestDTO request = new AttendanceRecordSearchRequestDTO(
                101L, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                AttendanceStatus.PRESENT, 2, 20
        );
        AttendanceRecordQueryResult item = queryResult();
        when(queryRepository.search(
                10001L, UserRole.STUDENT, 101L, null,
                request.fromDate(), request.toDate(), AttendanceStatus.PRESENT, 20L, 20
        )).thenReturn(new AttendanceRecordSearchResult(List.of(item), 21L));

        var response = service.search(request, new CurrentUser(10001L, "STUDENT"));

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.totalCount()).isEqualTo(21L);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.items()).singleElement().satisfies(record -> {
            assertThat(record.studentName()).isEqualTo("김미래");
            assertThat(record.courseName()).isEqualTo("운영체제");
            assertThat(record.status()).isEqualTo(AttendanceStatus.PRESENT);
        });
    }

    @Test
    void returnsEmptyPageWithDefaultAndMaximumPageSize() {
        AttendanceRecordQueryRepository queryRepository = mock(AttendanceRecordQueryRepository.class);
        AttendanceRecordService service = new AttendanceRecordService(
                queryRepository, mock(AttendanceRepository.class), mock(AuditLogService.class)
        );
        when(queryRepository.search(10001L, UserRole.STUDENT, null, null,
                null, null, null, 0L, 100))
                .thenReturn(new AttendanceRecordSearchResult(List.of(), 0L));

        var response = service.search(
                new AttendanceRecordSearchRequestDTO(null, null, null, null, null, 1, 500),
                new CurrentUser(10001L, "STUDENT")
        );

        assertThat(response.items()).isEmpty();
        assertThat(response.totalCount()).isZero();
        assertThat(response.size()).isEqualTo(100);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void professorUpdatesOwnedAttendanceAndRecordsAudit() {
        AttendanceRecordQueryRepository queryRepository = mock(AttendanceRecordQueryRepository.class);
        AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        AttendanceRecordService service = new AttendanceRecordService(
                queryRepository, attendanceRepository, auditLogService
        );
        Attendance attendance = ownedAttendance(11001L);
        when(attendance.getStatus()).thenReturn(
                AttendanceStatus.PRESENT,
                AttendanceStatus.PRESENT,
                AttendanceStatus.LATE,
                AttendanceStatus.LATE
        );
        when(attendance.getRemarks()).thenReturn(null, "교통 지연", "교통 지연");
        when(attendance.isModified()).thenReturn(false, true, true);
        when(queryRepository.findByIdForUpdate(501L)).thenReturn(java.util.Optional.of(attendance));
        when(attendanceRepository.saveAndFlush(attendance)).thenReturn(attendance);

        var response = service.update(
                501L,
                new AttendanceRecordUpdateRequestDTO(AttendanceStatus.LATE, "  교통 지연  ", " 출석 확인 "),
                new CurrentUser(11001L, "PROFESSOR"),
                "req-501",
                "127.0.0.1"
        );

        assertThat(response.status()).isEqualTo(AttendanceStatus.LATE);
        assertThat(response.remarks()).isEqualTo("교통 지연");
        assertThat(response.modified()).isTrue();
        verify(attendance).modify(AttendanceStatus.LATE, "교통 지연");
        Map<String, Object> beforeValue = new LinkedHashMap<>();
        beforeValue.put("status", AttendanceStatus.PRESENT);
        beforeValue.put("remarks", null);
        beforeValue.put("modified", false);
        verify(auditLogService).record(
                11001L,
                "ATTENDANCE_RECORD_UPDATED",
                "ATTENDANCE",
                501L,
                beforeValue,
                Map.of("status", AttendanceStatus.LATE, "remarks", "교통 지연", "modified", true),
                "출석 확인",
                "req-501",
                "127.0.0.1"
        );
    }

    @Test
    void rejectsStudentUpdateAndProfessorUpdateForAnotherLecture() {
        AttendanceRecordQueryRepository queryRepository = mock(AttendanceRecordQueryRepository.class);
        AttendanceRecordService service = new AttendanceRecordService(
                queryRepository, mock(AttendanceRepository.class), mock(AuditLogService.class)
        );
        AttendanceRecordUpdateRequestDTO request = new AttendanceRecordUpdateRequestDTO(
                AttendanceStatus.ABSENT, null, "오입력 정정"
        );

        assertThatThrownBy(() -> service.update(
                501L, request, new CurrentUser(10001L, "STUDENT"), null, null
        )).isInstanceOf(AttendanceRecordAccessDeniedException.class);
        verify(queryRepository, never()).findByIdForUpdate(501L);

        Attendance anotherProfessorAttendance = ownedAttendance(11002L);
        when(queryRepository.findByIdForUpdate(501L))
                .thenReturn(java.util.Optional.of(anotherProfessorAttendance));
        assertThatThrownBy(() -> service.update(
                501L, request, new CurrentUser(11001L, "PROFESSOR"), null, null
        )).isInstanceOf(AttendanceRecordAccessDeniedException.class);
    }

    @Test
    void rejectsDuplicateUpdateWithSameStatusAndRemarks() {
        AttendanceRecordQueryRepository queryRepository = mock(AttendanceRecordQueryRepository.class);
        AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
        AttendanceRecordService service = new AttendanceRecordService(
                queryRepository, attendanceRepository, mock(AuditLogService.class)
        );
        Attendance attendance = ownedAttendance(11001L);
        when(attendance.getStatus()).thenReturn(AttendanceStatus.PRESENT);
        when(attendance.getRemarks()).thenReturn("확인 완료");
        when(queryRepository.findByIdForUpdate(501L)).thenReturn(java.util.Optional.of(attendance));

        assertThatThrownBy(() -> service.update(
                501L,
                new AttendanceRecordUpdateRequestDTO(AttendanceStatus.PRESENT, "확인 완료", "중복 요청"),
                new CurrentUser(11001L, "PROFESSOR"),
                null,
                null
        )).isInstanceOf(AttendanceStateConflictException.class);
        verify(attendanceRepository, never()).saveAndFlush(attendance);
    }

    private Attendance ownedAttendance(Long professorUserId) {
        Attendance attendance = mock(Attendance.class);
        Enrollment enrollment = mock(Enrollment.class);
        Student student = mock(Student.class);
        User studentUser = mock(User.class);
        Lecture lecture = mock(Lecture.class);
        Professor professor = mock(Professor.class);
        User professorUser = mock(User.class);
        Course course = mock(Course.class);
        Semester semester = mock(Semester.class);
        AttendanceSession session = mock(AttendanceSession.class);

        when(attendance.getId()).thenReturn(501L);
        when(attendance.getEnrollment()).thenReturn(enrollment);
        when(attendance.getSession()).thenReturn(session);
        when(attendance.getLectureDate()).thenReturn(LocalDate.of(2026, 9, 7));
        when(attendance.getPeriod()).thenReturn(2);
        when(attendance.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 9, 7, 11, 0));
        when(session.getId()).thenReturn(701L);
        when(enrollment.getId()).thenReturn(12001L);
        when(enrollment.getStudent()).thenReturn(student);
        when(enrollment.getLecture()).thenReturn(lecture);
        when(student.getId()).thenReturn(2001L);
        when(student.getUser()).thenReturn(studentUser);
        when(studentUser.getName()).thenReturn("김미래");
        when(lecture.getId()).thenReturn(101L);
        when(lecture.getCourse()).thenReturn(course);
        when(lecture.getProfessor()).thenReturn(professor);
        when(lecture.getSemester()).thenReturn(semester);
        when(lecture.getSectionNo()).thenReturn("01");
        when(course.getCode()).thenReturn("CSE301");
        when(course.getName()).thenReturn("운영체제");
        when(professor.getUser()).thenReturn(professorUser);
        when(professorUser.getId()).thenReturn(professorUserId);
        when(professorUser.getName()).thenReturn("홍길동");
        when(semester.getAcademicYear()).thenReturn((short) 2026);
        when(semester.getTerm()).thenReturn(SemesterTerm.SECOND);
        return attendance;
    }

    private AttendanceRecordQueryResult queryResult() {
        return new AttendanceRecordQueryResult(
                501L, 12001L, 2001L, "김미래", 101L, "CSE301", "운영체제", "01", "홍길동",
                (short) 2026, SemesterTerm.SECOND, 701L, LocalDate.of(2026, 9, 7), 2,
                AttendanceStatus.PRESENT, null, LocalDateTime.of(2026, 9, 7, 10, 2), false,
                LocalDateTime.of(2026, 9, 7, 10, 2)
        );
    }
}
