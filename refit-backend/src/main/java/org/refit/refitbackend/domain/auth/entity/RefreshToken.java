package org.refit.refitbackend.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refresh_token")
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 500, nullable = false)
    private String token;

    @Column(name = "device_id", length = 100, nullable = false)
    private String deviceId;

    @Column(name = "device_type", length = 20, nullable = false)
    private String deviceType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RefreshTokenStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    private RefreshToken(
            User user,
            String token,
            String deviceId,
            String deviceType,
            RefreshTokenStatus status,
            LocalDateTime expiresAt
    ) {
        this.user = user;
        this.token = token;
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    /* =====================
     * 도메인 메서드
     * ===================== */

    public static RefreshToken create(
            User user,
            String token,
            String deviceId,
            String deviceType,
            LocalDateTime expiresAt
    ) {
        return RefreshToken.builder()
                .user(user)
                .token(token)
                .deviceId(deviceId)
                .deviceType(deviceType)
                .status(RefreshTokenStatus.ACTIVE)
                .expiresAt(expiresAt)
                .build();
    }
}
