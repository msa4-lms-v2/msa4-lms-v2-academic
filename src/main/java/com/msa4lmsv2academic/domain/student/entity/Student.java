package com.msa4lmsv2academic.domain.student.entity;

import com.msa4lmsv2academic.domain.organization.entity.*;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity @Getter @EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED) @EntityListeners(AuditingEntityListener.class)
@Table(name = "students", uniqueConstraints = @UniqueConstraint(name = "uk_students_user_id", columnNames = "user_id"))
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id", nullable = false) private Department department;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "major_id", nullable = false) private Major major;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "double_major_id") private Major doubleMajor;
    @Column(name = "grade_level", nullable = false) private byte gradeLevel;
    @Column(name = "admission_year", nullable = false) private short admissionYear;
    @Enumerated(EnumType.STRING) @Column(name = "academic_status", nullable = false, length = 20) private AcademicStatus academicStatus;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "advisor_id") private Professor advisor;

    private Student(User user, Department department, Major major, byte gradeLevel, short admissionYear,
                    AcademicStatus academicStatus, Professor advisor) {
        this.user = user;
        this.department = department;
        this.major = major;
        this.gradeLevel = gradeLevel;
        this.admissionYear = admissionYear;
        this.academicStatus = academicStatus;
        this.advisor = advisor;
    }

    public static Student create(User user, Department department, Major major, byte gradeLevel,
                                 short admissionYear, AcademicStatus academicStatus, Professor advisor) {
        return new Student(user, department, major, gradeLevel, admissionYear, academicStatus, advisor);
    }

    public void changeAcademicStatus(AcademicStatus academicStatus) {
        this.academicStatus = academicStatus;
    }
}
