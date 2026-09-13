package com.msa4lmsv2academic.domain.infochange.service;

import com.msa4lmsv2academic.domain.infochange.repository.AdminInfoChangeRequestQueryRepository;
import com.msa4lmsv2academic.domain.infochange.repository.AdminInfoChangeRequestSearchResult;
import com.msa4lmsv2academic.domain.infochange.request.AdminInfoChangeRequestSearchRequestDTO;
import com.msa4lmsv2academic.domain.infochange.response.AdminInfoChangeRequestSummaryResponseDTO;
import com.msa4lmsv2academic.global.error.InfoChangeRequestAccessDeniedException;
import com.msa4lmsv2academic.global.error.InvalidInfoChangeRequestException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInfoChangeRequestService {

    private final AdminInfoChangeRequestQueryRepository queryRepository;

    public PageResponseDTO<AdminInfoChangeRequestSummaryResponseDTO> search(
            AdminInfoChangeRequestSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new InfoChangeRequestAccessDeniedException("관리자만 통합 정보 변경 신청 목록을 조회할 수 있습니다.");
        }
        if (request.requestedFrom() != null && request.requestedTo() != null
                && request.requestedFrom().isAfter(request.requestedTo())) {
            throw new InvalidInfoChangeRequestException("신청일 시작은 종료일보다 늦을 수 없습니다.");
        }

        int page = request.resolvedPage();
        int size = request.resolvedSize();
        AdminInfoChangeRequestSearchResult result = queryRepository.search(request, (page - 1L) * size, size);
        boolean hasNext = (page - 1L) * size + result.items().size() < result.totalCount();
        return new PageResponseDTO<>(result.items(), result.totalCount(), page, size, hasNext);
    }
}
