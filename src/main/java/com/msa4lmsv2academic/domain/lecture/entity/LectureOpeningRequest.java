package com.msa4lmsv2academic.domain.lecture.entity;

import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.user.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "lecture_opening_requests",
        indexes = {
                @Index(
                        name = "idx_lecture_opening_requests_professor_status",
                        columnList = "professor_id, status"
                ),
                @Index(
                        name = "idx_lecture_opening_requests_status_created",
                        columnList = "status, created_at"
                )
        }
)
public class LectureOpeningRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "section_no", nullable = false, length = 10)
    private String sectionNo;

    @Column(name = "requested_capacity", nullable = false)
    private int requestedCapacity;

    @Column(nullable = false, length = 50)
    private String classroom;

    @Column(name = "midterm_ratio", nullable = false)
    private int midtermRatio;

    @Column(name = "final_ratio", nullable = false)
    private int finalRatio;

    @Column(name = "assignment_ratio", nullable = false)
    private int assignmentRatio;

    @Column(name = "attendance_ratio", nullable = false)
    private int attendanceRatio;

    @Column(nullable = false, columnDefinition = "text")
    private String syllabus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LectureOpeningRequestStatus status;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @OneToMany(mappedBy = "openingRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek ASC, startPeriod ASC, endPeriod ASC")
    @BatchSize(size = 100)
    private List<LectureOpeningRequestSchedule> schedules = new ArrayList<>();

    @OneToOne(mappedBy = "approvedRequest", fetch = FetchType.LAZY)
    private Lecture approvedLecture;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private LectureOpeningRequest(
            Course course,
            Professor professor,
            Semester semester,
            String sectionNo,
            int requestedCapacity,
            String classroom,
            int midtermRatio,
            int finalRatio,
            int assignmentRatio,
            int attendanceRatio,
            String syllabus
    ) {
        this.course = course;
        this.professor = professor;
        this.semester = semester;
        this.sectionNo = sectionNo;
        this.requestedCapacity = requestedCapacity;
        this.classroom = classroom;
        this.midtermRatio = midtermRatio;
        this.finalRatio = finalRatio;
        this.assignmentRatio = assignmentRatio;
        this.attendanceRatio = attendanceRatio;
        this.syllabus = syllabus;
        this.status = LectureOpeningRequestStatus.PENDING;
    }

    public static LectureOpeningRequest create(
            Course course,
            Professor professor,
            Semester semester,
            String sectionNo,
            int requestedCapacity,
            String classroom,
            int midtermRatio,
            int finalRatio,
            int assignmentRatio,
            int attendanceRatio,
            String syllabus
    ) {
        return new LectureOpeningRequest(
                course,
                professor,
                semester,
                sectionNo,
                requestedCapacity,
                classroom,
                midtermRatio,
                finalRatio,
                assignmentRatio,
                attendanceRatio,
                syllabus
        );
    }

    public void addSchedule(LectureDayOfWeek dayOfWeek, byte startPeriod, byte endPeriod) {
        requirePending();
        schedules.add(LectureOpeningRequestSchedule.create(this, dayOfWeek, startPeriod, endPeriod));
    }

    public void clearSchedules() {
        requirePending();
        schedules.clear();
    }

    public void correct(
            Course course,
            Semester semester,
            String sectionNo,
            int requestedCapacity,
            String classroom,
            int midtermRatio,
            int finalRatio,
            int assignmentRatio,
            int attendanceRatio,
            String syllabus
    ) {
        requirePending();
        this.course = course;
        this.semester = semester;
        this.sectionNo = sectionNo;
        this.requestedCapacity = requestedCapacity;
        this.classroom = classroom;
        this.midtermRatio = midtermRatio;
        this.finalRatio = finalRatio;
        this.assignmentRatio = assignmentRatio;
        this.attendanceRatio = attendanceRatio;
        this.syllabus = syllabus;
    }

    public void approve(User reviewer, LocalDateTime reviewedAt) {
        requirePending();
        this.status = LectureOpeningRequestStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewedAt = reviewedAt;
        this.rejectReason = null;
    }

    public void reject(User reviewer, String rejectReason, LocalDateTime reviewedAt) {
        requirePending();
        this.status = LectureOpeningRequestStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewedAt = reviewedAt;
        this.rejectReason = rejectReason;
    }

    public void linkApprovedLecture(Lecture approvedLecture) {
        if (status != LectureOpeningRequestStatus.APPROVED) {
            throw new IllegalStateException("승인된 강의 개설 신청에만 강의를 연결할 수 있습니다.");
        }
        this.approvedLecture = approvedLecture;
    }

    private void requirePending() {
        if (status != LectureOpeningRequestStatus.PENDING) {
            throw new IllegalStateException("처리 대기 상태인 강의 개설 신청만 변경할 수 있습니다.");
        }
    }
}
