package org.refit.refitbackend.domain.resume.service;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import org.refit.refitbackend.domain.resume.dto.ResumeReq;
import org.refit.refitbackend.domain.resume.dto.ResumeRes;
import org.refit.refitbackend.domain.resume.entity.Resume;
import org.refit.refitbackend.domain.resume.repository.ResumeRepository;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

    private static final int MAX_RESUMES = 5;

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ResumeRes.ResumeId create(Long userId, ResumeReq.Create request) {
        validateResumeLimit(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));

        Resume resume = Resume.builder()
                .user(user)
                .title(request.title())
                .isFresher(request.isFresher())
                .educationLevel(request.educationLevel())
                .fileUrl(request.fileUrl())
                .contentJson(toJson(request.contentJson()))
                .build();

        Resume saved = resumeRepository.save(resume);
        return new ResumeRes.ResumeId(saved.getId());
    }

    public ResumeRes.ResumeListResponse getMyResumes(Long userId) {
        List<ResumeRes.ResumeListItem> items = resumeRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toListItem)
                .toList();

        return new ResumeRes.ResumeListResponse(items);
    }

    public ResumeRes.ResumeDetail getDetail(Long userId, Long resumeId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.RESUME_NOT_FOUND));
        return toDetail(resume);
    }

    @Transactional
    public void update(Long userId, Long resumeId, ResumeReq.Update request) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.RESUME_NOT_FOUND));

        resume.update(
                request.title(),
                request.isFresher(),
                request.educationLevel(),
                request.fileUrl(),
                request.contentJson() != null ? toJson(request.contentJson()) : null
        );
    }

    @Transactional
    public void updateTitle(Long userId, Long resumeId, ResumeReq.UpdateTitle request) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.RESUME_NOT_FOUND));

        resume.updateTitle(request.title());
    }

    @Transactional
    public void delete(Long userId, Long resumeId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.RESUME_NOT_FOUND));
        resumeRepository.delete(resume);
    }

    private void validateResumeLimit(Long userId) {
        if (resumeRepository.countByUserId(userId) >= MAX_RESUMES) {
            throw new CustomException(ExceptionType.RESUME_LIMIT_EXCEEDED);
        }
    }

    private ResumeRes.ResumeListItem toListItem(Resume resume) {
        return new ResumeRes.ResumeListItem(
                resume.getId(),
                resume.getTitle(),
                resume.getIsFresher(),
                resume.getEducationLevel(),
                resume.getFileUrl(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }

    private ResumeRes.ResumeDetail toDetail(Resume resume) {
        return new ResumeRes.ResumeDetail(
                resume.getId(),
                resume.getTitle(),
                resume.getIsFresher(),
                resume.getEducationLevel(),
                resume.getFileUrl(),
                parseJson(resume.getContentJson()),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new CustomException(ExceptionType.INVALID_JSON);
        }
    }

    private JsonNode parseJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            throw new CustomException(ExceptionType.INVALID_JSON);
        }
    }
}
