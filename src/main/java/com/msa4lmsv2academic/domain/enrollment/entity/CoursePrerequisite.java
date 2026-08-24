package com.msa4lmsv2academic.domain.enrollment.entity;

import com.msa4lmsv2academic.domain.course.entity.Course;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "course_prerequisites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_prerequisites_course_prerequisite",
                columnNames = {"course_id", "prerequisite_course_id"}
        ),
        indexes = {
                @Index(
                        name = "idx_course_prerequisites_course_active",
                        columnList = "course_id,is_active"
                ),
                @Index(
                        name = "idx_course_prerequisites_prerequisite_active",
                        columnList = "prerequisite_course_id,is_active"
                )
        },
        check = @CheckConstraint(
                name = "ck_course_prerequisites_distinct_courses",
                constraint = "course_id <> prerequisite_course_id"
        )
)
public class CoursePrerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prerequisite_course_id", nullable = false)
    private Course prerequisiteCourse;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private CoursePrerequisite(Course course, Course prerequisiteCourse) {
        this.course = course;
        this.prerequisiteCourse = prerequisiteCourse;
        this.active = true;
    }

    public static CoursePrerequisite create(Course course, Course prerequisiteCourse) {
        return new CoursePrerequisite(course, prerequisiteCourse);
    }

    public void changeCourses(Course course, Course prerequisiteCourse) {
        this.course = course;
        this.prerequisiteCourse = prerequisiteCourse;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
