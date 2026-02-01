package org.refit.refitbackend.domain.auth.repository;

import org.refit.refitbackend.domain.auth.entity.RefreshToken;
import org.refit.refitbackend.domain.auth.entity.RefreshTokenStatus;
import org.refit.refitbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndStatus(String token, RefreshTokenStatus status);

    @Modifying
    @Query("update RefreshToken rt set rt.status = :newStatus where rt.token = :token")
    int updateStatusByToken(
            @Param("token") String token,
            @Param("newStatus") RefreshTokenStatus newStatus
    );

    @Modifying
    @Query("update RefreshToken rt set rt.status = :newStatus where rt.user = :user and rt.status = :currentStatus")
    int updateStatusByUserAndStatus(
            @Param("user") User user,
            @Param("currentStatus") RefreshTokenStatus currentStatus,
            @Param("newStatus") RefreshTokenStatus newStatus
    );
}
