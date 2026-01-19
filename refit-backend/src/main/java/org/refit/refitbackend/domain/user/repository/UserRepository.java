package org.refit.refitbackend.domain.user.repository;

import org.refit.refitbackend.domain.user.entity.OAuthProvider;
import org.refit.refitbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByOauthProviderAndOauthId(OAuthProvider oauthProvider, String oauthId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
