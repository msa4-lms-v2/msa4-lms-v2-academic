package com.msa4lmsv2academic.domain.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.CollegeRepository;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentRepository;
import com.msa4lmsv2academic.domain.organization.request.DepartmentCreateRequestDTO;
import com.msa4lmsv2academic.domain.organization.request.DepartmentUpdateRequestDTO;
import com.msa4lmsv2academic.domain.organization.response.DepartmentResponseDTO;
import com.msa4lmsv2academic.global.error.DuplicateDepartmentException;
import com.msa4lmsv2academic.global.error.InvalidDepartmentRequestException;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DepartmentServiceTest extends MySqlIntegrationTest {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private College engineering;
    private College inactiveCollege;

    @BeforeEach
    void setUp() {
        departmentRepository.deleteAllInBatch();
        collegeRepository.deleteAllInBatch();

        engineering = collegeRepository.save(College.create("ENG", "공과대학", true));
        inactiveCollege = collegeRepository.save(College.create("OLD", "폐지대학", false));
    }

    @Test
    void createDefaultsActiveAndKeepsDocumentedCodeValue() {
        DepartmentResponseDTO created = departmentService.createDepartment(
                new DepartmentCreateRequestDTO("cse_01", " 컴퓨터공학과 ", engineering.getId(), null)
        );

        assertThat(created.code()).isEqualTo("cse_01");
        assertThat(created.name()).isEqualTo("컴퓨터공학과");
        assertThat(created.active()).isTrue();
    }

    @Test
    void createAllowsDepartmentWithoutCollege() {
        DepartmentResponseDTO created = departmentService.createDepartment(
                new DepartmentCreateRequestDTO("FREE", "자유전공학부", null, true)
        );

        assertThat(created.college()).isNull();
        assertThat(departmentRepository.findById(created.id()).orElseThrow().getCollege()).isNull();
    }

    @Test
    void createAllowsExplicitInactiveDepartment() {
        DepartmentResponseDTO created = departmentService.createDepartment(
                new DepartmentCreateRequestDTO("CSE", "컴퓨터공학과", engineering.getId(), false)
        );

        assertThat(created.active()).isFalse();
    }

    @Test
    void createRejectsCodeLongerThanTwentyCharacters() {
        assertThatThrownBy(() -> departmentService.createDepartment(
                new DepartmentCreateRequestDTO("123456789012345678901", "컴퓨터공학과", engineering.getId(), true)
        )).isInstanceOf(InvalidDepartmentRequestException.class);
    }

    @Test
    void createRejectsDuplicateCodeButAllowsDuplicateName() {
        departmentService.createDepartment(
                new DepartmentCreateRequestDTO("CSE", "컴퓨터공학과", engineering.getId(), true)
        );

        DepartmentResponseDTO sameName = departmentService.createDepartment(
                new DepartmentCreateRequestDTO("AIC", "컴퓨터공학과", engineering.getId(), true)
        );

        assertThat(sameName.code()).isEqualTo("AIC");
        assertThatThrownBy(() -> departmentService.createDepartment(
                new DepartmentCreateRequestDTO("CSE", "전산학과", null, true)
        )).isInstanceOf(DuplicateDepartmentException.class);
    }

    @Test
    void createRejectsInactiveCollegeWhenSpecified() {
        assertThatThrownBy(() -> departmentService.createDepartment(
                new DepartmentCreateRequestDTO("OLD", "폐지학과", inactiveCollege.getId(), false)
        )).isInstanceOf(InvalidDepartmentRequestException.class);
    }

    @Test
    void updateChangesOnlyProvidedFieldsAndNeverChangesCodeOrCollege() {
        DepartmentResponseDTO created = departmentService.createDepartment(
                new DepartmentCreateRequestDTO("CSE", "컴퓨터공학과", engineering.getId(), true)
        );

        DepartmentResponseDTO updated = departmentService.updateDepartment(
                created.id(),
                new DepartmentUpdateRequestDTO(" AI컴퓨터공학과 ", false)
        );

        assertThat(updated.code()).isEqualTo("CSE");
        assertThat(updated.name()).isEqualTo("AI컴퓨터공학과");
        assertThat(updated.college().id()).isEqualTo(engineering.getId());
        assertThat(updated.active()).isFalse();
    }

    @Test
    void updateRejectsEmptyPatch() {
        Department department = departmentRepository.saveAndFlush(
                Department.create("CSE", engineering, "컴퓨터공학과", true)
        );

        assertThatThrownBy(() -> departmentService.updateDepartment(
                department.getId(), new DepartmentUpdateRequestDTO(null, null)
        )).isInstanceOf(InvalidDepartmentRequestException.class);
    }

    @Test
    void updateRejectsActivationUnderInactiveCollege() {
        Department inactiveDepartment = departmentRepository.saveAndFlush(
                Department.create("OLD", inactiveCollege, "폐지학과", false)
        );

        assertThatThrownBy(() -> departmentService.updateDepartment(
                inactiveDepartment.getId(), new DepartmentUpdateRequestDTO(null, true)
        )).isInstanceOf(InvalidDepartmentRequestException.class);
    }

    @Test
    void updateAllowsDeactivationWithoutDeletingExistingRelationships() {
        Department department = departmentRepository.saveAndFlush(
                Department.create("CSE", engineering, "컴퓨터공학과", true)
        );

        DepartmentResponseDTO updated = departmentService.updateDepartment(
                department.getId(), new DepartmentUpdateRequestDTO(null, false)
        );

        assertThat(updated.active()).isFalse();
        assertThat(departmentRepository.existsById(department.getId())).isTrue();
    }
}
