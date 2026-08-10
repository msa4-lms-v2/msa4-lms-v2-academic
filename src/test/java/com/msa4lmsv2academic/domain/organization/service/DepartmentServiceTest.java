package com.msa4lmsv2academic.domain.organization.service;

import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.error.DuplicateDepartmentException;
import com.msa4lmsv2academic.domain.organization.error.InvalidDepartmentRequestException;
import com.msa4lmsv2academic.domain.organization.repository.CollegeRepository;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentRepository;
import com.msa4lmsv2academic.domain.organization.request.DepartmentCreateRequestDTO;
import com.msa4lmsv2academic.domain.organization.request.DepartmentUpdateRequestDTO;
import com.msa4lmsv2academic.domain.organization.response.DepartmentResponseDTO;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    private College humanities;
    private College inactiveCollege;

    @BeforeEach
    void setUp() {
        departmentRepository.deleteAllInBatch();
        collegeRepository.deleteAllInBatch();

        engineering = collegeRepository.save(College.create("ENG", "공과대학", true));
        humanities = collegeRepository.save(College.create("HUM", "인문대학", true));
        inactiveCollege = collegeRepository.save(College.create("OLD", "폐지대학", false));
    }

    @Test
    void createDefaultsActiveToTrueAndNormalizesCodeAndName() {
        DepartmentResponseDTO created = departmentService.createDepartment(
                new DepartmentCreateRequestDTO(" cse ", " 컴퓨터공학과 ", engineering.getId(), null)
        );

        assertThat(created.code()).isEqualTo("CSE");
        assertThat(created.name()).isEqualTo("컴퓨터공학과");
        assertThat(created.active()).isTrue();
    }

    @Test
    void createAllowsExplicitInactiveDepartment() {
        DepartmentResponseDTO created = departmentService.createDepartment(
                new DepartmentCreateRequestDTO("CSE", "컴퓨터공학과", engineering.getId(), false)
        );

        assertThat(created.active()).isFalse();
    }

    @Test
    void createRejectsInvalidCodeFormat() {
        assertThatThrownBy(() -> departmentService.createDepartment(
                new DepartmentCreateRequestDTO("CSE_01", "컴퓨터공학과", engineering.getId(), true)
        )).isInstanceOf(InvalidDepartmentRequestException.class);
    }

    @Test
    void createRejectsDuplicateCodeAndDuplicateNameWithinCollege() {
        departmentService.createDepartment(
                new DepartmentCreateRequestDTO("CSE", "컴퓨터공학과", engineering.getId(), true)
        );

        assertThatThrownBy(() -> departmentService.createDepartment(
                new DepartmentCreateRequestDTO("AIC", "컴퓨터공학과", engineering.getId(), true)
        )).isInstanceOf(DuplicateDepartmentException.class);
        assertThatThrownBy(() -> departmentService.createDepartment(
                new DepartmentCreateRequestDTO("CSE", "전산학과", humanities.getId(), true)
        )).isInstanceOf(DuplicateDepartmentException.class);
    }

    @Test
    void createRejectsInactiveCollege() {
        assertThatThrownBy(() -> departmentService.createDepartment(
                new DepartmentCreateRequestDTO("OLD", "폐지학과", inactiveCollege.getId(), false)
        )).isInstanceOf(InvalidDepartmentRequestException.class);
    }

    @Test
    void updateChangesOnlyProvidedFieldsAndNeverChangesCode() {
        DepartmentResponseDTO created = departmentService.createDepartment(
                new DepartmentCreateRequestDTO("CSE", "컴퓨터공학과", engineering.getId(), true)
        );

        DepartmentResponseDTO updated = departmentService.updateDepartment(
                created.id(),
                new DepartmentUpdateRequestDTO(" AI컴퓨터공학과 ", null, false)
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
                department.getId(), new DepartmentUpdateRequestDTO(null, null, null)
        )).isInstanceOf(InvalidDepartmentRequestException.class);
    }

    @Test
    void updateValidatesFinalCollegeAndNameCombinationWhenMovingAndRenaming() {
        Department moving = departmentRepository.saveAndFlush(
                Department.create("CSE", engineering, "컴퓨터공학과", true)
        );
        departmentRepository.saveAndFlush(
                Department.create("KOR", humanities, "국어국문학과", true)
        );

        assertThatThrownBy(() -> departmentService.updateDepartment(
                moving.getId(),
                new DepartmentUpdateRequestDTO("국어국문학과", humanities.getId(), null)
        )).isInstanceOf(DuplicateDepartmentException.class);
    }

    @Test
    void updateRejectsMoveToInactiveCollegeAndActivationUnderInactiveCollege() {
        Department department = departmentRepository.saveAndFlush(
                Department.create("CSE", engineering, "컴퓨터공학과", true)
        );
        Department inactiveDepartment = departmentRepository.saveAndFlush(
                Department.create("OLD", inactiveCollege, "폐지학과", false)
        );

        assertThatThrownBy(() -> departmentService.updateDepartment(
                department.getId(), new DepartmentUpdateRequestDTO(null, inactiveCollege.getId(), null)
        )).isInstanceOf(InvalidDepartmentRequestException.class);
        assertThatThrownBy(() -> departmentService.updateDepartment(
                inactiveDepartment.getId(), new DepartmentUpdateRequestDTO(null, null, true)
        )).isInstanceOf(InvalidDepartmentRequestException.class);
    }

    @Test
    void updateAllowsDeactivationWithoutDeletingExistingRelationships() {
        Department department = departmentRepository.saveAndFlush(
                Department.create("CSE", engineering, "컴퓨터공학과", true)
        );

        DepartmentResponseDTO updated = departmentService.updateDepartment(
                department.getId(), new DepartmentUpdateRequestDTO(null, null, false)
        );

        assertThat(updated.active()).isFalse();
        assertThat(departmentRepository.existsById(department.getId())).isTrue();
    }
}
