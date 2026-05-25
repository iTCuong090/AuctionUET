package com.auctionuet.server.domain.manager;

import com.auctionuet.server.domain.model.User;
import com.auctionuet.server.exception.AuthenticationException;

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
        return token;
    }

    public User validateToken(String token) {
        User user = tokenMap.get(token);
        if (user == null) {
            throw new AuthenticationException("Token không hợp lệ hoặc đã hết hạn");
        }
        return user;
    }

    public void removeSession(String token) {
        tokenMap.remove(token);
    }

    public void updateSessionUser(String token, User user) {
        if (token != null && user != null && tokenMap.containsKey(token)) {
            tokenMap.put(token, user);
        }
    }

    public void invalidateByUserId(String userId) {
        tokenMap.entrySet().removeIf(entry -> entry.getValue().getId().equals(userId));
    }
}
