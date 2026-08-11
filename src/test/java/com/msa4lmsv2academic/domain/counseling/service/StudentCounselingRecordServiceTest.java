package com.msa4lmsv2academic.domain.counseling.service;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingRecord;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRecordQueryRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRecordRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRecordSearchResult;
import com.msa4lmsv2academic.domain.counseling.repository.StudentCounselingRecordSearchCondition;
import com.msa4lmsv2academic.domain.counseling.request.OnlineCounselingCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.StudentCounselingRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordResponseDTO;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.CounselingAccessDeniedException;
import com.msa4lmsv2academic.global.error.CounselingParticipantNotFoundException;
import com.msa4lmsv2academic.global.error.CounselingRecordNotFoundException;
import com.msa4lmsv2academic.global.error.DuplicateCounselingRequestException;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCounselingRecordServiceTest {

    @Mock
    private CounselingRecordRepository counselingRecordRepository;

    @Mock
    private CounselingRecordQueryRepository counselingRecordQueryRepository;

    @InjectMocks
    private CounselingRecordService counselingRecordService;

    private CurrentUser studentCurrentUser;
    private Student student;
    private Professor advisor;

    @BeforeEach
    void setUp() {
        studentCurrentUser = new CurrentUser(1001L, "STUDENT");
        student = org.mockito.Mockito.mock(Student.class);
        advisor = org.mockito.Mockito.mock(Professor.class);
        User studentUser = org.mockito.Mockito.mock(User.class);
        User advisorUser = org.mockito.Mockito.mock(User.class);

        lenient().when(student.getId()).thenReturn(101L);
        lenient().when(student.getUser()).thenReturn(studentUser);
        lenient().when(student.getAdvisor()).thenReturn(advisor);
        lenient().when(studentUser.getName()).thenReturn("이학생");
        lenient().when(advisor.getId()).thenReturn(301L);
        lenient().when(advisor.getUser()).thenReturn(advisorUser);
        lenient().when(advisorUser.getName()).thenReturn("김교수");
    }

    @Test
    void createOnlineUsesAuthenticatedStudentsAdvisor() {
        OnlineCounselingCreateRequestDTO request = new OnlineCounselingCreateRequestDTO(
                "  졸업 요건 상담  ",
                "  전공필수 학점을 확인하고 싶습니다.  "
        );
        when(counselingRecordQueryRepository.findStudentWithAdvisorByUserId(1001L))
                .thenReturn(Optional.of(student));
        when(counselingRecordRepository.existsByStudentIdAndProfessorIdAndCounselingMethodAndStatus(
                101L,
                301L,
                CounselingMethod.ONLINE,
                CounselingStatus.PENDING
        )).thenReturn(false);
        when(counselingRecordRepository.saveAndFlush(any(CounselingRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CounselingRecordResponseDTO response = counselingRecordService.createOnline(request, studentCurrentUser);

        ArgumentCaptor<CounselingRecord> recordCaptor = ArgumentCaptor.forClass(CounselingRecord.class);
        verify(counselingRecordRepository).saveAndFlush(recordCaptor.capture());
        CounselingRecord record = recordCaptor.getValue();

        assertThat(record.getStudent()).isSameAs(student);
        assertThat(record.getProfessor()).isSameAs(advisor);
        assertThat(record.getCounselingMethod()).isEqualTo(CounselingMethod.ONLINE);
        assertThat(record.getStatus()).isEqualTo(CounselingStatus.PENDING);
        assertThat(record.getTitle()).isEqualTo("졸업 요건 상담");
        assertThat(response.studentContent()).isEqualTo("전공필수 학점을 확인하고 싶습니다.");
        assertThat(response.professorResponse()).isNull();
    }

    @Test
    void createOnlineRejectsDuplicatePendingRequest() {
        when(counselingRecordQueryRepository.findStudentWithAdvisorByUserId(1001L))
                .thenReturn(Optional.of(student));
        when(counselingRecordRepository.existsByStudentIdAndProfessorIdAndCounselingMethodAndStatus(
                101L,
                301L,
                CounselingMethod.ONLINE,
                CounselingStatus.PENDING
        )).thenReturn(true);

        assertThatThrownBy(() -> counselingRecordService.createOnline(
                new OnlineCounselingCreateRequestDTO("졸업 상담", "졸업 학점을 확인하고 싶습니다."),
                studentCurrentUser
        )).isInstanceOf(DuplicateCounselingRequestException.class);
    }

    @Test
    void createOnlineRejectsStudentWithoutAdvisor() {
        when(student.getAdvisor()).thenReturn(null);
        when(counselingRecordQueryRepository.findStudentWithAdvisorByUserId(1001L))
                .thenReturn(Optional.of(student));

        assertThatThrownBy(() -> counselingRecordService.createOnline(
                new OnlineCounselingCreateRequestDTO("학업 상담", "지도교수 상담을 신청합니다."),
                studentCurrentUser
        )).isInstanceOf(CounselingParticipantNotFoundException.class)
                .hasMessage("지도교수가 지정되지 않았습니다.");
    }

    @Test
    void searchMyRecordsUsesAuthenticatedStudentScopeAndClampsPageSize() {
        StudentCounselingRecordSearchRequestDTO request = new StudentCounselingRecordSearchRequestDTO(
                2,
                500,
                CounselingMethod.ONLINE,
                CounselingStatus.COMPLETED
        );
        when(counselingRecordQueryRepository.searchForStudent(any()))
                .thenReturn(new CounselingRecordSearchResult(List.of(), 150L));

        PageRes<?> response = counselingRecordService.searchMyRecords(request, studentCurrentUser);

        ArgumentCaptor<StudentCounselingRecordSearchCondition> conditionCaptor =
                ArgumentCaptor.forClass(StudentCounselingRecordSearchCondition.class);
        verify(counselingRecordQueryRepository).searchForStudent(conditionCaptor.capture());
        StudentCounselingRecordSearchCondition condition = conditionCaptor.getValue();

        assertThat(condition.studentUserId()).isEqualTo(1001L);
        assertThat(condition.offset()).isEqualTo(100L);
        assertThat(condition.size()).isEqualTo(100);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void getMyRecordReturnsProfessorResponse() {
        CounselingRecord record = CounselingRecord.requestOnline(
                student,
                advisor,
                "졸업 상담",
                "졸업 학점을 확인하고 싶습니다."
        );
        record.answer("전공필수 6학점이 더 필요합니다.", LocalDateTime.now());
        when(counselingRecordQueryRepository.findByIdForStudent(11L, 1001L))
                .thenReturn(Optional.of(record));

        CounselingRecordResponseDTO response = counselingRecordService.getMyRecord(11L, studentCurrentUser);

        assertThat(response.professorResponse()).isEqualTo("전공필수 6학점이 더 필요합니다.");
        assertThat(response.status()).isEqualTo(CounselingStatus.COMPLETED);
    }

    @Test
    void anotherStudentsRecordIsReturnedAsNotFound() {
        when(counselingRecordQueryRepository.findByIdForStudent(99L, 1001L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> counselingRecordService.getMyRecord(99L, studentCurrentUser))
                .isInstanceOf(CounselingRecordNotFoundException.class);
    }

    @Test
    void professorRoleCannotUseStudentCounselingService() {
        assertThatThrownBy(() -> counselingRecordService.searchMyRecords(
                new StudentCounselingRecordSearchRequestDTO(null, null, null, null),
                new CurrentUser(2001L, "PROFESSOR")
        )).isInstanceOf(CounselingAccessDeniedException.class);
    }
}
