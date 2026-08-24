package com.msa4lmsv2academic.domain.graduation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.graduation.entity.GraduationRequirement;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationRequirementQueryRepository;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationRequirementRepository;
import com.msa4lmsv2academic.domain.graduation.request.GraduationRequirementCreateRequestDTO;
import com.msa4lmsv2academic.domain.graduation.request.GraduationRequirementUpdateRequestDTO;
import com.msa4lmsv2academic.domain.graduation.response.GraduationRequirementResponseDTO;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentQueryRepository;
import com.msa4lmsv2academic.global.error.DuplicateGraduationRequirementException;
import com.msa4lmsv2academic.global.error.GraduationCreditAccessDeniedException;
import com.msa4lmsv2academic.global.error.InvalidGraduationRequirementRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GraduationRequirementServiceTest {

    private static final CurrentUser ADMIN = new CurrentUser(10L, "ADMIN");

    @Mock
    private GraduationRequirementRepository graduationRequirementRepository;

    @Mock
    private GraduationRequirementQueryRepository graduationRequirementQueryRepository;

    @Mock
    private GraduationCreditQueryRepository graduationCreditQueryRepository;

    @Mock
    private DepartmentQueryRepository departmentQueryRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private Department department;

    private GraduationRequirementService service;

    @BeforeEach
    void setUp() {
        service = new GraduationRequirementService(
                graduationRequirementRepository,
                graduationRequirementQueryRepository,
                graduationCreditQueryRepository,
                departmentQueryRepository,
                auditLogService
        );
    }

    @Test
    void adminCreatesUniqueRequirementAndRecordsAudit() {
        when(departmentQueryRepository.findByIdWithCollege(1L)).thenReturn(Optional.of(department));
        when(department.getId()).thenReturn(1L);
        when(department.getCode()).thenReturn("001");
        when(department.getName()).thenReturn("컴퓨터공학과");
        when(department.isActive()).thenReturn(true);
        when(graduationRequirementRepository.saveAndFlush(any(GraduationRequirement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GraduationRequirementResponseDTO response = service.create(
                new GraduationRequirementCreateRequestDTO(1L, 2026, 60, 30, 130),
                ADMIN,
                " graduation-create ",
                " 127.0.0.1 "
        );

        assertThat(response.departmentId()).isEqualTo(1L);
        assertThat(response.admissionYear()).isEqualTo((short) 2026);
        assertThat(response.requiredMajorCredits()).isEqualTo(60);
        verify(auditLogService).record(
                eq(10L),
                eq("GRADUATION_REQUIREMENT_CREATE"),
                eq("GRADUATION_REQUIREMENT"),
                isNull(),
                isNull(),
                anyMap(),
                isNull(),
                eq("graduation-create"),
                eq("127.0.0.1")
        );
    }

    @Test
    void duplicateDepartmentAndAdmissionYearIsRejected() {
        when(departmentQueryRepository.findByIdWithCollege(1L)).thenReturn(Optional.of(department));
        when(department.getId()).thenReturn(1L);
        when(department.isActive()).thenReturn(true);
        when(graduationRequirementRepository.existsByDepartmentIdAndAdmissionYear(1L, (short) 2026))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new GraduationRequirementCreateRequestDTO(1L, 2026, 60, 30, 130),
                ADMIN,
                null,
                null
        )).isInstanceOf(DuplicateGraduationRequirementException.class);

        verify(graduationRequirementRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsCreditsWhoseMajorAndGeneralSumExceedsTotal() {
        assertThatThrownBy(() -> service.create(
                new GraduationRequirementCreateRequestDTO(1L, 2026, 80, 60, 130),
                ADMIN,
                null,
                null
        )).isInstanceOf(InvalidGraduationRequirementRequestException.class);

        verify(departmentQueryRepository, never()).findByIdWithCollege(any());
    }

    @Test
    void inactiveDepartmentRequiresExistingStudentForAdmissionYear() {
        when(departmentQueryRepository.findByIdWithCollege(1L)).thenReturn(Optional.of(department));
        when(department.getId()).thenReturn(1L);
        when(department.isActive()).thenReturn(false);
        when(graduationCreditQueryRepository.existsStudentInDepartmentAndAdmissionYear(1L, (short) 2026))
                .thenReturn(false);

        assertThatThrownBy(() -> service.create(
                new GraduationRequirementCreateRequestDTO(1L, 2026, 60, 30, 130),
                ADMIN,
                null,
                null
        )).isInstanceOf(InvalidGraduationRequirementRequestException.class)
                .hasMessageContaining("비활성 학과");
    }

    @Test
    void sameValueUpdateReturnsWithoutAudit() {
        GraduationRequirement requirement = GraduationRequirement.create(
                department,
                (short) 2026,
                60,
                30,
                130,
                null
        );
        when(department.getId()).thenReturn(1L);
        when(department.getCode()).thenReturn("001");
        when(department.getName()).thenReturn("컴퓨터공학과");
        when(department.isActive()).thenReturn(true);
        when(graduationRequirementQueryRepository.findByIdWithDepartment(11L))
                .thenReturn(Optional.of(requirement));

        GraduationRequirementResponseDTO response = service.update(
                11L,
                new GraduationRequirementUpdateRequestDTO(null, null, 60, null, null, "동일 값 확인"),
                ADMIN,
                null,
                null
        );

        assertThat(response.requiredMajorCredits()).isEqualTo(60);
        verify(graduationRequirementRepository, never()).saveAndFlush(any());
        verify(auditLogService, never()).record(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void nonAdminCannotManageRequirements() {
        assertThatThrownBy(() -> service.create(
                new GraduationRequirementCreateRequestDTO(1L, 2026, 60, 30, 130),
                new CurrentUser(20L, "PROFESSOR"),
                null,
                null
        )).isInstanceOf(GraduationCreditAccessDeniedException.class);
    }
}
