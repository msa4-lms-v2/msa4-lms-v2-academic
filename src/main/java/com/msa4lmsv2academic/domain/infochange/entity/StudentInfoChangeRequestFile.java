package com.msa4lmsv2academic.domain.infochange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(
        name = "student_info_change_request_files",
        indexes = @Index(name = "idx_student_info_change_request_files_request_id", columnList = "request_id")
)
public class StudentInfoChangeRequestFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private StudentInfoChangeRequest request;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private StudentInfoChangeRequestFile(
            StudentInfoChangeRequest request,
            String fileName,
            String objectKey,
            String contentType,
            long fileSize
    ) {
        this.request = request;
        this.fileName = fileName;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    public static StudentInfoChangeRequestFile create(
            StudentInfoChangeRequest request,
            String fileName,
            String objectKey,
            String contentType,
            long fileSize
    ) {
        return new StudentInfoChangeRequestFile(request, fileName, objectKey, contentType, fileSize);
    }
}
