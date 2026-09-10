package com.msa4lmsv2academic.domain.academicschedule.entity;

import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "academic_schedules")
public class AcademicSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AcademicScheduleCategory category;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "academic_year", nullable = false)
    private short academicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SemesterTerm term;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false, length = 20)
    private AcademicScheduleTargetRole targetRole;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    private AcademicSchedule(String title, String content, AcademicScheduleCategory category,
                             LocalDate startDate, LocalDate endDate, short academicYear, SemesterTerm term,
                             AcademicScheduleTargetRole targetRole, User author) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.startDate = startDate;
        this.endDate = endDate;
        this.academicYear = academicYear;
        this.term = term;
        this.targetRole = targetRole;
        this.active = true;
        this.author = author;
    }

    public static AcademicSchedule create(String title, String content, AcademicScheduleCategory category,
                                          LocalDate startDate, LocalDate endDate, short academicYear,
                                          SemesterTerm term, AcademicScheduleTargetRole targetRole, User author) {
        return new AcademicSchedule(title, content, category, startDate, endDate, academicYear, term, targetRole, author);
    }

    /** 기존 데이터 생성 코드와 테스트 호환용 팩터리입니다. */
    public static AcademicSchedule create(String title, String content, LocalDate startDate, LocalDate endDate,
                                          AcademicScheduleTargetRole targetRole, User author) {
        return new AcademicSchedule(
                title, content, AcademicScheduleCategory.OTHER, startDate, endDate,
                (short) startDate.getYear(), startDate.getMonthValue() <= 7 ? SemesterTerm.FIRST : SemesterTerm.SECOND,
                targetRole, author
        );
    }

    public void update(String title, String content, AcademicScheduleCategory category,
                       LocalDate startDate, LocalDate endDate, short academicYear, SemesterTerm term,
                       AcademicScheduleTargetRole targetRole) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.startDate = startDate;
        this.endDate = endDate;
        this.academicYear = academicYear;
        this.term = term;
        this.targetRole = targetRole;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}
