package com.msa4lmsv2academic.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.entity.Major;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AcademicCommonEntityPersistenceTest extends MySqlIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsNullableProfileFieldsAndJpaAuditingValues() {
        College college = College.create("ENG-PERSIST", "공과대학", true);
        Department department = Department.create("203", college, "컴퓨터공학과", true);
        User professorUser = User.synchronize(
                10001L, "교수", null, null, null, UserRole.PROFESSOR, UserStatus.ACTIVE
        );
        Professor professor = Professor.create(professorUser, null, department);
        User studentUser = User.synchronize(
                10002L, "학생", null, null, null, UserRole.STUDENT, UserStatus.ACTIVE
        );
        Student student = Student.create(studentUser, department, null, (byte) 1, (short) 2026, professor);

        entityManager.persist(college);
        entityManager.persist(department);
        entityManager.persist(professorUser);
        entityManager.persist(professor);
        entityManager.persist(studentUser);
        entityManager.persist(student);
        entityManager.flush();

        assertThat(professor.getHireYear()).isNull();
        assertThat(student.getMajor()).isNull();
        assertThat(studentUser.getCreatedAt()).isNotNull();
        assertThat(studentUser.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsSamePrimaryAndDoubleMajorAtDatabaseBoundary() {
        College college = College.create("204", "경영대학", true);
        Department department = Department.create("204", college, "경영학과", true);
        Major major = Major.create(department, "BUS-MAJOR", "경영학", true);
        User studentUser = User.synchronize(
                10003L, "복수전공학생", null, null, null, UserRole.STUDENT, UserStatus.ACTIVE
        );
        Student student = Student.create(studentUser, department, major, (byte) 2, (short) 2025, null);

        entityManager.persist(college);
        entityManager.persist(department);
        entityManager.persist(major);
        entityManager.persist(studentUser);
        entityManager.persist(student);
        entityManager.flush();

        student.assignDoubleMajor(major);

        assertThatThrownBy(entityManager::flush)
                .isInstanceOf(PersistenceException.class);
    }
}
