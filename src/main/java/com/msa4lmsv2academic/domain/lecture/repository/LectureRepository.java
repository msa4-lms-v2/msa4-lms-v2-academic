package com.msa4lmsv2academic.domain.lecture.repository;

import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    boolean existsBySemesterIdAndCourseIdAndSectionNo(Long semesterId, Long courseId, String sectionNo);
}
