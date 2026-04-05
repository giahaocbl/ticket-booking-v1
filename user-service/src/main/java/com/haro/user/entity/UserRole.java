package com.haro.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @Column(nullable = false, length = 50)
    private String role;

    @Column(name = "granted_at", nullable = false)
    private OffsetDateTime grantedAt;

    @PrePersist
    void onCreate() {
        if (grantedAt == null) {
            grantedAt = OffsetDateTime.now();
        }
    }
}