package com.msa4lmsv2academic.domain.infochange.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.infochange.request.AdminInfoChangeRequestSearchRequestDTO;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AdminInfoChangeRequestQueryRepositoryIntegrationTest extends MySqlIntegrationTest {

    @Autowired
    private AdminInfoChangeRequestQueryRepository queryRepository;

    @Test
    void searchesStudentAndProfessorRequestsWithNullableFilters() {
        AdminInfoChangeRequestSearchResult result = queryRepository.search(
                new AdminInfoChangeRequestSearchRequestDTO(null, null, null, null, null, 1, 20),
                0,
                20
        );

        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isZero();
    }
}
