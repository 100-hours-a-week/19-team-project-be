package org.refit.refitbackend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.domain.user.entity.enums.Role;
import org.refit.refitbackend.domain.master.entity.CareerLevel;
import org.refit.refitbackend.domain.expert.entity.ExpertProfile;
import org.refit.refitbackend.domain.user.entity.enums.UserType;
import org.refit.refitbackend.global.common.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_oauth",
                        columnNames = {"oauth_provider", "oauth_id"}
                ),
                @UniqueConstraint(
                        name = "uk_user_email",
                        columnNames = {"email"}
                ),
                @UniqueConstraint(
                        name = "uk_user_nickname",
                        columnNames = {"nickname"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "career_level_id", nullable = false)
    private CareerLevel careerLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider oauthProvider;

    @Column(nullable = false, length = 100)
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType userType;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(length = 255)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role =  Role.USER;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserJob> userJobs = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSkill> userSkills = new ArrayList<>();

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private ExpertProfile expertProfile;

    public void updateUserType(UserType userType) {
        this.userType = userType;
    }

    public void attachExpertProfile(ExpertProfile expertProfile) {
        this.expertProfile = expertProfile;
    }

    public void clearExpertProfile() {
        this.expertProfile = null;
    }


    @Builder
    private User(CareerLevel careerLevel, OAuthProvider oauthProvider, String oauthId, UserType userType, String email, String nickname, String introduction
    ) {
        this.careerLevel = careerLevel;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.userType = userType;
        this.email = email;
        this.nickname = nickname;
        this.introduction = introduction;

        this.role = Role.USER;
    }

    public void updateProfile(String email, String nickname) {
        this.email = email != null ? email : this.email;
        this.nickname = (nickname != null && !nickname.isBlank()) ? nickname : this.nickname;
    }

    public void updateNickname(String nickname) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
    }

    public void updateIntroduction(String introduction) {
        if (introduction != null) {
            this.introduction = introduction;
        }
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public void clearProfileImageUrl() {
        this.profileImageUrl = null;
    }

    public void updateCareerLevel(CareerLevel careerLevel) {
        if (careerLevel != null) {
            this.careerLevel = careerLevel;
        }
    }

    public void replaceUserJobs(List<UserJob> jobs) {
        this.userJobs.clear();
        if (jobs != null) {
            this.userJobs.addAll(jobs);
        }
    }

    public void replaceUserSkills(List<UserSkill> skills) {
        this.userSkills.clear();
        if (skills != null) {
            this.userSkills.addAll(skills);
        }
    }
}
