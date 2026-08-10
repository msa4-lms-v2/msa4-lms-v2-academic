package com.msa4lmsv2academic.domain.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity @Getter @EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP, email = CONCAT(LEFT(email, 70), '#deleted#', id) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {
    @Id @EqualsAndHashCode.Include private Long id;
    @Column(nullable = false, length = 50) private String name;
    @Column(nullable = false, length = 100) private String email;
    @Column(name = "phone_number", length = 30) private String phoneNumber;
    @Column(length = 255) private String address;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private UserRole role;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private UserStatus status;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @Column(name = "deleted_at") private LocalDateTime deletedAt;

    private User(Long id, String name, String email, String phoneNumber, String address, UserRole role, UserStatus status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.role = role;
        this.status = status;
    }

    public static User synchronize(Long id, String name, String email, String phoneNumber, String address, UserRole role, UserStatus status) {
        return new User(id, name, email, phoneNumber, address, role, status);
    }
}
