package com.msa4lmsv2academic.domain.lecture.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureOpeningRequest;
import com.msa4lmsv2academic.domain.lecture.entity.LectureOpeningRequestStatus;
import com.msa4lmsv2academic.domain.lecture.entity.LectureSchedule;
import com.msa4lmsv2academic.domain.lecture.repository.LectureOpeningReferenceQueryRepository;
import com.msa4lmsv2academic.domain.lecture.repository.LectureOpeningRequestRepository;
import com.msa4lmsv2academic.domain.lecture.repository.LectureRepository;
import com.msa4lmsv2academic.domain.lecture.repository.LectureScheduleRepository;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningCorrectionRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningCreateRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningReviewRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningScheduleRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.LectureOpeningResponseDTO;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.DuplicateLectureOpeningRequestException;
import com.msa4lmsv2academic.global.error.InvalidLectureOpeningRequestException;
import com.msa4lmsv2academic.global.error.LectureOpeningAccessDeniedException;
import com.msa4lmsv2academic.global.error.LectureOpeningRequestNotFoundException;
import com.msa4lmsv2academic.global.error.LectureOpeningReferenceNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureOpeningService {

    private static final String AUDIT_TARGET_TYPE = "LECTURE_OPENING_REQUEST";

    private final LectureOpeningRequestRepository openingRequestRepository;
    private final LectureOpeningReferenceQueryRepository referenceQueryRepository;
    private final LectureRepository lectureRepository;
    private final LectureScheduleRepository lectureScheduleRepository;
    private final AuditLogService auditLogService;

    public Page<LectureOpeningResponseDTO> search(
            LectureOpeningRequestStatus status,
            Pageable pageable,
            CurrentUser currentUser
    ) {
        validateAuthenticated(currentUser);

        Page<LectureOpeningRequest> result = switch (currentUser.role()) {
            case "PROFESSOR" -> status == null
                    ? openingRequestRepository.findByProfessorUserId(currentUser.id(), pageable)
                    : openingRequestRepository.findByProfessorUserIdAndStatus(
                            currentUser.id(), status, pageable
                    );
            case "ADMIN" -> status == null
                    ? openingRequestRepository.findAll(pageable)
                    : openingRequestRepository.findByStatus(status, pageable);
            default -> throw new LectureOpeningAccessDeniedException("강의 개설 신청 조회 권한이 없습니다.");
        };

        return result.map(LectureOpeningResponseDTO::from);
    }

    public LectureOpeningResponseDTO get(Long requestId, CurrentUser currentUser) {
        validateAuthenticated(currentUser);
        LectureOpeningRequest request = openingRequestRepository.findDetailById(requestId)
                .orElseThrow(LectureOpeningRequestNotFoundException::new);
        validateReadable(request, currentUser);
        return LectureOpeningResponseDTO.from(request);
    }

    @Transactional
    public LectureOpeningResponseDTO create(
            LectureOpeningCreateRequestDTO createRequest,
            CurrentUser currentUser
    ) {
        validateRole(currentUser, "PROFESSOR");
        Professor professor = referenceQueryRepository.findProfessorByUserId(currentUser.id())
                .orElseThrow(() -> new LectureOpeningReferenceNotFoundException("교수 정보를 찾을 수 없습니다."));
        Course course = referenceQueryRepository.findCourseById(createRequest.courseId())
                .orElseThrow(() -> new LectureOpeningReferenceNotFoundException("교과목을 찾을 수 없습니다."));
        Semester semester = referenceQueryRepository.findSemesterById(createRequest.semesterId())
                .orElseThrow(() -> new LectureOpeningReferenceNotFoundException("학기를 찾을 수 없습니다."));

        if (!course.getDepartment().getId().equals(professor.getDepartment().getId())) {
            throw new LectureOpeningAccessDeniedException("소속 학과의 교과목만 강의 개설을 신청할 수 있습니다.");
        }

        String sectionNo = createRequest.sectionNo().trim();
        validateNoDuplicateRequest(course.getId(), professor.getId(), semester.getId(), sectionNo);
        validateNoExistingLecture(semester.getId(), course.getId(), sectionNo);
        validateScheduleConflicts(professor.getId(), semester.getId(), createRequest.schedules());

        LectureOpeningRequest request = LectureOpeningRequest.create(
                course,
                professor,
                semester,
                sectionNo,
                createRequest.requestedCapacity(),
                createRequest.classroom().trim(),
                createRequest.midtermRatio(),
                createRequest.finalRatio(),
                createRequest.assignmentRatio(),
                createRequest.attendanceRatio(),
                createRequest.syllabus().trim()
        );
        addSchedules(request, createRequest.schedules());

        try {
            openingRequestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateLectureOpeningRequestException("동일한 처리 대기 강의 개설 신청이 이미 존재합니다.");
        }
        auditLogService.record(
                currentUser.id(),
                "LECTURE_OPENING_REQUESTED",
                AUDIT_TARGET_TYPE,
                request.getId(),
                null,
                auditSnapshot(request, null),
                null,
                null,
                null
        );
        return LectureOpeningResponseDTO.from(request);
    }

    @Transactional
    public LectureOpeningResponseDTO update(
            Long requestId,
            LectureOpeningCorrectionRequestDTO updateRequest,
            CurrentUser currentUser
    ) {
        validateRole(currentUser, "PROFESSOR");
        LectureOpeningRequest request = openingRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(LectureOpeningRequestNotFoundException::new);
        validateProfessorOwner(request, currentUser.id());
        Map<String, Object> beforeValue = auditSnapshot(request, null);

        Course course = referenceQueryRepository.findCourseById(updateRequest.courseId())
                .orElseThrow(() -> new LectureOpeningReferenceNotFoundException("보완할 교과목을 찾을 수 없습니다."));
        Semester semester = referenceQueryRepository.findSemesterById(updateRequest.semesterId())
                .orElseThrow(() -> new LectureOpeningReferenceNotFoundException("보완할 학기를 찾을 수 없습니다."));
        if (!course.getDepartment().getId().equals(request.getProfessor().getDepartment().getId())) {
            throw new LectureOpeningAccessDeniedException("소속 학과의 교과목으로만 강의 개설 신청을 보완할 수 있습니다.");
        }

        try {
            request.correct(
                    course,
                    semester,
                    updateRequest.sectionNo().trim(),
                    updateRequest.requestedCapacity(),
                    updateRequest.classroom().trim(),
                    updateRequest.midtermRatio(),
                    updateRequest.finalRatio(),
                    updateRequest.assignmentRatio(),
                    updateRequest.attendanceRatio(),
                    updateRequest.syllabus().trim()
            );
            request.clearSchedules();
            addSchedules(request, updateRequest.schedules());
            validateNoOtherPendingRequest(request);
            referenceQueryRepository.lockProfessor(request.getProfessor().getId())
                    .orElseThrow(() -> new LectureOpeningReferenceNotFoundException("교수 정보를 찾을 수 없습니다."));
            validateNoExistingLecture(
                    request.getSemester().getId(),
                    request.getCourse().getId(),
                    request.getSectionNo()
            );
            validateScheduleConflicts(
                    request.getProfessor().getId(),
                    request.getSemester().getId(),
                    updateRequest.schedules()
            );
            openingRequestRepository.saveAndFlush(request);
            auditLogService.record(
                    currentUser.id(),
                    "LECTURE_OPENING_UPDATED",
                    AUDIT_TARGET_TYPE,
                    request.getId(),
                    beforeValue,
                    auditSnapshot(request, null),
                    null,
                    null,
                    null
            );
            return LectureOpeningResponseDTO.from(request);
        } catch (IllegalStateException exception) {
            throw new DuplicateLectureOpeningRequestException("처리 대기 상태인 강의 개설 신청만 보완할 수 있습니다.");
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateLectureOpeningRequestException("보완한 강의 개설 신청 정보가 이미 존재합니다.");
        }
    }

    @Transactional
    public LectureOpeningResponseDTO review(
            LectureOpeningReviewRequestDTO reviewRequest,
            CurrentUser currentUser
    ) {
        validateRole(currentUser, "ADMIN");
        LectureOpeningRequest request = openingRequestRepository.findByIdForUpdate(reviewRequest.openingRequestId())
                .orElseThrow(LectureOpeningRequestNotFoundException::new);
        User reviewer = referenceQueryRepository.findUserById(currentUser.id())
                .orElseThrow(() -> new LectureOpeningReferenceNotFoundException("관리자 정보를 찾을 수 없습니다."));
        Map<String, Object> beforeValue = auditSnapshot(request, null);

        try {
            if (Boolean.TRUE.equals(reviewRequest.approved())) {
                if (reviewRequest.correction() != null) {
                    applyCorrection(request, reviewRequest.correction());
                }
                validateNoOtherPendingRequest(request);
                referenceQueryRepository.lockProfessor(request.getProfessor().getId())
                        .orElseThrow(() -> new LectureOpeningReferenceNotFoundException("교수 정보를 찾을 수 없습니다."));
                validateNoExistingLecture(
                        request.getSemester().getId(),
                        request.getCourse().getId(),
                        request.getSectionNo()
                );
                validateScheduleConflicts(
                        request.getProfessor().getId(),
                        request.getSemester().getId(),
                        request.getSchedules().stream()
                                .map(schedule -> new LectureOpeningScheduleRequestDTO(
                                        schedule.getDayOfWeek(),
                                        schedule.getStartPeriod(),
                                        schedule.getEndPeriod()
                                ))
                                .toList()
                );
                request.approve(reviewer, LocalDateTime.now());
                Lecture lecture = lectureRepository.saveAndFlush(Lecture.fromApprovedOpeningRequest(request));
                List<LectureSchedule> lectureSchedules = request.getSchedules().stream()
                        .map(schedule -> LectureSchedule.create(
                                lecture,
                                schedule.getDayOfWeek(),
                                schedule.getStartPeriod(),
                                schedule.getEndPeriod()
                        ))
                        .toList();
                lectureScheduleRepository.saveAll(lectureSchedules);
                lectureScheduleRepository.flush();
                request.linkApprovedLecture(lecture);
                openingRequestRepository.saveAndFlush(request);
                recordReviewAudit(currentUser, request, beforeValue, "LECTURE_OPENING_APPROVED", null);
                return LectureOpeningResponseDTO.from(request);
            }

            String rejectReason = requiredRejectReason(reviewRequest.rejectReason());
            request.reject(reviewer, rejectReason, LocalDateTime.now());
            openingRequestRepository.saveAndFlush(request);
            recordReviewAudit(currentUser, request, beforeValue, "LECTURE_OPENING_REJECTED", rejectReason);
            return LectureOpeningResponseDTO.from(request);
        } catch (IllegalStateException exception) {
            throw new DuplicateLectureOpeningRequestException("처리 대기 상태인 강의 개설 신청만 검토할 수 있습니다.");
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateLectureOpeningRequestException("이미 승인되었거나 중복되는 강의 개설 정보입니다.");
        }
    }

    private void applyCorrection(
            LectureOpeningRequest request,
            LectureOpeningCorrectionRequestDTO correction
    ) {
        Course course = referenceQueryRepository.findCourseById(correction.courseId())
                .orElseThrow(() -> new LectureOpeningReferenceNotFoundException("보정할 교과목을 찾을 수 없습니다."));
        Semester semester = referenceQueryRepository.findSemesterById(correction.semesterId())
                .orElseThrow(() -> new LectureOpeningReferenceNotFoundException("보정할 학기를 찾을 수 없습니다."));
        request.correct(
                course,
                semester,
                correction.sectionNo().trim(),
                correction.requestedCapacity(),
                correction.classroom().trim(),
                correction.midtermRatio(),
                correction.finalRatio(),
                correction.assignmentRatio(),
                correction.attendanceRatio(),
                correction.syllabus().trim()
        );
        request.clearSchedules();
        addSchedules(request, correction.schedules());
    }

    private void validateNoOtherPendingRequest(LectureOpeningRequest request) {
        if (openingRequestRepository.existsByCourseIdAndProfessorIdAndSemesterIdAndSectionNoAndStatusAndIdNot(
                request.getCourse().getId(),
                request.getProfessor().getId(),
                request.getSemester().getId(),
                request.getSectionNo(),
                LectureOpeningRequestStatus.PENDING,
                request.getId()
        )) {
            throw new DuplicateLectureOpeningRequestException("보정한 정보와 동일한 처리 대기 신청이 이미 존재합니다.");
        }
    }

    private void addSchedules(
            LectureOpeningRequest request,
            List<LectureOpeningScheduleRequestDTO> schedules
    ) {
        for (LectureOpeningScheduleRequestDTO schedule : schedules) {
            request.addSchedule(schedule.dayOfWeek(), schedule.startPeriod(), schedule.endPeriod());
        }
    }

    private void validateNoDuplicateRequest(
            Long courseId,
            Long professorId,
            Long semesterId,
            String sectionNo
    ) {
        if (openingRequestRepository.existsByCourseIdAndProfessorIdAndSemesterIdAndSectionNoAndStatus(
                courseId,
                professorId,
                semesterId,
                sectionNo,
                LectureOpeningRequestStatus.PENDING
        )) {
            throw new DuplicateLectureOpeningRequestException("동일한 처리 대기 강의 개설 신청이 이미 존재합니다.");
        }
    }

    private void validateNoExistingLecture(Long semesterId, Long courseId, String sectionNo) {
        if (lectureRepository.existsBySemesterIdAndCourseIdAndSectionNo(semesterId, courseId, sectionNo)) {
            throw new DuplicateLectureOpeningRequestException("동일한 학기·교과목·분반의 강의가 이미 개설되어 있습니다.");
        }
    }

    private void validateScheduleConflicts(
            Long professorId,
            Long semesterId,
            List<LectureOpeningScheduleRequestDTO> schedules
    ) {
        for (LectureOpeningScheduleRequestDTO schedule : schedules) {
            if (lectureScheduleRepository.existsProfessorScheduleConflict(
                    professorId,
                    semesterId,
                    schedule.dayOfWeek(),
                    schedule.startPeriod(),
                    schedule.endPeriod()
            )) {
                throw new DuplicateLectureOpeningRequestException("같은 학기에 담당 교수의 강의 시간이 겹칩니다.");
            }
        }
    }

    private void validateReadable(LectureOpeningRequest request, CurrentUser currentUser) {
        boolean readable = switch (currentUser.role()) {
            case "PROFESSOR" -> request.getProfessor().getUser().getId().equals(currentUser.id());
            case "ADMIN" -> true;
            default -> false;
        };
        if (!readable) {
            throw new LectureOpeningAccessDeniedException("본인의 신청 또는 관리자 범위만 조회할 수 있습니다.");
        }
    }

    private void validateProfessorOwner(LectureOpeningRequest request, Long professorUserId) {
        if (!request.getProfessor().getUser().getId().equals(professorUserId)) {
            throw new LectureOpeningAccessDeniedException("본인의 강의 개설 신청만 보완할 수 있습니다.");
        }
    }

    private void validateAuthenticated(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.role() == null) {
            throw new LectureOpeningAccessDeniedException("인증된 사용자만 강의 개설 신청을 조회할 수 있습니다.");
        }
    }

    private void validateRole(CurrentUser currentUser, String expectedRole) {
        if (currentUser == null || currentUser.id() == null || !expectedRole.equals(currentUser.role())) {
            throw new LectureOpeningAccessDeniedException("강의 개설 신청 처리 권한이 없습니다.");
        }
    }

    private String requiredRejectReason(String rejectReason) {
        if (rejectReason == null || rejectReason.isBlank()) {
            throw new InvalidLectureOpeningRequestException("반려 시 반려 사유는 필수입니다.");
        }
        return rejectReason.trim();
    }

    private void recordReviewAudit(
            CurrentUser currentUser,
            LectureOpeningRequest request,
            Map<String, Object> beforeValue,
            String action,
            String reason
    ) {
        auditLogService.record(
                currentUser.id(),
                action,
                AUDIT_TARGET_TYPE,
                request.getId(),
                beforeValue,
                auditSnapshot(request, request.getApprovedLecture() == null ? null : request.getApprovedLecture().getId()),
                reason,
                null,
                null
        );
    }

    private Map<String, Object> auditSnapshot(LectureOpeningRequest request, Long lectureId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", request.getStatus().name());
        snapshot.put("courseId", request.getCourse().getId());
        snapshot.put("professorId", request.getProfessor().getId());
        snapshot.put("semesterId", request.getSemester().getId());
        snapshot.put("sectionNo", request.getSectionNo());
        snapshot.put("requestedCapacity", request.getRequestedCapacity());
        snapshot.put("classroom", request.getClassroom());
        snapshot.put("midtermRatio", request.getMidtermRatio());
        snapshot.put("finalRatio", request.getFinalRatio());
        snapshot.put("assignmentRatio", request.getAssignmentRatio());
        snapshot.put("attendanceRatio", request.getAttendanceRatio());
        snapshot.put("syllabus", request.getSyllabus());
        snapshot.put("schedules", request.getSchedules().stream()
                .map(schedule -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("dayOfWeek", schedule.getDayOfWeek().name());
                    value.put("startPeriod", schedule.getStartPeriod());
                    value.put("endPeriod", schedule.getEndPeriod());
                    return value;
                })
                .toList());
        snapshot.put("lectureId", lectureId);
        return snapshot;
    }
}
