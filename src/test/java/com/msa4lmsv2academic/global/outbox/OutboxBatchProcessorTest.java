package com.msa4lmsv2academic.global.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.msa4lmsv2academic.domain.admission.client.AdmissionAccountClient;
import com.msa4lmsv2academic.domain.outbox.entity.*;
import com.msa4lmsv2academic.domain.outbox.repository.OutboxEventRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutboxBatchProcessorTest {
    @Mock OutboxEventRepository outboxEventRepository;
    @Mock KafkaTemplate<Object, Object> kafkaTemplate;
    @Mock ObjectMapper objectMapper;
    @Mock AdmissionAccountClient admissionAccountClient;
    @InjectMocks OutboxBatchProcessor processor;

    @Test
    void admissionEventCallsAuthAndCompletesWithoutKafka() {
        var event = OutboxEvent.create("ADMISSION_CANDIDATE", 7L, "AdmissionCandidateRegistered", Map.of("admissionCandidateId", 7L), 1L);
        when(outboxEventRepository.lockNextBatch(any(), anyInt())).thenReturn(List.of(event));
        processor.publishPendingBatch();
        verify(admissionAccountClient).createAccount(event.getPayload());
        verifyNoInteractions(kafkaTemplate);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.COMPLETED);
    }

    @Test
    void transientAuthFailureKeepsRegistrationQueuedForRetry() {
        var event = OutboxEvent.create("ADMISSION_CANDIDATE", 7L, "AdmissionCandidateRegistered", Map.of("admissionCandidateId", 7L), 1L);
        when(outboxEventRepository.lockNextBatch(any(), anyInt())).thenReturn(List.of(event));
        doThrow(new ResourceAccessException("timeout")).when(admissionAccountClient).createAccount(any());
        processor.publishPendingBatch();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
    }
}
