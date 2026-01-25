package org.refit.refitbackend.domain.master.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.master.dto.MasterRes;
import org.refit.refitbackend.domain.master.entity.EmailDomain;
import org.refit.refitbackend.domain.master.entity.Skill;
import org.refit.refitbackend.domain.master.repository.CareerLevelRepository;
import org.refit.refitbackend.domain.master.repository.EmailDomainRepository;
import org.refit.refitbackend.domain.master.repository.JobRepository;
import org.refit.refitbackend.domain.master.repository.SkillRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterService {

    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final CareerLevelRepository careerLevelRepository;
    private final EmailDomainRepository emailDomainRepository;

    public MasterRes.Jobs getJobs() {
        return new MasterRes.Jobs(
                jobRepository.findAll()
                        .stream()
                        .map(MasterRes.MasterItemDto::from)
                        .toList()
        );
    }

    public MasterRes.Skills getSkills(String keyword) {
        List<Skill> skills = (keyword == null || keyword.isBlank())
                ? skillRepository.findAll()
                : skillRepository.findByNameContainingIgnoreCase(keyword);

        return new MasterRes.Skills(
                skills.stream()
                        .map(MasterRes.MasterItemDto::from)
                        .toList()
        );
    }

    public MasterRes.CareerLevels getCareerLevels() {
        return new MasterRes.CareerLevels(
                careerLevelRepository.findAll()
                        .stream()
                        .map(MasterRes.CareerLevelDto::from)
                        .toList()
        );
    }

    public MasterRes.EmailDomains getEmailDomains(String cursor, int size) {
        List<EmailDomain> domains = emailDomainRepository.findByCursor(
                cursor,
                PageRequest.of(0, size + 1)
        );

        boolean hasMore = domains.size() > size;
        if (hasMore) {
            domains = domains.subList(0, size);
        }

        List<MasterRes.EmailDomainDto> items = domains.stream()
                .map(MasterRes.EmailDomainDto::from)
                .toList();

        String nextCursor = domains.isEmpty() ? null : domains.get(domains.size() - 1).getDomain();

        return new MasterRes.EmailDomains(items, nextCursor, hasMore);
    }

}
