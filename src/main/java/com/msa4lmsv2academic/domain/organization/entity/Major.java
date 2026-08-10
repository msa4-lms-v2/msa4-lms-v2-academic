package com.msa4lmsv2academic.domain.organization.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity @Getter @EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED) @EntityListeners(AuditingEntityListener.class)
@Table(name = "majors", uniqueConstraints = @UniqueConstraint(name = "uk_majors_code", columnNames = "code"), indexes = @Index(name = "idx_majors_department_id", columnList = "department_id"))
public class Major {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id", nullable = false) private Department department;
    @Column(nullable = false, length = 50) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false) private boolean active;

    private Major(Department department, String code, String name, boolean active) {
        this.department = department;
        this.code = code;
        this.name = name;
        this.active = active;
    }

    public static Major create(Department department, String code, String name, boolean active) {
        return new Major(department, code, name, active);
    }
}
