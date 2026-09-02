package com.msa4lmsv2academic.domain.lecture.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class LectureOpeningRequestTest {

    @Test
    void approvesPendingRequestAndCreatesLectureFromFinalRequestValues() {
        LectureOpeningRequest request = request();
        request.addSchedule(LectureDayOfWeek.MON, (byte) 1, (byte) 2);
        User admin = User.synchronize(9002L, "관리자", null, null, null, UserRole.ADMIN, UserStatus.ACTIVE);
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 2, 20, 10, 0);

        request.approve(admin, reviewedAt);
        Lecture lecture = Lecture.fromApprovedOpeningRequest(request);
        request.linkApprovedLecture(lecture);

        assertThat(request.getStatus()).isEqualTo(LectureOpeningRequestStatus.APPROVED);
        assertThat(request.getReviewedBy()).isEqualTo(admin);
        assertThat(request.getReviewedAt()).isEqualTo(reviewedAt);
        assertThat(lecture.getApprovedRequest()).isEqualTo(request);
        assertThat(lecture.getCapacity()).isEqualTo(40);
        assertThat(lecture.getStatus()).isEqualTo(LectureStatus.OPEN);
    }

    @Test
    void rejectsSecondReviewAfterRequestWasProcessed() {
        LectureOpeningRequest request = request();
        User admin = User.synchronize(9002L, "관리자", null, null, null, UserRole.ADMIN, UserStatus.ACTIVE);
        request.reject(admin, "시간표 재검토 필요", LocalDateTime.now());

        assertThatThrownBy(() -> request.approve(admin, LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    private LectureOpeningRequest request() {
        Department department = Department.create("100", null, "컴퓨터공학과", true);
        User professorUser = User.synchronize(
                9001L, "담당교수", null, null, null, UserRole.PROFESSOR, UserStatus.ACTIVE
        );
        Professor professor = Professor.create(professorUser, (short) 2020, department);
        Course course = Course.create(
                department, "CSE301", "운영체제", (byte) 3, (byte) 3, CompletionType.MAJOR_REQUIRED
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
        return LectureOpeningRequest.create(
                course, professor, semester, "01", 40, "공학관 301호", 30, 30, 30, 10, "강의계획서"
        );
    }
}
