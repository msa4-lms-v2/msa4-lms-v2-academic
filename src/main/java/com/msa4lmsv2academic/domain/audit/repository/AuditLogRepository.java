package com.msa4lmsv2academic.domain.audit.repository;

import com.msa4lmsv2academic.domain.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
