package com.msa4lmsv2academic.domain.lecture.entity;

import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity @Getter @EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED) @EntityListeners(AuditingEntityListener.class)
@Table(name = "lectures")
public class Lecture {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "semester_id", nullable = false) private Semester semester;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "course_id", nullable = false) private Course course;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "professor_id", nullable = false) private Professor professor;
    @Column(name = "section_no", nullable = false, length = 20) private String sectionNo;
    @Column(nullable = false) private int capacity;
    @Column(length = 50) private String classroom;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private LectureStatus status;
    @Column(name = "midterm_ratio", nullable = false) private int midtermRatio;
    @Column(name = "final_ratio", nullable = false) private int finalRatio;
    @Column(name = "assignment_ratio", nullable = false) private int assignmentRatio;
    @Column(name = "attendance_ratio", nullable = false) private int attendanceRatio;
    @Lob private String syllabus;

    private Lecture(Semester semester, Course course, Professor professor, String sectionNo, int capacity,
                    String classroom, LectureStatus status, int midtermRatio, int finalRatio,
                    int assignmentRatio, int attendanceRatio, String syllabus) {
        this.semester = semester;
        this.course = course;
        this.professor = professor;
        this.sectionNo = sectionNo;
        this.capacity = capacity;
        this.classroom = classroom;
        this.status = status;
        this.midtermRatio = midtermRatio;
        this.finalRatio = finalRatio;
        this.assignmentRatio = assignmentRatio;
        this.attendanceRatio = attendanceRatio;
        this.syllabus = syllabus;
    }

    public static Lecture create(Semester semester, Course course, Professor professor, String sectionNo,
                                 int capacity, String classroom, LectureStatus status, int midtermRatio,
                                 int finalRatio, int assignmentRatio, int attendanceRatio, String syllabus) {
        return new Lecture(semester, course, professor, sectionNo, capacity, classroom, status,
                midtermRatio, finalRatio, assignmentRatio, attendanceRatio, syllabus);
    }
}
