package org.refit.refitbackend.domain.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.refit.refitbackend.domain.task.entity.enums.TaskStatus;
import org.refit.refitbackend.domain.task.entity.enums.TaskType;
import org.refit.refitbackend.domain.task.entity.enums.TaskTargetType;
import org.refit.refitbackend.global.common.entity.BaseEntity;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends BaseEntity {

    @Id
    @Column(length = 50, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskType type;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column
    private Integer progress;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    private TaskTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String result;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Builder
    private Task(
            String id,
            Long userId,
            TaskType type,
            String fileUrl,
            String payload,
            TaskStatus status,
            Integer progress,
            TaskTargetType targetType,
            Long targetId
    ) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.fileUrl = fileUrl;
        this.payload = payload;
        this.status = status;
        this.progress = progress;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public void markCompleted(String result) {
        this.status = TaskStatus.COMPLETED;
        this.result = result;
        this.progress = 100;
        this.failReason = null;
    }

    public void markFailed(String reason) {
        this.status = TaskStatus.FAILED;
        this.failReason = reason;
    }
}
