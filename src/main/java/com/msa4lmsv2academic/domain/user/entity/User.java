package com.msa4lmsv2academic.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP, "
        + "email = CASE WHEN email IS NULL THEN NULL ELSE CONCAT(LEFT(email, 70), '#deleted#', id) END "
        + "WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {

    @Id
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String address;

    @Column(name = "profile_image_key", length = 500)
    private String profileImageKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private User(Long id, String name, String email, String phoneNumber, String address, UserRole role, UserStatus status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.role = role;
        this.status = status;
    }

    public static User synchronize(Long id, String name, String email, String phoneNumber, String address,
                                   UserRole role, UserStatus status) {
        return new User(id, name, email, phoneNumber, address, role, status);
    }

    public static User provision(Long id, String name, String email, String phoneNumber, String address,
                                 UserRole role) {
        return new User(id, name, email, phoneNumber, address, role, UserStatus.ACTIVE);
    }

    public void synchronizeAccount(UserRole role, UserStatus status) {
        this.role = role;
        this.status = status;
    }

    // 학적 정보 변경 신청 승인 시 반영한다. null인 필드는 변경하지 않는다.
    public void applyProfileChange(String name, String phoneNumber, String email, String address, String profileImageKey) {
        if (name != null) this.name = name;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (email != null) this.email = email;
        if (address != null) this.address = address;
        if (profileImageKey != null) this.profileImageKey = profileImageKey;
    }
}
