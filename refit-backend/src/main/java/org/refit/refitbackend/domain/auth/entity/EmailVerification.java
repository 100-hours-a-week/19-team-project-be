package org.refit.refitbackend.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 10)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailVerificationStatus status;

    @Column(name = "sent_count", nullable = false)
    private int sentCount;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Builder
    private EmailVerification(User user, String email, String code, int sentCount, LocalDateTime expiresAt) {
        this.user = user;
        this.email = email;
        this.code = code;
        this.sentCount = sentCount;
        this.expiresAt = expiresAt;
        this.status = EmailVerificationStatus.PENDING;
    }

    public void updateForResend(String code, int sentCount, LocalDateTime expiresAt) {
        this.code = code;
        this.sentCount = sentCount;
        this.expiresAt = expiresAt;
        this.status = EmailVerificationStatus.PENDING;
        this.verifiedAt = null;
    }

    public void markVerified(LocalDateTime verifiedAt) {
        this.status = EmailVerificationStatus.VERIFIED;
        this.verifiedAt = verifiedAt;
    }

    public void markExpired() {
        this.status = EmailVerificationStatus.EXPIRED;
    }
}
