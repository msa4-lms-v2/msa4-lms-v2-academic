package com.msa4lmsv2academic.domain.student.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StudentQueryRepositoryIntegrationTest extends MySqlIntegrationTest {

    private static final Long PROFESSOR_USER_ID = 9101L;

    @Autowired
    private StudentQueryRepository studentQueryRepository;

    @Autowired
    private EntityManager entityManager;

    private Professor professor;
    private Department professorDepartment;
    private Department otherDepartment;

    @BeforeEach
    void setUp() {
        College college = College.create("COL-STUDENT-DIR", "통합대학", true);
        professorDepartment = Department.create("200", college, "컴퓨터공학과", true);
        otherDepartment = Department.create("201", college, "경영학과", true);
        entityManager.persist(college);
        entityManager.persist(professorDepartment);
        entityManager.persist(otherDepartment);

        User professorUser = user(PROFESSOR_USER_ID, "김교수", UserRole.PROFESSOR);
        entityManager.persist(professorUser);
        professor = Professor.create(professorUser, (short) 2020, professorDepartment);
        entityManager.persist(professor);

        Semester currentSemester = semester((short) 2026, SemesterTerm.FIRST, true);
        Semester previousSemester = semester((short) 2025, SemesterTerm.SECOND, false);
        entityManager.persist(currentSemester);
        entityManager.persist(previousSemester);
        Course course = Course.create(
                professorDepartment, "COURSE-STUDENT-DIR", "교양컴퓨팅",
                (byte) 3, null, CompletionType.GENERAL_ELECTIVE
        );
        entityManager.persist(course);
        Lecture currentLecture = lecture(currentSemester, course, "01");
        Lecture previousLecture = lecture(previousSemester, course, "01");
        entityManager.persist(currentLecture);
        entityManager.persist(previousLecture);

        Student sameDepartment = student(9201L, "가학과학생", professorDepartment, null, AcademicStatus.ENROLLED,
                (byte) 2, (short) 2025);
        Student advisee = student(9202L, "나지도학생", otherDepartment, professor, AcademicStatus.ON_LEAVE,
                (byte) 3, (short) 2024);
        Student currentLectureStudent = student(
                9203L, "다수강학생", otherDepartment, null, AcademicStatus.ENROLLED,
                (byte) 1, (short) 2026
        );
        Student unrelated = student(9204L, "라무관학생", otherDepartment, null, AcademicStatus.ENROLLED,
                (byte) 4, (short) 2023);
        Student withdrawnSameDepartment = student(
                9205L, "마자퇴학생", professorDepartment, null, AcademicStatus.WITHDRAWN,
                (byte) 4, (short) 2022
        );
        Student cancelledEnrollmentStudent = student(
                9206L, "바사취학생", otherDepartment, null, AcademicStatus.ENROLLED,
                (byte) 2, (short) 2025
        );
        Student previousLectureStudent = student(
                9207L, "사과거학생", otherDepartment, null, AcademicStatus.ENROLLED,
                (byte) 3, (short) 2024
        );

        entityManager.persist(sameDepartment);
        entityManager.persist(advisee);
        entityManager.persist(currentLectureStudent);
        entityManager.persist(unrelated);
        entityManager.persist(withdrawnSameDepartment);
        entityManager.persist(cancelledEnrollmentStudent);
        entityManager.persist(previousLectureStudent);

        entityManager.persist(Enrollment.create(currentLectureStudent, currentLecture, LocalDateTime.now()));
        Enrollment cancelled = Enrollment.create(cancelledEnrollmentStudent, currentLecture, LocalDateTime.now());
        cancelled.cancel();
        entityManager.persist(cancelled);
        entityManager.persist(Enrollment.create(previousLectureStudent, previousLecture, LocalDateTime.now()));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void professorScopeIsUnionOfAdviseeCurrentLectureAndDepartmentWithCurrentStatuses() {
        ProfessorStudentScope scope = studentQueryRepository.findProfessorScopeByUserId(PROFESSOR_USER_ID)
                .orElseThrow();

        StudentSearchResult result = studentQueryRepository.search(condition(scope, null, "name", false, 100));

        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.items())
                .extracting(student -> student.getUser().getName())
                .containsExactly("가학과학생", "나지도학생", "다수강학생");
    }

    @Test
    void adminCanFilterTerminalStatusWithoutProfessorScope() {
        StudentSearchResult result = studentQueryRepository.search(
                condition(null, AcademicStatus.WITHDRAWN, "name", false, 100)
        );

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.items()).extracting(student -> student.getUser().getName())
                .containsExactly("마자퇴학생");
    }

    @Test
    void adminSearchSortAndPaginationRemainStable() {
        StudentSearchCondition condition = new StudentSearchCondition(
                0, 2, "학생", null, null, null, null,
                "gradeLevel", true, null
        );

        StudentSearchResult result = studentQueryRepository.search(condition);

        assertThat(result.totalCount()).isEqualTo(7);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items()).extracting(Student::getGradeLevel).containsExactly((byte) 4, (byte) 4);
        assertThat(result.items().get(0).getId()).isLessThan(result.items().get(1).getId());
    }

    private StudentSearchCondition condition(
            ProfessorStudentScope scope,
            AcademicStatus academicStatus,
            String sortBy,
            boolean descending,
            long limit
    ) {
        return new StudentSearchCondition(
                0, limit, null, null, null, null, academicStatus, sortBy, descending, scope
        );
    }

    private User user(Long id, String name, UserRole role) {
        return User.synchronize(
                id, name, name + "@student-directory.test", null, null, role, UserStatus.ACTIVE
        );
    }

    private Student student(Long userId, String name, Department department, Professor advisor,
                            AcademicStatus academicStatus, byte gradeLevel, short admissionYear) {
        User studentUser = user(userId, name, UserRole.STUDENT);
        entityManager.persist(studentUser);
        Student student = Student.create(
                studentUser, department, gradeLevel, admissionYear, advisor
        );
        student.changeAcademicStatus(academicStatus);
        return student;
    }

    private Semester semester(short year, SemesterTerm term, boolean current) {
        return Semester.create(
                year,
                term,
                LocalDate.of(year, term == SemesterTerm.FIRST ? 3 : 9, 1),
                LocalDate.of(year, term == SemesterTerm.FIRST ? 6 : 12, 20),
                LocalDateTime.of(year, term == SemesterTerm.FIRST ? 2 : 8, 1, 9, 0),
                LocalDateTime.of(year, term == SemesterTerm.FIRST ? 2 : 8, 20, 18, 0),
                current
        );
    }

    private Lecture lecture(Semester semester, Course course, String sectionNo) {
        return Lecture.create(
                semester, course, professor, sectionNo, 30, "A101", LectureStatus.OPEN,
                25, 25, 25, 25, null
        );
    }
}
