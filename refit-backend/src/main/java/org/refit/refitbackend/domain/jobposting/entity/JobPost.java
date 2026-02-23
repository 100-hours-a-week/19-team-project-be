package org.refit.refitbackend.domain.jobposting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.refit.refitbackend.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_posts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_job_posts_source_job", columnNames = {"source", "source_job_id"}),
                @UniqueConstraint(name = "uk_job_posts_url_hash", columnNames = {"url_hash"})
        },
        indexes = {
                @Index(name = "idx_job_posts_is_active", columnList = "is_active"),
                @Index(name = "idx_job_posts_deadline_at", columnList = "deadline_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "source_job_id", nullable = false, length = 100)
    private String sourceJobId;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "url_hash", nullable = false, length = 64)
    private String urlHash;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 200)
    private String company;

    @Column(length = 200)
    private String department;

    @Column(length = 200)
    private String location;

    @Column(name = "employment_type", length = 50)
    private String employmentType;

    @Column(name = "experience_required", length = 100)
    private String experienceRequired;

    @Column(name = "education_required", length = 100)
    private String educationRequired;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tech_stack", columnDefinition = "JSONB")
    private String techStack;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requirements", columnDefinition = "JSONB")
    private String requirements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", columnDefinition = "JSONB")
    private String preferences;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "responsibilities", columnDefinition = "JSONB")
    private String responsibilities;

    @Column(name = "description_raw", columnDefinition = "TEXT")
    private String descriptionRaw;

    @Column(name = "description_clean", columnDefinition = "TEXT")
    private String descriptionClean;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "crawled_at", nullable = false)
    private LocalDateTime crawledAt;

    @Builder
    private JobPost(
            String source,
            String sourceJobId,
            String url,
            String urlHash,
            String title,
            String company,
            String department,
            String location,
            String employmentType,
            String experienceRequired,
            String educationRequired,
            String techStack,
            String requirements,
            String preferences,
            String responsibilities,
            String descriptionRaw,
            String descriptionClean,
            LocalDateTime postedAt,
            LocalDateTime deadlineAt,
            Boolean isActive,
            LocalDateTime crawledAt
    ) {
        this.source = source;
        this.sourceJobId = sourceJobId;
        this.url = url;
        this.urlHash = urlHash;
        this.title = title;
        this.company = company;
        this.department = department;
        this.location = location;
        this.employmentType = employmentType;
        this.experienceRequired = experienceRequired;
        this.educationRequired = educationRequired;
        this.techStack = techStack;
        this.requirements = requirements;
        this.preferences = preferences;
        this.responsibilities = responsibilities;
        this.descriptionRaw = descriptionRaw;
        this.descriptionClean = descriptionClean;
        this.postedAt = postedAt;
        this.deadlineAt = deadlineAt;
        this.isActive = isActive != null ? isActive : true;
        this.crawledAt = crawledAt != null ? crawledAt : LocalDateTime.now();
    }

    public void markInactive() {
        this.isActive = false;
    }

    public void updateFromCrawler(
            String title,
            String company,
            String department,
            String employmentType,
            String experienceRequired,
            String educationRequired,
            String requirements,
            String preferences,
            String techStack,
            String responsibilities,
            String descriptionRaw
    ) {
        this.title = title;
        this.company = company;
        this.department = department;
        this.employmentType = employmentType;
        this.experienceRequired = experienceRequired;
        this.educationRequired = educationRequired;
        this.requirements = requirements;
        this.preferences = preferences;
        this.techStack = techStack;
        this.responsibilities = responsibilities;
        this.descriptionRaw = descriptionRaw;
        this.isActive = true;
        this.crawledAt = LocalDateTime.now();
    }
}
