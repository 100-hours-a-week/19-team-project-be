package org.refit.refitbackend.domain.expert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "expert_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertProfile extends BaseEntity {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(name = "company_email", length = 255)
    private String companyEmail;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "rating_avg", nullable = false)
    private double ratingAvg = 0.0;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount = 0;

    @Column(name = "responded_request_count", nullable = false)
    private int respondedRequestCount = 0;

    @Column(name = "accepted_request_count", nullable = false)
    private int acceptedRequestCount = 0;

    @Column(name = "rejected_request_count", nullable = false)
    private int rejectedRequestCount = 0;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    private ExpertProfile(User user, String companyName, String companyEmail) {
        this.user = user;
        this.companyName = companyName;
        this.companyEmail = companyEmail;
        this.verified = false;
        this.ratingAvg = 0.0;
        this.ratingCount = 0;
        this.respondedRequestCount = 0;
        this.acceptedRequestCount = 0;
        this.rejectedRequestCount = 0;
    }

    public static ExpertProfile create(User user, String companyName, String companyEmail) {
        ExpertProfile profile = new ExpertProfile(user, companyName, companyEmail);
        user.attachExpertProfile(profile);
        return profile;
    }

    public void markVerified(LocalDateTime verifiedAt) {
        this.verified = true;
        this.verifiedAt = verifiedAt;
    }
}
