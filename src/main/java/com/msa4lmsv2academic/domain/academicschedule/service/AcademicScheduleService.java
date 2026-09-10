package com.msa4lmsv2academic.domain.academicschedule.service;

import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicSchedule;
import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicScheduleCategory;
import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicScheduleTargetRole;
import com.msa4lmsv2academic.domain.academicschedule.repository.AcademicScheduleQueryRepository;
import com.msa4lmsv2academic.domain.academicschedule.repository.AcademicScheduleRepository;
import com.msa4lmsv2academic.domain.academicschedule.repository.AcademicScheduleSearchCondition;
import com.msa4lmsv2academic.domain.academicschedule.repository.AcademicScheduleSearchResult;
import com.msa4lmsv2academic.domain.academicschedule.request.AcademicScheduleCreateRequestDTO;
import com.msa4lmsv2academic.domain.academicschedule.request.AcademicScheduleSearchRequestDTO;
import com.msa4lmsv2academic.domain.academicschedule.request.AcademicScheduleStatusRequestDTO;
import com.msa4lmsv2academic.domain.academicschedule.request.AcademicScheduleUpdateRequestDTO;
import com.msa4lmsv2academic.domain.academicschedule.response.AcademicScheduleDetailResponseDTO;
import com.msa4lmsv2academic.domain.academicschedule.response.AcademicScheduleSummaryResponseDTO;
import com.msa4lmsv2academic.domain.academicschedule.response.AcademicScheduleTemplateResponseDTO;
import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.coursecorrection.entity.CourseCorrectionPeriod;
import com.msa4lmsv2academic.domain.coursecorrection.repository.CourseCorrectionPeriodRepository;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestPeriod;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import com.msa4lmsv2academic.domain.leaverequest.repository.LeavePeriodRepository;
import com.msa4lmsv2academic.domain.outbox.service.OutboxEventService;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.error.AcademicScheduleAccessDeniedException;
import com.msa4lmsv2academic.global.error.AcademicScheduleAuthorNotFoundException;
import com.msa4lmsv2academic.global.error.AcademicScheduleNotFoundException;
import com.msa4lmsv2academic.global.error.DuplicateAcademicScheduleException;
import com.msa4lmsv2academic.global.error.InvalidAcademicScheduleRequestException;
import com.msa4lmsv2academic.global.error.SemesterNotFoundException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicScheduleService {

    private static final String TARGET_TYPE = "ACADEMIC_SCHEDULE";
    private static final String CREATE_ACTION = "ACADEMIC_SCHEDULE_CREATE";
    private static final String UPDATE_ACTION = "ACADEMIC_SCHEDULE_UPDATE";
    private static final String STATUS_ACTION = "ACADEMIC_SCHEDULE_STATUS_CHANGE";
    private static final String AGGREGATE_TYPE = "ACADEMIC_SCHEDULE";
    private static final String SCHOLARSHIP_PERIOD_CHANGED = "ScholarshipApplicationPeriodChanged";

    private final AcademicScheduleRepository academicScheduleRepository;
    private final AcademicScheduleQueryRepository academicScheduleQueryRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final SemesterRepository semesterRepository;
    private final LeavePeriodRepository leavePeriodRepository;
    private final CourseCorrectionPeriodRepository courseCorrectionPeriodRepository;
    private final OutboxEventService outboxEventService;

    public PageResponseDTO<AcademicScheduleSummaryResponseDTO> search(
            AcademicScheduleSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateSearchRequest(request);
        AcademicScheduleTargetRole userRole = resolveUserRole(currentUser);
        boolean admin = currentUser.isAdmin();
        Set<AcademicScheduleTargetRole> targetRoles = resolveTargetRoles(request.targetRole(), userRole, admin);
        Boolean active = admin ? request.active() : Boolean.TRUE;

        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        AcademicScheduleSearchResult result = academicScheduleQueryRepository.search(
                new AcademicScheduleSearchCondition(
                        offset,
                        size,
                        normalizeKeyword(request.keyword()),
                        request.from(),
                        request.to(),
                        targetRoles,
                        request.category(),
                        request.academicYear(),
                        request.term(),
                        active
                )
        );
        List<AcademicScheduleSummaryResponseDTO> items = result.items().stream()
                .map(AcademicScheduleSummaryResponseDTO::from)
                .toList();
        boolean hasNext = offset + items.size() < result.totalCount();
        return new PageResponseDTO<>(items, result.totalCount(), page, size, hasNext);
    }

    public AcademicScheduleDetailResponseDTO get(Long scheduleId, CurrentUser currentUser) {
        AcademicSchedule schedule = findSchedule(scheduleId);
        if (currentUser == null) {
            throw new AcademicScheduleAccessDeniedException();
        }
        if (currentUser.isAdmin()) {
            return AcademicScheduleDetailResponseDTO.from(schedule);
        }

        AcademicScheduleTargetRole userRole = resolveUserRole(currentUser);
        if (!schedule.isActive()) {
            throw new AcademicScheduleNotFoundException();
        }
        if (schedule.getTargetRole() != AcademicScheduleTargetRole.ALL
                && schedule.getTargetRole() != userRole) {
            throw new AcademicScheduleAccessDeniedException();
        }
        return AcademicScheduleDetailResponseDTO.from(schedule);
    }

    public List<AcademicScheduleTemplateResponseDTO> getTemplates(CurrentUser currentUser) {
        validateAdmin(currentUser);
        return List.of(AcademicScheduleCategory.values()).stream()
                .map(AcademicScheduleTemplateResponseDTO::from)
                .toList();
    }

    @Transactional
    public AcademicScheduleDetailResponseDTO create(AcademicScheduleCreateRequestDTO request,
                                                    CurrentUser currentUser, String requestId, String ipAddress) {
        validateAdmin(currentUser);
        validateCreateRequest(request);

        String title = normalizeTitle(request.title());
        String content = normalizeContent(request.content());
        validateDateRange(request.startDate(), request.endDate());
        validateOperationalPeriodDate(request.category(), request.endDate());
        TermAssignment assignment = resolveTermAssignment(request.startDate(), request.endDate());
        validateNoDuplicate(null, title, content, request.startDate(), request.endDate(), request.targetRole());

        User author = userRepository.findById(currentUser.id())
                .orElseThrow(AcademicScheduleAuthorNotFoundException::new);
        AcademicSchedule saved = academicScheduleRepository.saveAndFlush(
                AcademicSchedule.create(
                        title,
                        content,
                        request.category(),
                        request.startDate(),
                        request.endDate(),
                        assignment.academicYear(),
                        assignment.term(),
                        request.targetRole(),
                        author
                )
        );
        applyOperationalPeriod(saved, assignment);
        auditLogService.record(
                currentUser.id(), CREATE_ACTION, TARGET_TYPE, saved.getId(), null, snapshot(saved), null,
                normalizeNullable(requestId), normalizeNullable(ipAddress)
        );
        return AcademicScheduleDetailResponseDTO.from(saved);
    }

    @Transactional
    public AcademicScheduleDetailResponseDTO update(Long scheduleId, AcademicScheduleUpdateRequestDTO request,
                                                    CurrentUser currentUser, String requestId, String ipAddress) {
        validateAdmin(currentUser);
        validateUpdateRequest(request);

        AcademicSchedule schedule = findSchedule(scheduleId);
        String title = normalizeTitle(request.title());
        String content = normalizeContent(request.content());
        String reason = normalizeRequiredReason(request.reason());
        validateDateRange(request.startDate(), request.endDate());
        validateOperationalPeriodDate(request.category(), request.endDate());
        TermAssignment assignment = resolveTermAssignment(request.startDate(), request.endDate());

        if (schedule.getCategory() != request.category()) {
            throw new InvalidAcademicScheduleRequestException("일정 분류는 등록 후 변경할 수 없습니다.");
        }
        if (isSameSchedule(schedule, title, content, request.startDate(), request.endDate(), request.targetRole())) {
            return AcademicScheduleDetailResponseDTO.from(schedule);
        }
        if (schedule.isActive()) {
            validateNoDuplicate(scheduleId, title, content, request.startDate(), request.endDate(), request.targetRole());
        }

        Map<String, Object> beforeValue = snapshot(schedule);
        schedule.update(title, content, request.category(), request.startDate(), request.endDate(),
                assignment.academicYear(), assignment.term(), request.targetRole());
        AcademicSchedule saved = academicScheduleRepository.saveAndFlush(schedule);
        applyOperationalPeriod(saved, assignment);
        auditLogService.record(
                currentUser.id(), UPDATE_ACTION, TARGET_TYPE, saved.getId(), beforeValue, snapshot(saved), reason,
                normalizeNullable(requestId), normalizeNullable(ipAddress)
        );
        return AcademicScheduleDetailResponseDTO.from(saved);
    }

    @Transactional
    public AcademicScheduleDetailResponseDTO changeStatus(Long scheduleId, AcademicScheduleStatusRequestDTO request,
                                                          CurrentUser currentUser, String requestId, String ipAddress) {
        validateAdmin(currentUser);
        validateStatusRequest(request);

        AcademicSchedule schedule = findSchedule(scheduleId);
        if (schedule.isActive() == request.active()) {
            return AcademicScheduleDetailResponseDTO.from(schedule);
        }
        if (request.active()) {
            validateNoDuplicate(
                    scheduleId,
                    schedule.getTitle(),
                    schedule.getContent(),
                    schedule.getStartDate(),
                    schedule.getEndDate(),
                    schedule.getTargetRole()
            );
        }

        Map<String, Object> beforeValue = snapshot(schedule);
        schedule.changeActive(request.active());
        AcademicSchedule saved = academicScheduleRepository.saveAndFlush(schedule);
        if (saved.getCategory() == AcademicScheduleCategory.SCHOLARSHIP) {
            applyOperationalPeriod(saved, new TermAssignment(saved.getAcademicYear(), saved.getTerm()));
        }
        auditLogService.record(
                currentUser.id(), STATUS_ACTION, TARGET_TYPE, saved.getId(), beforeValue, snapshot(saved),
                normalizeRequiredReason(request.reason()), normalizeNullable(requestId), normalizeNullable(ipAddress)
        );
        return AcademicScheduleDetailResponseDTO.from(saved);
    }

    private void validateSearchRequest(AcademicScheduleSearchRequestDTO request) {
        if (request == null) {
            throw new InvalidAcademicScheduleRequestException("학사일정 검색 조건이 필요합니다.");
        }
        if (request.from() != null && request.to() != null && request.from().isAfter(request.to())) {
            throw new InvalidAcademicScheduleRequestException("from은 to보다 늦을 수 없습니다.");
        }
    }

    private void validateCreateRequest(AcademicScheduleCreateRequestDTO request) {
        if (request == null || request.title() == null || request.title().isBlank()
                || request.category() == null || request.startDate() == null || request.targetRole() == null) {
            throw new InvalidAcademicScheduleRequestException("title, category, startDate, targetRole은 필수입니다.");
        }
        validateLengths(request.title(), request.content());
    }

    private void validateUpdateRequest(AcademicScheduleUpdateRequestDTO request) {
        if (request == null || request.title() == null || request.title().isBlank()
                || request.category() == null || request.startDate() == null || request.targetRole() == null
                || request.reason() == null || request.reason().isBlank()) {
            throw new InvalidAcademicScheduleRequestException(
                    "title, category, startDate, targetRole, reason은 필수입니다."
            );
        }
        validateLengths(request.title(), request.content());
        normalizeRequiredReason(request.reason());
    }

    private void validateStatusRequest(AcademicScheduleStatusRequestDTO request) {
        if (request == null || request.active() == null || request.reason() == null || request.reason().isBlank()) {
            throw new InvalidAcademicScheduleRequestException("active와 reason은 필수입니다.");
        }
        normalizeRequiredReason(request.reason());
    }

    private void validateLengths(String title, String content) {
        if (title.strip().length() > 100) {
            throw new InvalidAcademicScheduleRequestException("title은 100자 이하여야 합니다.");
        }
        if (content != null && content.length() > 5000) {
            throw new InvalidAcademicScheduleRequestException("content는 5000자 이하여야 합니다.");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new InvalidAcademicScheduleRequestException("startDate는 필수입니다.");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidAcademicScheduleRequestException("endDate는 startDate보다 빠를 수 없습니다.");
        }
    }

    private TermAssignment resolveTermAssignment(LocalDate startDate, LocalDate endDate) {
        SemesterTerm startTerm = startDate.getMonthValue() <= 7 ? SemesterTerm.FIRST : SemesterTerm.SECOND;
        if (endDate != null) {
            SemesterTerm endTerm = endDate.getMonthValue() <= 7 ? SemesterTerm.FIRST : SemesterTerm.SECOND;
            if (startDate.getYear() != endDate.getYear() || startTerm != endTerm) {
                throw new InvalidAcademicScheduleRequestException(
                        "일정 시작일과 종료일은 같은 학년도·학기 구간에 있어야 합니다."
                );
            }
        }
        if (startDate.getYear() > Short.MAX_VALUE) {
            throw new InvalidAcademicScheduleRequestException("학년도 범위가 올바르지 않습니다.");
        }
        return new TermAssignment((short) startDate.getYear(), startTerm);
    }

    private void validateNoDuplicate(Long excludedId, String title, String content, LocalDate startDate,
                                     LocalDate endDate, AcademicScheduleTargetRole targetRole) {
        if (academicScheduleQueryRepository.existsDuplicate(
                excludedId, title, content, startDate, endDate, targetRole
        )) {
            throw new DuplicateAcademicScheduleException();
        }
    }

    private void validateAdmin(CurrentUser currentUser) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new AcademicScheduleAccessDeniedException();
        }
    }

    private AcademicScheduleTargetRole resolveUserRole(CurrentUser currentUser) {
        if (currentUser == null || currentUser.role() == null || currentUser.isAdmin()) {
            if (currentUser != null && currentUser.isAdmin()) {
                return null;
            }
            throw new AcademicScheduleAccessDeniedException();
        }
        try {
            return AcademicScheduleTargetRole.valueOf(currentUser.role());
        } catch (IllegalArgumentException exception) {
            throw new AcademicScheduleAccessDeniedException();
        }
    }

    private Set<AcademicScheduleTargetRole> resolveTargetRoles(AcademicScheduleTargetRole requestedRole,
                                                               AcademicScheduleTargetRole userRole,
                                                               boolean admin) {
        if (admin) {
            return requestedRole == null ? null : Set.of(requestedRole);
        }
        if (requestedRole == null) {
            return Set.of(AcademicScheduleTargetRole.ALL, userRole);
        }
        if (requestedRole != AcademicScheduleTargetRole.ALL && requestedRole != userRole) {
            throw new AcademicScheduleAccessDeniedException();
        }
        return Set.of(requestedRole);
    }

    private AcademicSchedule findSchedule(Long scheduleId) {
        return academicScheduleRepository.findById(scheduleId)
                .orElseThrow(AcademicScheduleNotFoundException::new);
    }

    private boolean isSameSchedule(AcademicSchedule schedule, String title, String content,
                                   LocalDate startDate, LocalDate endDate,
                                   AcademicScheduleTargetRole targetRole) {
        return Objects.equals(schedule.getTitle(), title)
                && Objects.equals(schedule.getContent(), content)
                && Objects.equals(schedule.getStartDate(), startDate)
                && Objects.equals(schedule.getEndDate(), endDate)
                && schedule.getTargetRole() == targetRole;
    }

    private String normalizeTitle(String title) {
        return title.strip();
    }

    private String normalizeContent(String content) {
        return content == null || content.isBlank() ? null : content.strip();
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.strip();
    }

    private String normalizeRequiredReason(String reason) {
        String normalized = reason == null ? null : reason.strip();
        if (normalized == null || normalized.isBlank() || normalized.length() > 255) {
            throw new InvalidAcademicScheduleRequestException("reason은 공백이 아닌 255자 이하의 값이어야 합니다.");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private Map<String, Object> snapshot(AcademicSchedule schedule) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", schedule.getId());
        value.put("title", schedule.getTitle());
        value.put("content", schedule.getContent());
        value.put("category", schedule.getCategory().name());
        value.put("academicYear", schedule.getAcademicYear());
        value.put("term", schedule.getTerm().name());
        value.put("startDate", schedule.getStartDate().toString());
        value.put("endDate", schedule.getEndDate() == null ? null : schedule.getEndDate().toString());
        value.put("targetRole", schedule.getTargetRole().name());
        value.put("isActive", schedule.isActive());
        value.put("createdAt", schedule.getCreatedAt().toString());
        value.put("authorId", schedule.getAuthor().getId());
        return value;
    }

    private void applyOperationalPeriod(AcademicSchedule schedule, TermAssignment assignment) {
        if (schedule.getCategory() == AcademicScheduleCategory.OTHER) {
            return;
        }
        LocalDate endDate = schedule.getEndDate();
        if (endDate == null) {
            throw new InvalidAcademicScheduleRequestException(
                    schedule.getCategory().getLabel() + " 일정은 종료일이 필요합니다."
            );
        }
        Semester semester = semesterRepository.findByAcademicYearAndTermForUpdate(
                        assignment.academicYear(), assignment.term())
                .orElseThrow(SemesterNotFoundException::new);

        switch (schedule.getCategory()) {
            case ENROLLMENT -> semester.changeEnrollmentPeriod(
                    schedule.getStartDate().atStartOfDay(), endDate.atTime(23, 59, 59)
            );
            case COURSE_CORRECTION -> upsertCourseCorrectionPeriod(semester, schedule.getStartDate(), endDate);
            case LEAVE -> upsertLeavePeriod(semester, LeaveRequestType.GENERAL_LEAVE,
                    schedule.getStartDate(), endDate);
            case RETURN -> {
                upsertLeavePeriod(semester, LeaveRequestType.GENERAL_RETURN, schedule.getStartDate(), endDate);
                upsertLeavePeriod(semester, LeaveRequestType.MILITARY_RETURN, schedule.getStartDate(), endDate);
            }
            case SCHOLARSHIP -> recordScholarshipPeriodChanged(schedule, semester);
            default -> {
                // 성적·등록금 기간은 각 담당 서비스의 후속 연동으로 처리한다.
            }
        }
    }

    private void upsertCourseCorrectionPeriod(Semester semester, LocalDate startDate, LocalDate endDate) {
        courseCorrectionPeriodRepository.findBySemesterIdForUpdate(semester.getId())
                .ifPresentOrElse(
                        period -> period.change(startDate, endDate, true),
                        () -> courseCorrectionPeriodRepository.save(
                                CourseCorrectionPeriod.create(semester, startDate, endDate, true)
                        )
                );
    }

    private void upsertLeavePeriod(Semester semester, LeaveRequestType requestType,
                                   LocalDate startDate, LocalDate endDate) {
        leavePeriodRepository.findBySemesterIdAndRequestTypeForUpdate(semester.getId(), requestType)
                .ifPresentOrElse(
                        period -> period.change(
                                startDate.atStartOfDay(), endDate.atTime(23, 59, 59),
                                startDate.atStartOfDay(), endDate.atTime(23, 59, 59), true
                        ),
                        () -> leavePeriodRepository.save(
                                LeaveRequestPeriod.create(
                                        semester, requestType,
                                        startDate.atStartOfDay(), endDate.atTime(23, 59, 59),
                                        startDate.atStartOfDay(), endDate.atTime(23, 59, 59), true
                                )
                        )
                );
    }

    private void validateOperationalPeriodDate(AcademicScheduleCategory category, LocalDate endDate) {
        if (category != AcademicScheduleCategory.OTHER && endDate == null) {
            throw new InvalidAcademicScheduleRequestException(category.getLabel() + " 일정은 종료일이 필요합니다.");
        }
    }

    private void recordScholarshipPeriodChanged(AcademicSchedule schedule, Semester semester) {
        outboxEventService.record(
                AGGREGATE_TYPE,
                schedule.getId(),
                SCHOLARSHIP_PERIOD_CHANGED,
                Map.of(
                        "scheduleId", schedule.getId(),
                        "semesterId", semester.getId(),
                        "category", schedule.getCategory().name(),
                        "startDate", schedule.getStartDate().toString(),
                        "endDate", schedule.getEndDate().toString(),
                        "active", schedule.isActive(),
                        "createdBy", schedule.getAuthor().getId()
                ),
                1L
        );
    }

    private record TermAssignment(short academicYear, SemesterTerm term) {
    }
}
