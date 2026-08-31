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
@Table(name = "academic_change_request_files", uniqueConstraints =
        @UniqueConstraint(name = "uk_academic_change_request_files_type", columnNames = {"request_id", "document_type"}),
        indexes = @Index(name = "idx_academic_change_request_files_request", columnList = "request_id"))
public class AcademicChangeRequestFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private AcademicChangeRequest request;
    @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false, length = 30)
    private TransferDocumentType documentType;
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

    public static AcademicChangeRequestFile create(AcademicChangeRequest request, TransferDocumentType documentType,
                                                    String originalName, String storedName, String contentType,
                                                    long size) {
        AcademicChangeRequestFile file = new AcademicChangeRequestFile();
        file.request = request;
        file.documentType = documentType;
        file.originalName = originalName;
        file.storedName = storedName;
        file.contentType = contentType;
        file.size = size;
        return file;
    }
}
