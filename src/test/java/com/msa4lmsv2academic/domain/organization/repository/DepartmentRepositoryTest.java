package com.msa4lmsv2academic.domain.organization.repository;

import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class DepartmentRepositoryTest extends MySqlIntegrationTest {

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DepartmentQueryRepository departmentQueryRepository;

    @Autowired
    private EntityManager entityManager;

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
    void departmentCodeHasDatabaseUniqueConstraint() {
        departmentRepository.saveAndFlush(Department.create("CSE", engineering, "컴퓨터공학과", true));

        assertThatThrownBy(() -> departmentRepository.saveAndFlush(
                Department.create("CSE", humanities, "다른컴퓨터학과", true)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void departmentNameMustBeUniqueWithinCollege() {
        departmentRepository.saveAndFlush(Department.create("CSE", engineering, "컴퓨터공학과", true));

        assertThatThrownBy(() -> departmentRepository.saveAndFlush(
                Department.create("AIC", engineering, "컴퓨터공학과", true)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameDepartmentNameIsAllowedInDifferentColleges() {
        departmentRepository.save(Department.create("CSE", engineering, "컴퓨터공학과", true));
        departmentRepository.saveAndFlush(Department.create("HCS", humanities, "컴퓨터공학과", true));

        assertThat(departmentRepository.count()).isEqualTo(2);
    }

    @Test
    void departmentKeepsCollegeForeignKeyRelationship() {
        Department saved = departmentRepository.saveAndFlush(
                Department.create("CSE", engineering, "컴퓨터공학과", true)
        );
        entityManager.clear();

        Department found = departmentQueryRepository.findByIdWithCollege(saved.getId()).orElseThrow();

        assertThat(found.getCollege().getId()).isEqualTo(engineering.getId());
        assertThat(found.getCollege().getCode()).isEqualTo("ENG");
    }

    @Test
    void queryDslSearchCombinesRoleScopeCollegeActiveAndKeyword() {
        departmentRepository.saveAllAndFlush(List.of(
                Department.create("CSE", engineering, "컴퓨터공학과", true),
                Department.create("MEC", engineering, "기계공학과", false),
                Department.create("KOR", humanities, "국어국문학과", true),
                Department.create("OLD", inactiveCollege, "폐지학과", true)
        ));
        entityManager.clear();

        DepartmentSearchResult studentResult = departmentQueryRepository.search(
                new DepartmentSearchCondition(0, 20, engineering.getId(), false, " cse ", false)
        );
        DepartmentSearchResult adminInactiveResult = departmentQueryRepository.search(
                new DepartmentSearchCondition(0, 20, engineering.getId(), false, "공학", true)
        );

        assertThat(studentResult.items()).extracting(Department::getCode).containsExactly("CSE");
        assertThat(adminInactiveResult.items()).extracting(Department::getCode).containsExactly("MEC");
    }

    @Test
    void searchSortsByCodeAndSupportsStablePagingWithoutNPlusOne() {
        departmentRepository.saveAllAndFlush(List.of(
                Department.create("ZOO", engineering, "동물공학과", true),
                Department.create("AIC", engineering, "인공지능학과", true),
                Department.create("CSE", engineering, "컴퓨터공학과", true)
        ));
        entityManager.clear();

        DepartmentSearchResult firstPage = departmentQueryRepository.search(
                new DepartmentSearchCondition(0, 2, null, null, null, true)
        );
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertThat(firstPage.items()).extracting(Department::getCode).containsExactly("AIC", "CSE");
        assertThat(firstPage.totalCount()).isEqualTo(3);
        assertThat(firstPage.items()).allMatch(item -> persistenceUnitUtil.isLoaded(item, "college"));
    }
}
