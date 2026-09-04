package com.msa4lmsv2academic.domain.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import com.msa4lmsv2academic.domain.attendance.repository.ExcuseRequestQueryRepository;
import com.msa4lmsv2academic.domain.attendance.repository.ExcuseRequestQueryResult;
import com.msa4lmsv2academic.domain.attendance.repository.ExcuseRequestSearchResult;
import com.msa4lmsv2academic.domain.attendance.request.ExcuseRequestSearchRequestDTO;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.global.error.ExcuseRequestAccessDeniedException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExcuseRequestQueryServiceTest {

    @Test
    void returnsRoleScopedStatusPageWithResponseFields() {
        ExcuseRequestQueryRepository repository = mock(ExcuseRequestQueryRepository.class);
        ExcuseRequestQueryResult queryResult = queryResult();
        when(repository.search(10001L, UserRole.STUDENT, ExcuseRequestStatus.PENDING, 20L, 20))
                .thenReturn(new ExcuseRequestSearchResult(List.of(queryResult), 21L));
        ExcuseRequestQueryService service = new ExcuseRequestQueryService(repository);

        var response = service.search(
                new ExcuseRequestSearchRequestDTO(2, 20, ExcuseRequestStatus.PENDING),
                new CurrentUser(10001L, "STUDENT")
        );

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalCount()).isEqualTo(21L);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(301L);
            assertThat(item.studentName()).isEqualTo("김미래");
            assertThat(item.courseName()).isEqualTo("운영체제");
            assertThat(item.status()).isEqualTo(ExcuseRequestStatus.PENDING);
            assertThat(item.attachmentOriginalName()).isEqualTo("진료확인서.pdf");
        });
        verify(repository).search(10001L, UserRole.STUDENT, ExcuseRequestStatus.PENDING, 20L, 20);
    }

    @Test
    void allowsProfessorAndAdministratorScopes() {
        ExcuseRequestQueryRepository repository = mock(ExcuseRequestQueryRepository.class);
        when(repository.search(11001L, UserRole.PROFESSOR, null, 0L, 20))
                .thenReturn(new ExcuseRequestSearchResult(List.of(), 0L));
        when(repository.search(12001L, UserRole.ADMIN, null, 0L, 20))
                .thenReturn(new ExcuseRequestSearchResult(List.of(), 0L));
        ExcuseRequestQueryService service = new ExcuseRequestQueryService(repository);
        ExcuseRequestSearchRequestDTO request = new ExcuseRequestSearchRequestDTO(null, null, null);

        assertThat(service.search(request, new CurrentUser(11001L, "PROFESSOR")).items()).isEmpty();
        assertThat(service.search(request, new CurrentUser(12001L, "ADMIN")).items()).isEmpty();

        verify(repository).search(11001L, UserRole.PROFESSOR, null, 0L, 20);
        verify(repository).search(12001L, UserRole.ADMIN, null, 0L, 20);
    }

    @Test
    void returnsEmptyPageAndClampsPageSize() {
        ExcuseRequestQueryRepository repository = mock(ExcuseRequestQueryRepository.class);
        when(repository.search(10001L, UserRole.STUDENT, null, 0L, 100))
                .thenReturn(new ExcuseRequestSearchResult(List.of(), 0L));
        ExcuseRequestQueryService service = new ExcuseRequestQueryService(repository);

        var response = service.search(
                new ExcuseRequestSearchRequestDTO(1, 500, null),
                new CurrentUser(10001L, "STUDENT")
        );

        assertThat(response.items()).isEmpty();
        assertThat(response.totalCount()).isZero();
        assertThat(response.size()).isEqualTo(100);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void rejectsUnsupportedRole() {
        ExcuseRequestQueryService service = new ExcuseRequestQueryService(
                mock(ExcuseRequestQueryRepository.class)
        );

        assertThatThrownBy(() -> service.search(
                new ExcuseRequestSearchRequestDTO(null, null, null),
                new CurrentUser(13001L, "SYSTEM")
        )).isInstanceOf(ExcuseRequestAccessDeniedException.class);
    }

    private ExcuseRequestQueryResult queryResult() {
        return new ExcuseRequestQueryResult(
                301L,
                12001L,
                2001L,
                10001L,
                "김미래",
                501L,
                101L,
                "CSE301",
                "운영체제",
                "01",
                3001L,
                11001L,
                "홍길동",
                LocalDate.of(2026, 9, 1),
                (byte) 2,
                "병원 진료",
                ExcuseRequestStatus.PENDING,
                null,
                "진료확인서.pdf",
                "application/pdf",
                2048L,
                LocalDateTime.of(2026, 9, 2, 10, 30),
                LocalDateTime.of(2026, 9, 2, 10, 30)
        );
    }
}
