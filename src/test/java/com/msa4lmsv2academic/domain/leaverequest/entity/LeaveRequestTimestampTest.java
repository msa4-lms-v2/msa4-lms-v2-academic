package com.msa4lmsv2academic.domain.leaverequest.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;

class LeaveRequestTimestampTest {
    @Test void JVM이_UTC여도_신청과_변경_시각은_KST이고_신청시각은_보존된다() {
        var previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            var request = LeaveRequest.create(null, LeaveRequestType.GENERAL_LEAVE, "검증",
                    (short) 2026, (byte) 2, (short) 2027, (byte) 1);
            request.initializeTimestamps();
            var created = request.getCreatedAt();
            assertTrue(Duration.between(created, LocalDateTime.now(ZoneId.of("Asia/Seoul"))).abs().toSeconds() < 2);
            request.updateTimestamp();
            assertEquals(created, request.getCreatedAt());
            assertFalse(request.getUpdatedAt().isBefore(created));
            assertTrue(Duration.between(request.getUpdatedAt(), LocalDateTime.now(ZoneId.of("Asia/Seoul"))).abs().toSeconds() < 2);
        } finally {
            TimeZone.setDefault(previous);
        }
    }
}
