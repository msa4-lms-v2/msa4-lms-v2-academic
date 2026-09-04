package com.msa4lmsv2academic.domain.outbox.controller;

import com.msa4lmsv2academic.domain.outbox.response.SemesterSnapshotSyncResponseDTO;
import com.msa4lmsv2academic.domain.outbox.response.StudentSnapshotSyncResponseDTO;
import com.msa4lmsv2academic.domain.outbox.response.WithdrawalSnapshotSyncResponseDTO;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.student.repository.StudentRepository;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalRequest;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalStatus;
import com.msa4lmsv2academic.domain.withdrawal.repository.WithdrawalRequestRepository;
import com.msa4lmsv2academic.global.error.SemesterNotFoundException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.error.WithdrawalNotFoundException;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment 등 내부 백엔드가 Kafka 24시간 보관 제약을 넘겨 이벤트를 놓쳤을 때 쓰는 읽기 전용 재동기화 경로.
 * X-User-Id/X-User-Role 없이 오는 시스템 요청이며, 이 서비스 Pod가 클러스터 외부에 직접 노출되지 않는다는
 * 전제(SecurityConfig의 permitAll + NetworkPolicy)로만 접근을 제한한다. 별도 서비스 인증은 두지 않는다.
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class SnapshotSyncController {

    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;

    @Transactional(readOnly = true)
    @GetMapping("/api/academic/students/{studentId}/snapshot")
    public ResponseEntity<GlobalResponseDTO<StudentSnapshotSyncResponseDTO>> getStudentSnapshot(
            @PathVariable Long studentId
    ) {
        Student student = studentRepository.findById(studentId).orElseThrow(StudentNotFoundException::new);
        return ResponseEntity.ok(GlobalResponseDTO.success(StudentSnapshotSyncResponseDTO.from(student)));
    }

    @Transactional(readOnly = true)
    @GetMapping("/api/academic/students/by-user/{userId}/snapshot")
    public ResponseEntity<GlobalResponseDTO<StudentSnapshotSyncResponseDTO>> getStudentSnapshotByUserId(
            @PathVariable Long userId
    ) {
        Student student = studentRepository.findByUserId(userId).orElseThrow(StudentNotFoundException::new);
        return ResponseEntity.ok(GlobalResponseDTO.success(StudentSnapshotSyncResponseDTO.from(student)));
    }

    @GetMapping("/api/academic/catalog/semesters/{semesterId}/snapshot")
    public ResponseEntity<GlobalResponseDTO<SemesterSnapshotSyncResponseDTO>> getSemesterSnapshot(
            @PathVariable Long semesterId
    ) {
        Semester semester = semesterRepository.findById(semesterId).orElseThrow(SemesterNotFoundException::new);
        return ResponseEntity.ok(GlobalResponseDTO.success(SemesterSnapshotSyncResponseDTO.from(semester)));
    }

    @GetMapping("/api/academic/withdrawals/{withdrawalId}/snapshot")
    public ResponseEntity<GlobalResponseDTO<WithdrawalSnapshotSyncResponseDTO>> getWithdrawalSnapshot(
            @PathVariable Long withdrawalId
    ) {
        WithdrawalRequest request = withdrawalRequestRepository.findDetailById(withdrawalId)
                .filter(candidate -> candidate.getStatus() == WithdrawalStatus.APPROVED)
                .orElseThrow(WithdrawalNotFoundException::new);
        return ResponseEntity.ok(GlobalResponseDTO.success(WithdrawalSnapshotSyncResponseDTO.from(request)));
    }
}
