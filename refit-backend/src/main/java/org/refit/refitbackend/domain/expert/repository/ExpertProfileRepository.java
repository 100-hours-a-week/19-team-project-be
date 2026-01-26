package org.refit.refitbackend.domain.expert.repository;

import org.refit.refitbackend.domain.expert.entity.ExpertProfile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, Long> {

    @Modifying
    @Query(value = "UPDATE expert_profiles SET embedding = CAST(:embedding AS vector) WHERE user_id = :userId", nativeQuery = true)
    int updateEmbedding(@Param("userId") Long userId, @Param("embedding") String embedding);
}
