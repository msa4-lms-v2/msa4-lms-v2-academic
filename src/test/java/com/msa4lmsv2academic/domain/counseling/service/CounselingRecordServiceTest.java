package com.msa4lmsv2academic.domain.counseling.service;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingRecord;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRecordQueryRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRecordRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRecordSearchCondition;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingRecordSearchResult;
import com.msa4lmsv2academic.domain.counseling.request.CounselingRecordSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.InPersonCounselingCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.OnlineCounselingResponseUpdateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingRecordResponseDTO;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.CounselingAccessDeniedException;
import com.msa4lmsv2academic.global.error.CounselingRecordNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidCounselingRecordException;
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
class CounselingRecordServiceTest {

    @Mock
    private CounselingRecordRepository counselingRecordRepository;

    @Mock
    private CounselingRecordQueryRepository counselingRecordQueryRepository;

    @InjectMocks
    private CounselingRecordService counselingRecordService;

    private CurrentUser professorCurrentUser;
    private Professor professor;
    private Student student;

    @BeforeEach
    void setUp() {
        professorCurrentUser = new CurrentUser(2001L, "PROFESSOR");
        professor = org.mockito.Mockito.mock(Professor.class);
        student = org.mockito.Mockito.mock(Student.class);
        User professorUser = org.mockito.Mockito.mock(User.class);
        User studentUser = org.mockito.Mockito.mock(User.class);

        lenient().when(professor.getId()).thenReturn(301L);
        lenient().when(professor.getUser()).thenReturn(professorUser);
        lenient().when(professorUser.getName()).thenReturn("김교수");
        lenient().when(student.getId()).thenReturn(101L);
        lenient().when(student.getUser()).thenReturn(studentUser);
        lenient().when(studentUser.getName()).thenReturn("이학생");
    }

    @Test
    void searchUsesAuthenticatedProfessorScopeAndClampsPageSize() {
        CounselingRecordSearchRequestDTO request = new CounselingRecordSearchRequestDTO(
                2,
                500,
                101L,
                CounselingMethod.ONLINE,
                CounselingStatus.PENDING
        );
        when(counselingRecordQueryRepository.search(any()))
                .thenReturn(new CounselingRecordSearchResult(List.of(), 150L));

        PageRes<?> response = counselingRecordService.search(request, professorCurrentUser);

        ArgumentCaptor<CounselingRecordSearchCondition> conditionCaptor =
                ArgumentCaptor.forClass(CounselingRecordSearchCondition.class);
        verify(counselingRecordQueryRepository).search(conditionCaptor.capture());
        CounselingRecordSearchCondition condition = conditionCaptor.getValue();

        assertThat(condition.professorUserId()).isEqualTo(2001L);
        assertThat(condition.offset()).isEqualTo(100L);
        assertThat(condition.size()).isEqualTo(100);
        assertThat(condition.studentId()).isEqualTo(101L);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void createInPersonAllowsOnlyCurrentProfessorsAdvisedStudent() {
        InPersonCounselingCreateRequestDTO request = new InPersonCounselingCreateRequestDTO(
                101L,
                "  복수전공 상담  ",
                "  신청 일정과 필요 서류를 안내했습니다.  ",
                LocalDateTime.now().minusHours(1)
        );
        when(counselingRecordQueryRepository.findProfessorByUserId(2001L))
                .thenReturn(Optional.of(professor));
        when(counselingRecordQueryRepository.findAdvisedStudent(101L, 301L))
                .thenReturn(Optional.of(student));
        when(counselingRecordRepository.saveAndFlush(any(CounselingRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CounselingRecordResponseDTO response = counselingRecordService.createInPerson(
                request,
                professorCurrentUser
        );

        ArgumentCaptor<CounselingRecord> recordCaptor = ArgumentCaptor.forClass(CounselingRecord.class);
        verify(counselingRecordRepository).saveAndFlush(recordCaptor.capture());
        CounselingRecord savedRecord = recordCaptor.getValue();

        assertThat(savedRecord.getCounselingMethod()).isEqualTo(CounselingMethod.IN_PERSON);
        assertThat(savedRecord.getStatus()).isEqualTo(CounselingStatus.COMPLETED);
        assertThat(savedRecord.getTitle()).isEqualTo("복수전공 상담");
        assertThat(savedRecord.getProfessorResponse()).isEqualTo("신청 일정과 필요 서류를 안내했습니다.");
        assertThat(response.studentName()).isEqualTo("이학생");
        assertThat(response.professorName()).isEqualTo("김교수");
    }

    @Test
    void createInPersonRejectsStudentWhoIsNotAssignedToProfessor() {
        InPersonCounselingCreateRequestDTO request = new InPersonCounselingCreateRequestDTO(
                102L,
                "학업 상담",
                "학업 계획을 논의했습니다.",
                LocalDateTime.now().minusHours(1)
        );
        when(counselingRecordQueryRepository.findProfessorByUserId(2001L))
                .thenReturn(Optional.of(professor));
        when(counselingRecordQueryRepository.findAdvisedStudent(102L, 301L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> counselingRecordService.createInPerson(request, professorCurrentUser))
                .isInstanceOf(CounselingAccessDeniedException.class)
                .hasMessage("담당 학생의 상담 기록만 등록할 수 있습니다.");
    }

    @Test
    void createInPersonRejectsFutureCounselingDate() {
        InPersonCounselingCreateRequestDTO request = new InPersonCounselingCreateRequestDTO(
                101L,
                "학업 상담",
                "학업 계획을 논의했습니다.",
                LocalDateTime.now().plusDays(1)
        );

        assertThatThrownBy(() -> counselingRecordService.createInPerson(request, professorCurrentUser))
                .isInstanceOf(InvalidCounselingRecordException.class)
                .hasMessage("counseledAt은 현재보다 미래일 수 없습니다.");
    }

    @Test
    void updateOnlineResponseCompletesPendingRecord() {
        CounselingRecord record = CounselingRecord.requestOnline(
                student,
                professor,
                "졸업 요건 상담",
                "필수 학점을 확인하고 싶습니다."
        );
        when(counselingRecordQueryRepository.findByIdForProfessor(11L, 2001L))
                .thenReturn(Optional.of(record));
        when(counselingRecordRepository.saveAndFlush(record)).thenReturn(record);

        CounselingRecordResponseDTO response = counselingRecordService.updateOnlineResponse(
                11L,
                new OnlineCounselingResponseUpdateRequestDTO("  전공필수 6학점이 더 필요합니다.  "),
                professorCurrentUser
        );

        assertThat(record.getStatus()).isEqualTo(CounselingStatus.COMPLETED);
        assertThat(response.professorResponse()).isEqualTo("전공필수 6학점이 더 필요합니다.");
        assertThat(response.respondedAt()).isNotNull();
    }

    @Test
    void updateOnlineResponseRejectsInPersonRecord() {
        CounselingRecord record = CounselingRecord.recordInPerson(
                student,
                professor,
                "진로 상담",
                "진로 방향을 논의했습니다.",
                LocalDateTime.now().minusDays(1)
        );
        when(counselingRecordQueryRepository.findByIdForProfessor(12L, 2001L))
                .thenReturn(Optional.of(record));

        assertThatThrownBy(() -> counselingRecordService.updateOnlineResponse(
                12L,
                new OnlineCounselingResponseUpdateRequestDTO("답변"),
                professorCurrentUser
        )).isInstanceOf(InvalidCounselingRecordException.class)
                .hasMessage("온라인 상담에만 답변을 등록할 수 있습니다.");
    }

    @Test
    void studentRoleCannotUseProfessorCounselingService() {
        CounselingRecordSearchRequestDTO request = new CounselingRecordSearchRequestDTO(
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> counselingRecordService.search(
                request,
                new CurrentUser(1001L, "STUDENT")
        )).isInstanceOf(CounselingAccessDeniedException.class);
    }

    @Test
    void anotherProfessorsRecordIsReturnedAsNotFound() {
        when(counselingRecordQueryRepository.findByIdForProfessor(99L, 2001L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> counselingRecordService.get(99L, professorCurrentUser))
                .isInstanceOf(CounselingRecordNotFoundException.class);
    }
}
