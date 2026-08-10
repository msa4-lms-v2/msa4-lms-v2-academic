package com.msa4lmsv2academic.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.graduation.entity.GraduationRequirement;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.entity.Major;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AcademicCommonEntityTest {

    @Test
    void confirmedErdRelationshipsAreConnected() {
        College college = College.create("ENG", "공과대학", true);
        Department department = Department.create("CSE", college, "컴퓨터공학과", true);
        Major major = Major.create(department, "CSE", "컴퓨터공학", true);
        User professorUser = User.synchronize(10L, "교수", "professor@test.com", null, null,
                UserRole.PROFESSOR, UserStatus.ACTIVE);
        Professor professor = Professor.create(professorUser, (short) 2020, department);
        User studentUser = User.synchronize(20L, "학생", "student@test.com", null, null,
                UserRole.STUDENT, UserStatus.ACTIVE);
        Student student = Student.create(studentUser, department, major, (byte) 4, (short) 2022,
                AcademicStatus.ENROLLED, professor);
        Semester semester = Semester.create(2026, SemesterTerm.FIRST);
        Course course = Course.create(department, "CSE101", "프로그래밍", (byte) 3, (byte) 1,
                CompletionType.MAJOR_REQUIRED);
        Lecture lecture = Lecture.create(semester, course, professor, "01", 30, "A101",
                LectureStatus.OPEN, 30, 30, 20, 20, "강의계획");
        Enrollment enrollment = Enrollment.create(student, lecture, LocalDateTime.of(2026, 8, 10, 9, 0));
        GraduationRequirement requirement = GraduationRequirement.create(department, (short) 2022,
                60, 30, 130, List.of("CSE101"));

        assertThat(student.getDepartment()).isSameAs(department);
        assertThat(student.getMajor()).isSameAs(major);
        assertThat(lecture.getCourse()).isSameAs(course);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollment.getGradeStatus()).isEqualTo(GradeStatus.DRAFT);
        assertThat(requirement.getRequiredCourses()).containsExactly("CSE101");

        enrollment.cancel();

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    }
}
