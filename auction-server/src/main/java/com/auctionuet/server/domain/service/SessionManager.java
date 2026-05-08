package com.auctionuet.server.domain.service;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.util.AppLogger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SessionManager {

    // Singleton không lazy, khởi tạo luôn không cần đợi getInstance mới tạo.
    private static final SessionManager INSTANCE = new SessionManager();
    private final Map<String, User> tokenMap = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        tokenMap.put(token, user);

        // Log token prefix (8 chars) + username + role
        AppLogger.logSessionCreated(
            user.getUsername(),
            user.getRole().name(),
            token.substring(0, 8)
        );
        return token;
    }

    public User validateToken(String token) {
        User user = tokenMap.get(token);
        if (user == null) {
            throw new AuthenticationException("Token không hợp lệ hoặc đã hết hạn");
        }
        AppLogger.logTokenValidated(user.getUsername(), user.getRole().name());
        return user;
    }

    public void removeSession(String token) {
        User user = tokenMap.remove(token);
        String tokenPrefix = (token != null && token.length() >= 8) ? token.substring(0, 8) : token;
        AppLogger.logSessionRemoved(tokenPrefix);
    }

    public void invalidateByUserId(String userId) {
        int before = tokenMap.size();
        tokenMap.entrySet().removeIf(entry -> entry.getValue().getId().equals(userId));
        int removed = before - tokenMap.size();
        AppLogger.logServiceCall("SessionManager", "invalidateByUserId",
            "userId=" + userId + " | Removed " + removed + " session(s) | Active: " + tokenMap.size());
    }
}
