package com.msa4lmsv2academic.domain.admission.entity;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.AdmissionCandidateStateConflictException;
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
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "admission_candidates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_admission_candidates_application_number",
                        columnNames = "application_number"
                ),
                @UniqueConstraint(
                        name = "uk_admission_candidates_student_id",
                        columnNames = "student_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_admission_candidates_department_year_status",
                        columnList = "department_id, admission_year, status"
                ),
                @Index(name = "idx_admission_candidates_name", columnList = "name"),
                @Index(name = "idx_admission_candidates_created_at", columnList = "created_at")
        }
)
public class AdmissionCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "application_number", nullable = false, updatable = false, length = 50)
    private String applicationNumber;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "admission_year", nullable = false)
    private short admissionYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdmissionCandidateStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_changed_by")
    private User statusChangedBy;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private AdmissionCandidate(String applicationNumber, String name, LocalDate birthDate, String email,
                               String phoneNumber, String address, Department department, short admissionYear,
                               User createdBy) {
        this.applicationNumber = applicationNumber;
        this.name = name;
        this.birthDate = birthDate;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.department = department;
        this.admissionYear = admissionYear;
        this.status = AdmissionCandidateStatus.REGISTERED;
        this.createdBy = createdBy;
    }

    public static AdmissionCandidate create(String applicationNumber, String name, LocalDate birthDate,
                                            String email, String phoneNumber, String address,
                                            Department department, short admissionYear, User createdBy) {
        return new AdmissionCandidate(
                applicationNumber, name, birthDate, email, phoneNumber, address,
                department, admissionYear, createdBy
        );
    }

    public void update(String name, LocalDate birthDate, String email, String phoneNumber, String address,
                       Department department, short admissionYear) {
        ensureRegistered();
        this.name = name;
        this.birthDate = birthDate;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.department = department;
        this.admissionYear = admissionYear;
    }

    public boolean changeStatus(AdmissionCandidateStatus targetStatus, User changedBy, LocalDateTime changedAt) {
        if (status == targetStatus) {
            return false;
        }
        boolean allowed = status == AdmissionCandidateStatus.REGISTERED
                && (targetStatus == AdmissionCandidateStatus.CONFIRMED
                || targetStatus == AdmissionCandidateStatus.CANCELLED)
                || status == AdmissionCandidateStatus.CONFIRMED
                && targetStatus == AdmissionCandidateStatus.CANCELLED;
        if (!allowed) {
            throw new AdmissionCandidateStateConflictException(status, targetStatus);
        }
        this.status = targetStatus;
        this.statusChangedBy = changedBy;
        this.statusChangedAt = changedAt;
        return true;
    }

    private void ensureRegistered() {
        if (status != AdmissionCandidateStatus.REGISTERED) {
            throw new AdmissionCandidateStateConflictException(
                    "REGISTERED 상태의 입학 예정자만 인적사항을 수정할 수 있습니다."
            );
        }
    }
}
