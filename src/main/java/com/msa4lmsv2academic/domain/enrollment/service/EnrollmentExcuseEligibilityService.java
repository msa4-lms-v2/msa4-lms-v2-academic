package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentExcuseQueryRepository;
import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentExcuseEligibilityService {

    private final EnrollmentExcuseQueryRepository queryRepository;

    @Transactional
    public Optional<Enrollment> findOwnedEnrollmentForUpdate(Long enrollmentId, Long userId) {
        return queryRepository.findOwnedEnrollmentForUpdate(enrollmentId, userId);
    }

    public boolean hasLectureSchedule(Long lectureId, LectureDayOfWeek dayOfWeek, byte period) {
        return queryRepository.existsLectureSchedule(lectureId, dayOfWeek, period);
    }
}
