package com.msa4lmsv2academic.domain.provisioning.service;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentRepository;
import com.msa4lmsv2academic.domain.outbox.service.OutboxEventService;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.professor.repository.ProfessorRepository;
import com.msa4lmsv2academic.domain.provisioning.request.ProfessorProvisioningRequestDTO;
import com.msa4lmsv2academic.domain.provisioning.request.StudentProvisioningRequestDTO;
import com.msa4lmsv2academic.domain.provisioning.response.ProfessorProvisioningResponseDTO;
import com.msa4lmsv2academic.domain.provisioning.response.StudentProvisioningResponseDTO;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.student.repository.StudentRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountProvisioningService {

    private static final String AGGREGATE_TYPE_STUDENT = "STUDENT";
    private static final String EVENT_STUDENT_SNAPSHOT_CHANGED = "StudentSnapshotChanged";

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final OutboxEventService outboxEventService;
    private final com.msa4lmsv2academic.domain.admission.repository.AdmissionCandidateRepository admissionCandidateRepository;

    /*
     * 학생 프로비저닝
     */
    @Transactional
    public StudentProvisioningResponseDTO provisionStudent(
            StudentProvisioningRequestDTO request
    ) {
        // 응답 유실 후 재시도에도 기존 번호를 그대로 반환한다.
        Student existing = studentRepository.findByUserId(request.userId()).orElse(null);
        if (existing != null) {
            if (request.admissionCandidateId() != null) {
                var linked = admissionCandidateRepository.findByIdForUpdate(request.admissionCandidateId()).orElseThrow();
                if (linked.getStudent() == null || !linked.getStudent().getId().equals(existing.getId())) {
                    throw new IllegalStateException("입학 예정자와 학생 계정의 연결이 일치하지 않습니다.");
                }
            }
            return new StudentProvisioningResponseDTO(request.userId(), existing.getStudentNumber());
        }
        com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidate candidate = null;
        if (request.admissionCandidateId() != null) {
            candidate = admissionCandidateRepository.findByIdForUpdate(request.admissionCandidateId()).orElseThrow();
            if (!java.util.Objects.equals(candidate.getEmail(), request.email())
                    || !candidate.getDepartment().getId().equals(request.departmentId())
                    || candidate.getAdmissionYear() != request.admissionYear()) {
                throw new IllegalArgumentException("입학 예정자 등록 정보가 계정 생성 요청과 일치하지 않습니다.");
            }
        }
        // 사용자 및 이메일 중복 확인
        validateUserAndEmail(
                request.userId(),
                request.email()
        );

        // 학과 조회 및 검증
        Department department =
                findAndValidateDepartment(
                        request.departmentId()
                );
        Professor advisor = null;
        if (request.advisorProfessorId() != null) {
            advisor = professorRepository.findById(request.advisorProfessorId())
                    .orElseThrow(() -> new com.msa4lmsv2academic.global.error.InvalidAdmissionCandidateRequestException(
                            "지도교수를 찾을 수 없습니다."));
            if (!advisor.getDepartment().getId().equals(department.getId())
                    || advisor.getUser().getStatus() != com.msa4lmsv2academic.domain.user.entity.UserStatus.ACTIVE) {
                throw new com.msa4lmsv2academic.global.error.InvalidAdmissionCandidateRequestException(
                        "학생 학과의 활성 교수만 지도교수로 배정할 수 있습니다.");
            }
        }

        // Academic users 저장
        User user = User.provision(
                request.userId(),
                request.name(),
                request.email(),
                request.phoneNumber(),
                request.address(),
                UserRole.STUDENT
        );

        userRepository.save(user);

        // students 저장
        Student student = Student.create(
                user,
                department,
                (byte) 1,
                request.admissionYear(),
                advisor
        );

        Student savedStudent =
                studentRepository.saveAndFlush(student);

        // 학번 생성
        String studentNumber = AcademicNumberGenerator.generate(
                request.admissionYear(),
                department.getId(),
                savedStudent.getId()
        );
        savedStudent.assignStudentNumber(studentNumber);
        studentRepository.flush();
        if (candidate != null) candidate.markProvisioned(savedStudent);

        outboxEventService.record(
                AGGREGATE_TYPE_STUDENT,
                savedStudent.getId(),
                EVENT_STUDENT_SNAPSHOT_CHANGED,
                studentSnapshotPayload(savedStudent),
                savedStudent.getSnapshotVersion()
        );

        // 생성한 학번을 Auth에 반환
        return new StudentProvisioningResponseDTO(
                request.userId(),
                studentNumber
        );
    }

    /*
     * 교수 프로비저닝
     */
    @Transactional
    public ProfessorProvisioningResponseDTO provisionProfessor(
            ProfessorProvisioningRequestDTO request
    ) {
        Professor existing = professorRepository.findByUserId(request.userId()).orElse(null);
        if (existing != null && existing.getProfessorNumber() != null) {
            return new ProfessorProvisioningResponseDTO(request.userId(), existing.getProfessorNumber());
        }
        // 사용자 및 이메일 중복 확인
        validateUserAndEmail(
                request.userId(),
                request.email()
        );

        // 학과 조회 및 검증
        Department department =
                findAndValidateDepartment(
                        request.departmentId()
                );

        // Academic users 저장
        User user = User.provision(
                request.userId(),
                request.name(),
                request.email(),
                request.phoneNumber(),
                request.address(),
                UserRole.PROFESSOR
        );

        userRepository.save(user);

        // professors 저장
        Professor professor = Professor.create(
                user,
                request.hireYear(),
                department
        );

        Professor savedProfessor =
                professorRepository.saveAndFlush(professor);

        // 교번 생성
        String professorNumber = "p" + AcademicNumberGenerator.generate(
                request.hireYear(),
                department.getId(),
                savedProfessor.getId()
        );
        savedProfessor.assignProfessorNumber(professorNumber);
        professorRepository.flush();

        // 생성한 교번을 Auth에 반환
        return new ProfessorProvisioningResponseDTO(
                request.userId(),
                professorNumber
        );
    }

    /*
     * 사용자 ID와 이메일 중복 확인
     */
    private void validateUserAndEmail(
            Long userId,
            String email
    ) {
        if (userRepository.existsById(userId)) {
            throw new IllegalStateException(
                    "이미 프로비저닝된 사용자입니다."
            );
        }

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(
                email,
                userId
        )) {
            throw new IllegalStateException(
                    "이미 사용 중인 이메일입니다."
            );
        }
    }

    /*
     * 학과 조회 및 활성 상태 확인
     */
    private Department findAndValidateDepartment(
            Long departmentId
    ) {
        Department department =
                departmentRepository.findById(departmentId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 학과입니다."
                                )
                        );

        if (!department.isActive()) {
            throw new IllegalStateException(
                    "비활성화된 학과입니다."
            );
        }

        return department;
    }

    private Map<String, Object> studentSnapshotPayload(Student student) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("studentId", student.getId());
        payload.put("userId", student.getUser().getId());
        payload.put("displayName", student.getUser().getName());
        payload.put("departmentName", student.getDepartment().getName());
        payload.put("sourceVersion", student.getSnapshotVersion());
        return payload;
    }
}
