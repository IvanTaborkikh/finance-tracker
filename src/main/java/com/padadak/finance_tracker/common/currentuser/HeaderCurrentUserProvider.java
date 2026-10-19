package com.padadak.finance_tracker.common.currentuser;

import com.padadak.finance_tracker.common.exception.UnauthorizedException;
import com.padadak.finance_tracker.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

// ТИМЧАСОВА реалізація: юзер береться з заголовка X-User-Id.
// Пізніше заміна на JWT = нова реалізація цього інтерфейсу, решта коду не зміниться.
@Component
public class HeaderCurrentUserProvider implements CurrentUserProvider {

    static final String HEADER = "X-User-Id";

    private final HttpServletRequest request;
    private final UserRepository userRepository;

    // Constructor injection: Spring сам передасть сюди потрібні beans.
    // HttpServletRequest тут — проксі, який щоразу вказує на ПОТОЧНИЙ запит.
    public HeaderCurrentUserProvider(HttpServletRequest request, UserRepository userRepository) {
        this.request = request;
        this.userRepository = userRepository;
    }

    @Override
    public Long currentUserId() {
        String raw = request.getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            throw new UnauthorizedException("Missing " + HEADER + " header");
        }
        long userId;
        try {
            userId = Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("Invalid " + HEADER + " header");
        }
        if (!userRepository.existsById(userId)) {
            throw new UnauthorizedException("Unknown user");
        }
        return userId;
    }
}