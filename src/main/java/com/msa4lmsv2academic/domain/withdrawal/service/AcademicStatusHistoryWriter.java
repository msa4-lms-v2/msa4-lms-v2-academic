package com.msa4lmsv2academic.domain.withdrawal.service;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.withdrawal.entity.AcademicStatusHistory;
import com.msa4lmsv2academic.domain.withdrawal.repository.AcademicStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class AcademicStatusHistoryWriter {
    private final AcademicStatusHistoryRepository repository;

    public void recordLeave(Student student, AcademicStatus previous, User actor, Long requestId) {
        repository.saveAndFlush(AcademicStatusHistory.leaveChanged(student, previous, actor, requestId));
    }
}
