package com.msa4lmsv2academic.domain.semester.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity @Getter @EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED) @EntityListeners(AuditingEntityListener.class)
@Table(name = "semesters")
public class Semester {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @Column(name = "academic_year", nullable = false) private int academicYear;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private SemesterTerm term;

    private Semester(int academicYear, SemesterTerm term) {
        this.academicYear = academicYear;
        this.term = term;
    }

    public static Semester create(int academicYear, SemesterTerm term) {
        return new Semester(academicYear, term);
    }
}
