package com.msa4lmsv2academic.domain.evaluation.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class LectureEvaluationTimeProvider {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    public LocalDateTime now() {
        return LocalDateTime.now(SERVICE_ZONE);
    }
}
