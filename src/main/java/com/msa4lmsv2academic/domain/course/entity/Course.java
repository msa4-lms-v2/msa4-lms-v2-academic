package com.msa4lmsv2academic.domain.course.entity;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity @Getter @EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED) @EntityListeners(AuditingEntityListener.class)
@Table(name = "courses", uniqueConstraints = @UniqueConstraint(name = "uk_courses_code", columnNames = "code"), indexes = @Index(name = "idx_courses_department_id", columnList = "department_id"))
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id", nullable = false) private Department department;
    @Column(nullable = false, length = 20) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false) private byte credits;
    @Column(name = "target_grade", nullable = false) private byte targetGrade;
    @Enumerated(EnumType.STRING) @Column(name = "completion_type", nullable = false, length = 30) private CompletionType completionType;

    private Course(Department department, String code, String name, byte credits, byte targetGrade, CompletionType completionType) {
        this.department = department;
        this.code = code;
        this.name = name;
        this.credits = credits;
        this.targetGrade = targetGrade;
        this.completionType = completionType;
    }

    public static Course create(Department department, String code, String name, byte credits, byte targetGrade, CompletionType completionType) {
        return new Course(department, code, name, credits, targetGrade, completionType);
    }
}
