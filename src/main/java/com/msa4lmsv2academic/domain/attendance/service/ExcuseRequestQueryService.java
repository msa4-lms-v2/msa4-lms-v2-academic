package com.msa4lmsv2academic.domain.attendance.service;

import com.msa4lmsv2academic.domain.attendance.repository.ExcuseRequestQueryRepository;
import com.msa4lmsv2academic.domain.attendance.repository.ExcuseRequestSearchResult;
import com.msa4lmsv2academic.domain.attendance.request.ExcuseRequestSearchRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseRequestStatusResponseDTO;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.global.error.ExcuseRequestAccessDeniedException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcuseRequestQueryService {

    private final ExcuseRequestQueryRepository excuseRequestQueryRepository;

    public PageResponseDTO<ExcuseRequestStatusResponseDTO> search(
            ExcuseRequestSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        UserRole role = validateAndResolveRole(currentUser);
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (long) (page - 1) * size;

        ExcuseRequestSearchResult result = excuseRequestQueryRepository.search(
                currentUser.id(),
                role,
                request.status(),
                offset,
                size
        );
        List<ExcuseRequestStatusResponseDTO> items = result.items().stream()
                .map(ExcuseRequestStatusResponseDTO::from)
                .toList();
        boolean hasNext = offset + items.size() < result.totalCount();

        return new PageResponseDTO<>(items, result.totalCount(), page, size, hasNext);
    }

    private UserRole validateAndResolveRole(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.role() == null) {
            throw new ExcuseRequestAccessDeniedException("공결 처리 상태를 조회할 권한이 없습니다.");
        }

        try {
            return UserRole.valueOf(currentUser.role());
        } catch (IllegalArgumentException exception) {
            throw new ExcuseRequestAccessDeniedException("공결 처리 상태를 조회할 권한이 없습니다.");
        }
    }
}
