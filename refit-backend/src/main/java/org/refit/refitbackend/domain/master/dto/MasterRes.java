package org.refit.refitbackend.domain.master.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.refit.refitbackend.domain.master.entity.CareerLevel;
import org.refit.refitbackend.domain.master.entity.Job;
import org.refit.refitbackend.domain.master.entity.Skill;

import java.util.List;

public class MasterRes {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CareerLevelDto(
            Long id,
            String level
    ) {
        public static CareerLevelDto from(CareerLevel careerLevel) {
            return new CareerLevelDto(
                    careerLevel.getId(),
                    careerLevel.getLevel()
            );
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MasterItemDto(
            Long id,
            String name
    ) {
        public static MasterItemDto from(Job job) {
            return new MasterItemDto(job.getId(), job.getName());
        }

        public static MasterItemDto from(Skill skill) {
            return new MasterItemDto(skill.getId(), skill.getName());
        }
    }

    public record Jobs(List<MasterItemDto> jobs) {}
    public record Skills(List<MasterItemDto> skills) {}
    public record CareerLevels(List<CareerLevelDto> careerLevels) {}

}
