package com.msa4lmsv2academic.domain.graduation.entity;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity @Getter @EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED) @EntityListeners(AuditingEntityListener.class)
@Table(name = "graduation_requirements", indexes = @Index(name = "idx_graduation_requirements_department_year", columnList = "department_id, admission_year"))
public class GraduationRequirement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id", nullable = false) private Department department;
    @Column(name = "admission_year", nullable = false) private short admissionYear;
    @Column(name = "required_major_credits", nullable = false) private int requiredMajorCredits;
    @Column(name = "required_general_credits", nullable = false) private int requiredGeneralCredits;
    @Column(name = "required_total_credits", nullable = false) private int requiredTotalCredits;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "required_courses", nullable = false, columnDefinition = "json") private List<String> requiredCourses;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    private GraduationRequirement(Department department, short admissionYear, int requiredMajorCredits,
                                  int requiredGeneralCredits, int requiredTotalCredits, List<String> requiredCourses) {
        this.department = department;
        this.admissionYear = admissionYear;
        this.requiredMajorCredits = requiredMajorCredits;
        this.requiredGeneralCredits = requiredGeneralCredits;
        this.requiredTotalCredits = requiredTotalCredits;
        this.requiredCourses = List.copyOf(requiredCourses);
    }

    public static GraduationRequirement create(Department department, short admissionYear, int requiredMajorCredits,
                                               int requiredGeneralCredits, int requiredTotalCredits,
                                               List<String> requiredCourses) {
        return new GraduationRequirement(department, admissionYear, requiredMajorCredits, requiredGeneralCredits,
                requiredTotalCredits, requiredCourses);
    }
}
