package com.msa4lmsv2academic.domain.leaverequest.service;

import static org.assertj.core.api.Assertions.*;
import com.msa4lmsv2academic.domain.leaverequest.entity.*;
import com.msa4lmsv2academic.domain.leaverequest.request.LeaveRequestCreateRequestDTO;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class LeaveRequestPolicyTest {
    private final LeaveRequestPolicy policy = new LeaveRequestPolicy();

    @Test void generalLeaveAllowsLongDurationButNotSameOrEarlierTerm() {
        assertThatCode(() -> policy.validateCreate(general((short) 32767, (byte) 2))).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.validateCreate(general((short) 2026, (byte) 2)))
                .isInstanceOf(InvalidLeaveRequestException.class);
        assertThatThrownBy(() -> policy.validateCreate(general((short) 2026, (byte) 1)))
                .isInstanceOf(InvalidLeaveRequestException.class);
    }

    @Test void militaryAndReturnCannotOverrideCalculatedReturnSemester() {
        for (var type : new LeaveRequestType[]{LeaveRequestType.MILITARY_LEAVE, LeaveRequestType.GENERAL_RETURN,
                LeaveRequestType.MILITARY_RETURN}) {
            assertThatThrownBy(() -> policy.validateCreate(new LeaveRequestCreateRequestDTO(type, null, (short) 2026,
                    (byte) 2, (short) 2028, (byte) 2))).isInstanceOf(InvalidLeaveRequestException.class);
        }
    }

    @Test void receiptAndApprovalWindowsIncludeBothEdgesAndAreIndependent() {
        var start = LocalDateTime.of(2026, 8, 1, 9, 0);
        var end = start.plusDays(2);
        var period = LeaveRequestPeriod.create(null, LeaveRequestType.GENERAL_LEAVE,
                start, end, end.plusDays(1), end.plusDays(2), true);
        assertThat(period.accepts(start)).isTrue();
        assertThat(period.accepts(end)).isTrue();
        assertThat(period.accepts(start.minusNanos(1))).isFalse();
        assertThat(period.accepts(end.plusNanos(1))).isFalse();
        assertThat(period.allowsApproval(end)).isFalse();
        assertThat(period.allowsApproval(end.plusDays(1))).isTrue();
        assertThat(period.allowsApproval(end.plusDays(2))).isTrue();
        period.change(start, end, start, end, false);
        assertThat(period.accepts(start)).isFalse();
        assertThat(period.allowsApproval(start)).isFalse();
    }

    @Test void terminalRequestCannotBeChangedAgain() {
        var request = LeaveRequest.create(null, LeaveRequestType.GENERAL_LEAVE, "원본 사유",
                (short) 2026, (byte) 2, (short) 2027, (byte) 1);
        request.cancel("취소 사유");
        assertThat(request.getReason()).isEqualTo("원본 사유");
        assertThatThrownBy(request::approve).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> request.cancel("다시")).isInstanceOf(IllegalStateException.class);
    }

    @Test void onlyExpectedAcademicStateIsAccepted() {
        for (var type : LeaveRequestType.values()) {
            for (var status : AcademicStatus.values()) {
                if (status == (type.isLeave() ? AcademicStatus.ENROLLED : AcademicStatus.ON_LEAVE)) {
                    assertThatCode(() -> policy.validateAcademicStatus(status, type)).doesNotThrowAnyException();
                } else {
                    assertThatThrownBy(() -> policy.validateAcademicStatus(status, type))
                            .isInstanceOf(LeaveRequestConflictException.class);
                }
            }
        }
    }

    private LeaveRequestCreateRequestDTO general(short year, byte term) {
        return new LeaveRequestCreateRequestDTO(LeaveRequestType.GENERAL_LEAVE, "개인 사정",
                (short) 2026, (byte) 2, year, term);
    }
}
