package com.msa4lmsv2academic.domain.student.entity;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.user.entity.User;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "students",
        uniqueConstraints = @UniqueConstraint(name = "uk_students_user_id", columnNames = "user_id"),
        check = @CheckConstraint(
                name = "ck_students_distinct_departments",
                constraint = "double_major_id IS NULL OR department_id <> double_major_id"
        ),
        indexes = {
                @Index(name = "idx_students_department_id", columnList = "department_id"),
                @Index(name = "idx_students_double_major_id", columnList = "double_major_id"),
                @Index(name = "idx_students_advisor_id", columnList = "advisor_id")
        }
)
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "double_major_id")
    private Department doubleMajor;

    @Column(name = "grade_level", nullable = false)
    private byte gradeLevel;

    @Column(name = "admission_year", nullable = false)
    private short admissionYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_status", nullable = false, length = 20)
    private AcademicStatus academicStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advisor_id")
    private Professor advisor;

    private Student(User user, Department department, byte gradeLevel, short admissionYear,
                    Professor advisor) {
        this.user = user;
        this.department = department;
        this.gradeLevel = gradeLevel;
        this.admissionYear = admissionYear;
        this.academicStatus = AcademicStatus.ENROLLED;
        this.advisor = advisor;
    }

    public static Student create(User user, Department department, byte gradeLevel,
                                 short admissionYear, Professor advisor) {
        return new Student(user, department, gradeLevel, admissionYear, advisor);
    }

    public void changeAcademicStatus(AcademicStatus academicStatus) {
        this.academicStatus = academicStatus;
    }

    public void changeAffiliation(Department department) {
        this.department = department;
    }

    public void clearAdvisor() {
        this.advisor = null;
    }

    public void assignDoubleMajor(Department doubleMajor) {
        this.doubleMajor = doubleMajor;
    }
}
