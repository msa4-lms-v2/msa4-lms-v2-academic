package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.EnrollmentAcademicStatusNotAllowedException;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentAcademicStatusValidator {

    public void validate(AcademicStatus academicStatus) {
        if (academicStatus == AcademicStatus.ENROLLED) {
            return;
        }
        throw new EnrollmentAcademicStatusNotAllowedException(academicStatus);
    }
}
