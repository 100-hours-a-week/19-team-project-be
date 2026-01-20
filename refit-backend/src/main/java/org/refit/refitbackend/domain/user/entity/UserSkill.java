package org.refit.refitbackend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.domain.master.entity.Skill;
import org.refit.refitbackend.global.common.entity.BaseEntity;

@Entity
@Table(
        name = "user_skills",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "skill_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSkill extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private Integer displayOrder;

    @Builder
    private UserSkill(User user, Skill skill, Integer displayOrder) {
        this.user = user;
        this.skill = skill;
        this.displayOrder = displayOrder;
    }

    public static UserSkill of(User user, Skill skill, Integer displayOrder) {
        return new UserSkill(user, skill, displayOrder);
    }

}
