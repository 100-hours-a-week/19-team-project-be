package org.refit.refitbackend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.domain.user.entity.enums.Role;
import org.refit.refitbackend.domain.master.entity.CareerLevel;
import org.refit.refitbackend.domain.user.entity.enums.UserType;
import org.refit.refitbackend.global.common.entity.BaseEntity;

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

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(nullable = false, length = 255)
    private String profileImageUrl = "https://cdn.refit.com/default-profile.png";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role =  Role.USER;

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
        this.profileImageUrl = "https://cdn.refit.com/default-profile.png";
    }


}
