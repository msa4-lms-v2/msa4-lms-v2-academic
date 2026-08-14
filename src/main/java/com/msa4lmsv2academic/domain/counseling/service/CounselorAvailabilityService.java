package com.msa4lmsv2academic.domain.counseling.service;

import com.msa4lmsv2academic.domain.counseling.entity.CounselorAvailability;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingParticipantQueryRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselorAvailabilityRepository;
import com.msa4lmsv2academic.domain.counseling.request.CounselorAvailabilityReplaceRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselorAvailabilitySlotRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselorAvailabilityResponseDTO;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.global.error.CounselingAccessDeniedException;
import com.msa4lmsv2academic.global.error.CounselingParticipantNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidCounselingRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CounselorAvailabilityService {

    private final CounselorAvailabilityRepository availabilityRepository;
    private final CounselingParticipantQueryRepository participantQueryRepository;

    public List<CounselorAvailabilityResponseDTO> getAvailabilities(
            Long professorId,
            CurrentUser currentUser
    ) {
        validateAuthenticatedRole(currentUser);

        Long resolvedProfessorId = professorId;
        if (resolvedProfessorId == null && "PROFESSOR".equals(currentUser.role())) {
            resolvedProfessorId = participantQueryRepository.findProfessorByUserId(currentUser.id())
                    .map(Professor::getId)
                    .orElseThrow(() -> new CounselingParticipantNotFoundException("교수 정보를 찾을 수 없습니다."));
        }

        List<CounselorAvailability> availabilities = resolvedProfessorId == null
                ? availabilityRepository.findAllByOrderByProfessorIdAscDayOfWeekAscStartTimeAsc()
                : availabilityRepository.findByProfessorIdOrderByDayOfWeekAscStartTimeAsc(resolvedProfessorId);

        return availabilities.stream()
                .map(CounselorAvailabilityResponseDTO::from)
                .toList();
    }

    @Transactional
    public List<CounselorAvailabilityResponseDTO> replaceAvailabilities(
            CounselorAvailabilityReplaceRequestDTO request,
            CurrentUser currentUser
    ) {
        validateProfessor(currentUser);
        List<CounselorAvailabilitySlotRequestDTO> slots = request.slots();
        validateSlots(slots);

        Professor professor = participantQueryRepository.findProfessorByUserIdForUpdate(currentUser.id())
                .orElseThrow(() -> new CounselingParticipantNotFoundException("교수 정보를 찾을 수 없습니다."));

        availabilityRepository.deleteByProfessorId(professor.getId());
        List<CounselorAvailability> replacements = slots.stream()
                .map(slot -> CounselorAvailability.create(
                        professor,
                        slot.dayOfWeek(),
                        slot.startTime(),
                        slot.endTime(),
                        slot.validFrom(),
                        slot.validTo()
                ))
                .toList();

        return availabilityRepository.saveAllAndFlush(replacements).stream()
                .sorted(Comparator.comparing(CounselorAvailability::getDayOfWeek)
                        .thenComparing(CounselorAvailability::getStartTime))
                .map(CounselorAvailabilityResponseDTO::from)
                .toList();
    }

    private void validateSlots(List<CounselorAvailabilitySlotRequestDTO> slots) {
        if (slots == null) {
            throw new InvalidCounselingRequestException("상담 가능 시간 목록은 필수입니다.");
        }

        List<CounselorAvailabilitySlotRequestDTO> validated = new ArrayList<>();
        for (CounselorAvailabilitySlotRequestDTO slot : slots) {
            if (slot == null || slot.dayOfWeek() == null || slot.validFrom() == null) {
                throw new InvalidCounselingRequestException("요일과 유효 시작일은 필수입니다.");
            }
            LocalTime start = parseTime(slot.startTime());
            LocalTime end = parseTime(slot.endTime());
            if (!start.isBefore(end)) {
                throw new InvalidCounselingRequestException("상담 가능 시간의 종료 시각은 시작 시각보다 늦어야 합니다.");
            }
            if (slot.validTo() != null && slot.validTo().isBefore(slot.validFrom())) {
                throw new InvalidCounselingRequestException("유효 종료일은 유효 시작일보다 빠를 수 없습니다.");
            }
            if (validated.stream().anyMatch(existing -> overlaps(existing, slot))) {
                throw new InvalidCounselingRequestException("서로 겹치는 상담 가능 시간을 등록할 수 없습니다.");
            }
            validated.add(slot);
        }
    }

    private boolean overlaps(
            CounselorAvailabilitySlotRequestDTO first,
            CounselorAvailabilitySlotRequestDTO second
    ) {
        if (first.dayOfWeek() != second.dayOfWeek()) {
            return false;
        }
        if (!dateRangesOverlap(first.validFrom(), first.validTo(), second.validFrom(), second.validTo())) {
            return false;
        }
        LocalTime firstStart = parseTime(first.startTime());
        LocalTime firstEnd = parseTime(first.endTime());
        LocalTime secondStart = parseTime(second.startTime());
        LocalTime secondEnd = parseTime(second.endTime());
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }

    private boolean dateRangesOverlap(
            LocalDate firstStart,
            LocalDate firstEnd,
            LocalDate secondStart,
            LocalDate secondEnd
    ) {
        boolean firstStartsBeforeSecondEnds = secondEnd == null || !firstStart.isAfter(secondEnd);
        boolean secondStartsBeforeFirstEnds = firstEnd == null || !secondStart.isAfter(firstEnd);
        return firstStartsBeforeSecondEnds && secondStartsBeforeFirstEnds;
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (RuntimeException exception) {
            throw new InvalidCounselingRequestException("상담 가능 시각은 HH:mm 형식이어야 합니다.");
        }
    }

    private void validateAuthenticatedRole(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null
                || !("STUDENT".equals(currentUser.role())
                || "PROFESSOR".equals(currentUser.role())
                || "ADMIN".equals(currentUser.role()))) {
            throw new CounselingAccessDeniedException("상담 가능 시간을 조회할 권한이 없습니다.");
        }
    }

    private void validateProfessor(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"PROFESSOR".equals(currentUser.role())) {
            throw new CounselingAccessDeniedException("교수만 상담 가능 시간을 변경할 수 있습니다.");
        }
    }
}
