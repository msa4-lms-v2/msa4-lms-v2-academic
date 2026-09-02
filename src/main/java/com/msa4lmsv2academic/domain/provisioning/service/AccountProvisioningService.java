package com.msa4lmsv2academic.domain.provisioning.service;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountProvisioningService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;

    /*
     * 학생 프로비저닝
     */
    @Transactional
    public StudentProvisioningResponseDTO provisionStudent(
            StudentProvisioningRequestDTO request
    ) {
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
                UserRole.STUDENT
        );

        userRepository.save(user);

        // students 저장
        Student student = Student.create(
                user,
                department,
                (byte) 1,
                request.admissionYear(),
                null
        );

        Student savedStudent =
                studentRepository.saveAndFlush(student);

        // 학번 생성
        String studentNumber = generateStudentNumber(
                request.admissionYear(),
                department.getCode(),
                savedStudent.getId()
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
        String professorNumber = generateProfessorNumber(
                request.hireYear(),
                department.getCode(),
                savedProfessor.getId()
        );

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

    /*
     * 학번 생성
     *
     * 예:
     * 입학 연도: 2026
     * 학과 코드: 001
     * 학생 ID: 1
     * 결과: 26001001
     */
    private String generateStudentNumber(
            Short admissionYear,
            String departmentCode,
            Long studentId
    ) {
        int shortYear = admissionYear % 100;

        return String.format(
                "%02d%s%03d",
                shortYear,
                departmentCode,
                studentId
        );
    }

    /*
     * 교번 생성
     *
     * 예:
     * 임용 연도: 2026
     * 학과 코드: 001
     * 교수 ID: 1
     * 결과: p26001001
     */
    private String generateProfessorNumber(
            Short hireYear,
            String departmentCode,
            Long professorId
    ) {
        int shortYear = hireYear % 100;

        return String.format(
                "p%02d%s%03d",
                shortYear,
                departmentCode,
                professorId
        );
    }
}
