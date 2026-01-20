package org.refit.refitbackend.domain.master.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.master.dto.MasterRes;
import org.refit.refitbackend.domain.master.entity.Skill;
import org.refit.refitbackend.domain.master.repository.CareerLevelRepository;
import org.refit.refitbackend.domain.master.repository.JobRepository;
import org.refit.refitbackend.domain.master.repository.SkillRepository;
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

}
