package org.refit.refitbackend.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.user.dto.UserRes;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public UserRes.Me getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        return UserRes.Me.from(user);
    }

    /**
     * 닉네임 중복 검사
     */
    public UserRes.NicknameCheck checkNickname(String nickname) {
        validateNickname(nickname);
        boolean exists = userRepository.existsByNickname(nickname);
        return new UserRes.NicknameCheck(nickname, exists, !exists);
    }

    /**
     * 현직자 검색
     */
    /**
     * 모든 유저 검색
     */
    public CursorPage<UserRes.UserSearch> searchUsers(
            String keyword,
            Long jobId,
            Long skillId,
            Long cursorId,
            int size
    ) {
        List<User> users = userRepository.searchUsersByCursor(
                keyword,
                jobId,
                skillId,
                cursorId,
                PageRequest.of(0, size + 1)
        );

        boolean hasMore = users.size() > size;
        if (hasMore) {
            users = users.subList(0, size);
        }

        List<UserRes.UserSearch> items = users.stream()
                .map(UserRes.UserSearch::from)
                .toList();

        String nextCursor = users.isEmpty() ? null : String.valueOf(users.get(users.size() - 1).getId());

        return new CursorPage<>(items, nextCursor, hasMore);
    }

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new CustomException(ExceptionType.NICKNAME_EMPTY);
        }
        String trimmed = nickname.trim();
        int length = trimmed.length();
        if (length < 2) {
            throw new CustomException(ExceptionType.NICKNAME_TOO_SHORT);
        }
        if (length > 10) {
            throw new CustomException(ExceptionType.NICKNAME_TOO_LONG);
        }
        if (trimmed.contains(" ")) {
            throw new CustomException(ExceptionType.NICKNAME_CONTAINS_WHITESPACE);
        }
        if (!trimmed.matches("^[A-Za-z0-9가-힣]+$")) {
            throw new CustomException(ExceptionType.NICKNAME_INVALID_CHARACTERS);
        }
    }
}
