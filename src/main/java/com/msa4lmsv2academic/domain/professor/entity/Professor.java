package com.msa4lmsv2academic.domain.professor.entity;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity @Getter @EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED) @EntityListeners(AuditingEntityListener.class)
@Table(
        name = "professors",
        uniqueConstraints = @UniqueConstraint(name = "uk_professors_user_id", columnNames = "user_id"),
        indexes = @Index(name = "idx_professors_department_id", columnList = "department_id")
)
public class Professor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "hire_year") private Short hireYear;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id", nullable = false) private Department department;

    private Professor(User user, Short hireYear, Department department) {
        this.user = user;
        this.hireYear = hireYear;
        this.department = department;
    }

    public static Professor create(User user, Short hireYear, Department department) {
        return new Professor(user, hireYear, department);
    }

    public void updateEmployment(Department department, Short hireYear) {
        this.department = department;
        this.hireYear = hireYear;
    }
}
