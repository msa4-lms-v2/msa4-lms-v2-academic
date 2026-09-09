package com.msa4lmsv2academic.domain.transfer.entity;

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
@Table(name = "academic_change_request_files",
        indexes = @Index(name = "idx_academic_change_request_files_request", columnList = "request_id"))
public class AcademicChangeRequestFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private AcademicChangeRequest request;
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

    public static AcademicChangeRequestFile create(AcademicChangeRequest request, String originalName,
                                                    String storedName, String contentType, long size) {
        AcademicChangeRequestFile file = new AcademicChangeRequestFile();
        file.request = request;
        file.originalName = originalName;
        file.storedName = storedName;
        file.contentType = contentType;
        file.size = size;
        return file;
    }

    public void replace(String originalName, String storedName, String contentType, long size) {
        if (originalName == null || originalName.isBlank() || storedName == null || storedName.isBlank()
                || contentType == null || contentType.isBlank() || size <= 0) {
            throw new IllegalArgumentException("교체할 학적 변경 서류 정보가 올바르지 않습니다.");
        }
        this.originalName = originalName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.size = size;
    }
}
