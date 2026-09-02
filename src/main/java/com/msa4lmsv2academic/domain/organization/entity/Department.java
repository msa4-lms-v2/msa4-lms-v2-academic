package com.msa4lmsv2academic.domain.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "departments",
        uniqueConstraints = @UniqueConstraint(name = "uk_departments_code", columnNames = "code"),
        indexes = @Index(name = "idx_departments_college_id", columnList = "college_id")
)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 3, updatable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "college_id",
            foreignKey = @ForeignKey(name = "fk_departments_college")
    )
    private College college;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active;

    private Department(String code, College college, String name, boolean active) {
        this.code = code;
        this.college = college;
        this.name = name;
        this.active = active;
    }

    public static Department create(String code, College college, String name, boolean active) {
        return new Department(code, college, name, active);
    }

    public void update(String name, boolean active) {
        this.name = name;
        this.active = active;
    }
}
