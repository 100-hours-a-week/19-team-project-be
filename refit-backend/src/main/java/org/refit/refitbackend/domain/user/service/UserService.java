package org.refit.refitbackend.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.user.dto.UserRes;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserRes.Detail getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        return UserRes.Detail.from(user);
    }
}
