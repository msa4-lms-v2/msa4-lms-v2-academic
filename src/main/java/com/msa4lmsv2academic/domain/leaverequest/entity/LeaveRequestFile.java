package com.msa4lmsv2academic.domain.leaverequest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "leave_request_files", indexes =
        @Index(name = "idx_leave_request_files_request", columnList = "request_id"))
public class LeaveRequestFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private LeaveRequest request;
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;
    @Column(name = "stored_name", nullable = false, length = 500)
    private String storedName;
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;
    @Column(nullable = false)
    private long size;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static LeaveRequestFile create(LeaveRequest request, String originalName, String storedName,
                                          String contentType, long size) {
        LeaveRequestFile file = new LeaveRequestFile();
        file.request = request;
        file.originalName = originalName;
        file.storedName = storedName;
        file.contentType = contentType;
        file.size = size;
        return file;
    }
}
