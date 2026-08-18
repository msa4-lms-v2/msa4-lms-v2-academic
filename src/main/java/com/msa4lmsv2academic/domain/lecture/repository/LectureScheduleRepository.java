package com.msa4lmsv2academic.domain.lecture.repository;

import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.lecture.entity.LectureSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LectureScheduleRepository extends JpaRepository<LectureSchedule, Long> {

    @Query("""
            select (count(schedule) > 0)
            from LectureSchedule schedule
            where schedule.lecture.professor.id = :professorId
              and schedule.lecture.semester.id = :semesterId
              and schedule.dayOfWeek = :dayOfWeek
              and schedule.startPeriod <= :endPeriod
              and schedule.endPeriod >= :startPeriod
            """)
    boolean existsProfessorScheduleConflict(
            @Param("professorId") Long professorId,
            @Param("semesterId") Long semesterId,
            @Param("dayOfWeek") LectureDayOfWeek dayOfWeek,
            @Param("startPeriod") byte startPeriod,
            @Param("endPeriod") byte endPeriod
    );
}
