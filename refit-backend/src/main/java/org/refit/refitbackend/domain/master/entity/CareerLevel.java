package org.refit.refitbackend.domain.master.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "career_levels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareerLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String level;
}
