package com.msa4lmsv2academic.domain.organization.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

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
    void departmentCodeHasDatabaseLengthOfTwenty() {
        assertThatThrownBy(() -> departmentRepository.saveAndFlush(
                Department.create("123456789012345678901", engineering, "컴퓨터공학과", true)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateDepartmentNameIsAllowedWithinSameCollege() {
        departmentRepository.save(Department.create("CSE", engineering, "컴퓨터공학과", true));
        departmentRepository.saveAndFlush(Department.create("AIC", engineering, "컴퓨터공학과", true));

        assertThat(departmentRepository.count()).isEqualTo(2);
    }

    @Test
    void departmentKeepsNullableCollegeRelationship() {
        Department withCollege = departmentRepository.saveAndFlush(
                Department.create("CSE", engineering, "컴퓨터공학과", true)
        );
        Department withoutCollege = departmentRepository.saveAndFlush(
                Department.create("FREE", null, "자유전공학부", true)
        );
        entityManager.clear();

        Department foundWithCollege = departmentQueryRepository.findByIdWithCollege(withCollege.getId())
                .orElseThrow();
        Department foundWithoutCollege = departmentQueryRepository.findByIdWithCollege(withoutCollege.getId())
                .orElseThrow();

        assertThat(foundWithCollege.getCollege().getId()).isEqualTo(engineering.getId());
        assertThat(foundWithoutCollege.getCollege()).isNull();
    }

    @Test
    void queryDslSearchCombinesRoleScopeCollegeActiveAndKeyword() {
        departmentRepository.saveAllAndFlush(List.of(
                Department.create("CSE", engineering, "컴퓨터공학과", true),
                Department.create("MEC", engineering, "기계공학과", false),
                Department.create("KOR", humanities, "국어국문학과", true),
                Department.create("OLD", inactiveCollege, "폐지학과", true),
                Department.create("FREE", null, "자유전공학부", true)
        ));
        entityManager.clear();

        DepartmentSearchResult studentCodeResult = departmentQueryRepository.search(
                new DepartmentSearchCondition(0, 20, engineering.getId(), false, " CSE ", false)
        );
        DepartmentSearchResult studentAllResult = departmentQueryRepository.search(
                new DepartmentSearchCondition(0, 20, null, null, null, false)
        );
        DepartmentSearchResult adminInactiveResult = departmentQueryRepository.search(
                new DepartmentSearchCondition(0, 20, engineering.getId(), false, "공학", true)
        );

        assertThat(studentCodeResult.items()).extracting(Department::getCode).containsExactly("CSE");
        assertThat(studentAllResult.items()).extracting(Department::getCode)
                .containsExactly("CSE", "FREE", "KOR");
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
