package org.refit.refitbackend.domain.resume.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.global.common.entity.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "resumes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resume extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String title;

    @Column(name = "is_fresher", nullable = false)
    private boolean isFresher;

    @Column(name = "education_level", nullable = false)
    private String educationLevel;

    @Column(name = "file_url")
    private String fileUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", nullable = false, columnDefinition = "JSONB")
    private String contentJson;

    @Builder
    private Resume(User user, String title, boolean isFresher, String educationLevel, String fileUrl, String contentJson) {
        this.user = user;
        this.title = title;
        this.isFresher = isFresher;
        this.educationLevel = educationLevel;
        this.fileUrl = fileUrl;
        this.contentJson = contentJson;
    }

    public void update(String title, Boolean isFresher, String educationLevel, String fileUrl, String contentJson) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (isFresher != null) {
            this.isFresher = isFresher;
        }
        if (educationLevel != null && !educationLevel.isBlank()) {
            this.educationLevel = educationLevel;
        }
        if (fileUrl != null) {
            this.fileUrl = fileUrl;
        }
        if (contentJson != null) {
            this.contentJson = contentJson;
        }
    }

    public void updateTitle(String title) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
    }
}
