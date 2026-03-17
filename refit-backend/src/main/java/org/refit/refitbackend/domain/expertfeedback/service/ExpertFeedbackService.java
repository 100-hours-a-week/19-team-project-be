package org.refit.refitbackend.domain.expertfeedback.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.expertfeedback.dto.ExpertFeedbackReq;
import org.refit.refitbackend.domain.expertfeedback.dto.ExpertFeedbackRes;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpertFeedbackService {

    private static final String INSERT_SQL = """
            insert into expert_feedbacks (
                mentor_id,
                question,
                answer,
                job_tag,
                question_type,
                embedding_text,
                source_type,
                quality_score,
                embedding
            ) values (?, ?, ?, ?, ?, ?, ?, ?, cast(? as vector))
            """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public ExpertFeedbackRes.CreatedId create(ExpertFeedbackReq.CreateFeedback request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
            bind(ps, request);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return new ExpertFeedbackRes.CreatedId(key == null ? null : key.longValue());
    }

    @Transactional
    public ExpertFeedbackRes.BatchInsertResult createBatch(ExpertFeedbackReq.BatchCreateFeedback request) {
        List<ExpertFeedbackReq.CreateFeedback> feedbacks = request.feedbacks();
        int[] results = jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                bind(ps, feedbacks.get(i));
            }

            @Override
            public int getBatchSize() {
                return feedbacks.size();
            }
        });

        int insertedCount = 0;
        for (int result : results) {
            if (result >= 0 || result == Statement.SUCCESS_NO_INFO) {
                insertedCount++;
            }
        }
        return new ExpertFeedbackRes.BatchInsertResult(insertedCount);
    }

    private void bind(PreparedStatement ps, ExpertFeedbackReq.CreateFeedback request) throws java.sql.SQLException {
        ps.setLong(1, request.mentorId());
        ps.setString(2, request.question());
        ps.setString(3, request.answer());
        ps.setString(4, request.jobTag().value());
        ps.setString(5, request.questionType().value());
        ps.setString(6, request.embeddingText());
        ps.setString(7, request.sourceType().value());
        ps.setInt(8, request.qualityScore());

        String vectorLiteral = toVectorLiteral(request.embedding());
        if (vectorLiteral == null) {
            ps.setNull(9, Types.VARCHAR);
        } else {
            ps.setString(9, vectorLiteral);
        }
    }

    private String toVectorLiteral(List<Float> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            Float value = embedding.get(i);
            builder.append(value == null ? 0.0f : value);
        }
        return builder.append(']').toString();
    }
}
