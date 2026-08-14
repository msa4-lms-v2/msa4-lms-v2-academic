package com.msa4lmsv2academic.domain.semester.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SemesterCurrentUniquenessTest extends MySqlIntegrationTest {

    @Autowired
    private SemesterRepository semesterRepository;

    @Test
    void databaseRejectsSecondCurrentSemester() {
        semesterRepository.saveAndFlush(semester((short) 2026, SemesterTerm.FIRST));

        assertThatThrownBy(() -> semesterRepository.saveAndFlush(
                semester((short) 2026, SemesterTerm.SECOND)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Semester semester(short year, SemesterTerm term) {
        int startMonth = term == SemesterTerm.FIRST ? 3 : 9;
        int endMonth = term == SemesterTerm.FIRST ? 6 : 12;
        int enrollmentMonth = term == SemesterTerm.FIRST ? 2 : 8;
        return Semester.create(
                year,
                term,
                LocalDate.of(year, startMonth, 2),
                LocalDate.of(year, endMonth, 18),
                LocalDateTime.of(year, enrollmentMonth, 16, 9, 0),
                LocalDateTime.of(year, enrollmentMonth, 20, 18, 0),
                true
        );
    }
}
