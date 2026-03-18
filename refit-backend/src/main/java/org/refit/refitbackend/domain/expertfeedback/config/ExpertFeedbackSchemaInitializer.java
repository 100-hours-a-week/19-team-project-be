package org.refit.refitbackend.domain.expertfeedback.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpertFeedbackSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS expert_feedbacks (
                    id BIGSERIAL PRIMARY KEY,
                    mentor_id BIGINT NOT NULL,
                    question TEXT NOT NULL,
                    answer TEXT NOT NULL,
                    job_tag VARCHAR(50) NOT NULL,
                    question_type VARCHAR(50) NOT NULL,
                    embedding_text TEXT,
                    source_type VARCHAR(20) NOT NULL DEFAULT 'seed',
                    quality_score SMALLINT NOT NULL DEFAULT 5 CHECK (quality_score BETWEEN 1 AND 5),
                    embedding vector(1024),
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_expert_feedbacks_embedding_cosine
                ON expert_feedbacks USING hnsw (embedding vector_cosine_ops)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_expert_feedbacks_job_tag
                ON expert_feedbacks (job_tag)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_expert_feedbacks_mentor_id
                ON expert_feedbacks (mentor_id)
                """);

        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION update_updated_at_column()
                RETURNS TRIGGER AS $$
                BEGIN
                    NEW.updated_at = NOW();
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);

        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1
                        FROM pg_trigger
                        WHERE tgname = 'trg_expert_feedbacks_updated_at'
                    ) THEN
                        CREATE TRIGGER trg_expert_feedbacks_updated_at
                        BEFORE UPDATE ON expert_feedbacks
                        FOR EACH ROW
                        EXECUTE FUNCTION update_updated_at_column();
                    END IF;
                END
                $$;
                """);

        log.info("expert_feedbacks schema initialized");
    }
}
