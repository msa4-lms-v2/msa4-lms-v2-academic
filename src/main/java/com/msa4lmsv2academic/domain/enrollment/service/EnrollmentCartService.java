package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCart;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCartQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCartRepository;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCartCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCartSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentCartCreateResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentCartItemResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentCartSummaryResponseDTO;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.global.error.EnrollmentCartAccessDeniedException;
import com.msa4lmsv2academic.global.error.EnrollmentCartConflictException;
import com.msa4lmsv2academic.global.error.EnrollmentCartItemNotFoundException;
import com.msa4lmsv2academic.global.error.EnrollmentLectureNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidEnrollmentCartRequestException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentCartService {

    private final EnrollmentCartQueryRepository queryRepository;
    private final EnrollmentCartRepository cartRepository;
    private final EnrollmentAcademicStatusValidator academicStatusValidator;

    public EnrollmentCartSummaryResponseDTO getMyCart(
            EnrollmentCartSearchRequestDTO request,
            CurrentUser currentUser
    ) {
        validateStudent(currentUser);
        findStudent(currentUser.id());
        EnrollmentCartSearchRequestDTO resolved = request == null
                ? new EnrollmentCartSearchRequestDTO(null, null)
                : request;
        var items = queryRepository.findByStudentUserId(
                        currentUser.id(),
                        resolved.academicYear(),
                        resolved.term()
                ).stream()
                .map(EnrollmentCartItemResponseDTO::from)
                .toList();
        return EnrollmentCartSummaryResponseDTO.from(items);
    }

    @Transactional
    public EnrollmentCartCreateResponseDTO add(
            EnrollmentCartCreateRequestDTO request,
            CurrentUser currentUser
    ) {
        validateStudent(currentUser);
        validateCreateRequest(request);
        Student student = findStudent(currentUser.id());
        academicStatusValidator.validate(student.getAcademicStatus());
        Lecture lecture = queryRepository.findLecture(request.lectureId())
                .orElseThrow(EnrollmentLectureNotFoundException::new);
        validateAddableLecture(lecture, LocalDateTime.now());
        if (queryRepository.existsByStudentAndLecture(student.getId(), lecture.getId())) {
            throw new EnrollmentCartConflictException("이미 장바구니에 담은 강의입니다.");
        }

        EnrollmentCart cart = EnrollmentCart.create(student, lecture, LocalDateTime.now());
        try {
            return EnrollmentCartCreateResponseDTO.from(cartRepository.saveAndFlush(cart));
        } catch (DataIntegrityViolationException exception) {
            throw new EnrollmentCartConflictException("이미 장바구니에 담은 강의입니다.");
        }
    }

    @Transactional
    public void remove(Long cartItemId, CurrentUser currentUser) {
        validateStudent(currentUser);
        if (cartItemId == null || cartItemId <= 0) {
            throw new InvalidEnrollmentCartRequestException();
        }
        Student student = findStudent(currentUser.id());
        EnrollmentCart cart = queryRepository.findOwnedItemForUpdate(cartItemId, student.getId())
                .orElseThrow(EnrollmentCartItemNotFoundException::new);
        validateChangePeriod(cart.getLecture().getSemester(), LocalDateTime.now());
        cartRepository.delete(cart);
    }

    private Student findStudent(Long userId) {
        return queryRepository.findStudentByUserId(userId).orElseThrow(StudentNotFoundException::new);
    }

    private void validateStudent(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"STUDENT".equals(currentUser.role())) {
            throw new EnrollmentCartAccessDeniedException();
        }
    }

    private void validateCreateRequest(EnrollmentCartCreateRequestDTO request) {
        if (request == null || request.lectureId() == null || request.lectureId() <= 0) {
            throw new InvalidEnrollmentCartRequestException();
        }
    }

    private void validateAddableLecture(Lecture lecture, LocalDateTime now) {
        if (lecture.getStatus() != LectureStatus.OPEN) {
            throw new EnrollmentCartConflictException("장바구니에 담을 수 있는 개설 강의가 아닙니다.");
        }
        validateChangePeriod(lecture.getSemester(), now);
    }

    private void validateChangePeriod(Semester semester, LocalDateTime now) {
        if (semester.getEnrollmentStartAt() == null || semester.getEnrollmentEndAt() == null
                || now.isBefore(semester.getEnrollmentStartAt()) || now.isAfter(semester.getEnrollmentEndAt())) {
            throw new EnrollmentCartConflictException("수강신청 기간에만 장바구니를 변경할 수 있습니다.");
        }
    }
}
