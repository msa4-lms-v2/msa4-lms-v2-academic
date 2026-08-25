package com.msa4lmsv2academic.domain.enrollment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EnrollmentCreditLimitRuleQueryRepositoryIntegrationTest extends MySqlIntegrationTest {

    private static final long SEMESTER_ID = 98001L;

    @Autowired
    private EnrollmentCreditLimitRuleQueryRepository queryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, "
                + "enrollment_end_at, is_current) VALUES "
                + "(98001, 2027, 'FIRST', '2027-03-02', '2027-06-18', "
                + "'2027-02-08 09:00:00', '2027-02-12 18:00:00', 0)");
        jdbcTemplate.update("INSERT INTO enrollment_credit_limit_rules "
                + "(id, semester_id, max_credits, is_active) VALUES (98001, 98001, 18, 1)");
    }

    @Test
    void searchesRulesBySemesterAndActiveState() {
        EnrollmentCreditLimitRuleSearchResult result = queryRepository.search(
                new EnrollmentCreditLimitRuleSearchCondition(
                        0,
                        20,
                        (short) 2027,
                        SemesterTerm.FIRST,
                        true,
                        "academicYear",
                        true
                )
        );

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.items()).singleElement().satisfies(rule -> {
            assertThat(rule.getSemester().getId()).isEqualTo(SEMESTER_ID);
            assertThat(rule.getVersion()).isZero();
            assertThat(rule.getMaxCredits()).isEqualTo(18);
            assertThat(rule.isActive()).isTrue();
        });
    }

    @Test
    void enforcesSemesterUniquenessAndCreditRangeInDatabase() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO enrollment_credit_limit_rules "
                        + "(semester_id, max_credits, is_active) VALUES (98001, 21, 0)"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE enrollment_credit_limit_rules SET max_credits = 31 WHERE id = 98001"
        )).isInstanceOf(DataAccessException.class);
    }
}
