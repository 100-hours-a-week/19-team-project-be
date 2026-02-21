-- Expert search performance indexes for PostgreSQL (prefix search: keyword%)
-- Execute once in each environment (dev/prod) before load testing.

-- lower(column) LIKE 'keyword%' indexes
CREATE INDEX IF NOT EXISTS idx_users_nickname_prefix
    ON users (lower(nickname) text_pattern_ops);

CREATE INDEX IF NOT EXISTS idx_users_introduction_prefix
    ON users (lower(introduction) text_pattern_ops);

CREATE INDEX IF NOT EXISTS idx_jobs_name_prefix
    ON jobs (lower(name) text_pattern_ops);

CREATE INDEX IF NOT EXISTS idx_skills_name_prefix
    ON skills (lower(name) text_pattern_ops);

-- EXISTS filter indexes
CREATE INDEX IF NOT EXISTS idx_user_jobs_user_id_job_id
    ON user_jobs (user_id, job_id);

CREATE INDEX IF NOT EXISTS idx_user_skills_user_id_skill_id
    ON user_skills (user_id, skill_id);
