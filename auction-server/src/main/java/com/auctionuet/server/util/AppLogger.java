package com.auctionuet.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/**
 * Utility logger tập trung cho toàn bộ AuctionUET Server.
 *
 * <p><b>Hai chế độ hoạt động:</b>
 * <ul>
 *   <li><b>DEVELOPMENT</b> (mặc định): Log chi tiết toàn bộ lifecycle — raw JSON,
 *       parsed action, routing, service calls, timing, response status.</li>
 *   <li><b>PRODUCTION</b>: Chỉ log tóm tắt action + status + timing.
 *       Không log raw data hay thông tin nhạy cảm.</li>
 * </ul>
 *
 * <p><b>Cách chuyển mode:</b> Truyền {@code -Dlog.mode=PROD} khi chạy server.
 * Mặc định là DEV mode.
 */
public class AppLogger {

    // ──────────────────── MODE ────────────────────

    public enum LogMode {
        DEVELOPMENT,
        PRODUCTION;

        public static LogMode current() {
            String prop = System.getProperty("log.mode", "DEV").toUpperCase();
            return prop.startsWith("PROD") ? PRODUCTION : DEVELOPMENT;
        }
    }

    // ──────────────────── LOGGERS ────────────────────

    // Loggers riêng theo layer, để dễ filter trong log file
    private static final Logger SERVER     = LoggerFactory.getLogger("SERVER");
    private static final Logger NETWORK    = LoggerFactory.getLogger("NETWORK");
    private static final Logger ROUTING    = LoggerFactory.getLogger("ROUTING");
    private static final Logger CONTROLLER = LoggerFactory.getLogger("CONTROLLER");
    private static final Logger SERVICE    = LoggerFactory.getLogger("SERVICE");
    private static final Logger SESSION    = LoggerFactory.getLogger("SESSION");
    private static final Logger EXCEPTION  = LoggerFactory.getLogger("EXCEPTION");

    // Các trường nhạy cảm cần mask khi log
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password", "pass", "secret", "hashedPassword", "passwordSalt"
    );

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ──────────────────── BANNER ────────────────────

    /**
     * In banner khởi động server ra console.
     */
    public static void logBanner(String appName, String version, int port, LogMode mode) {
        String line = "═".repeat(60);
        String modeLabel = mode == LogMode.DEVELOPMENT ? "DEVELOPMENT 🔧" : "PRODUCTION 🚀";
        SERVER.info("\n" +
            "╔" + line + "╗\n" +
            "║  {} v{}{}║\n" +
            "║  Mode: {}{}║\n" +
            "║  Port: {}{}║\n" +
            "║  Started: {}{}║\n" +
            "╚" + line + "╝",
            appName, version, padRight("", 60 - appName.length() - version.length() - 4),
            modeLabel, padRight("", 60 - modeLabel.length() - 8),
            port, padRight("", 60 - String.valueOf(port).length() - 8),
            LocalDateTime.now().format(DT_FMT), padRight("", 60 - 21)
        );
    }

    // ──────────────────── NETWORK LAYER ────────────────────

    /**
     * Log khi client kết nối.
     */
    public static void logClientConnected(String ip, int port, int activeCount) {
        NETWORK.info("▶ CLIENT CONNECTED  │ {}:{} │ Active connections: {}", ip, port, activeCount);
    }

    /**
     * Log khi client ngắt kết nối.
     */
    public static void logClientDisconnected(String ip, long durationMs, int activeCount) {
        if (durationMs > 0) {
            NETWORK.info("✖ CLIENT DISCONNECTED │ {} │ Duration: {}s │ Active: {}", ip, durationMs / 1000, activeCount);
        } else {
            NETWORK.info("✖ CLIENT DISCONNECTED │ {} │ Active: {}", ip, activeCount);
        }
    }

    /**
     * Log raw JSON nhận vào từ client (DEV only, mask sensitive fields).
     */
    public static void logRawReceived(String rawJson) {
        if (LogMode.current() == LogMode.DEVELOPMENT) {
            String masked = maskSensitiveJson(rawJson);
            NETWORK.debug("▶ RAW RECEIVED      │ {}", masked);
        }
    }

    /**
     * Log thông tin request sau khi deserialize.
     */
    public static void logParsedRequest(String action, String token, java.util.Collection<String> dataKeys) {
        if (LogMode.current() == LogMode.DEVELOPMENT) {
            String tokenDisplay = maskToken(token);
            String keys = dataKeys != null ? dataKeys.toString() : "[]";
            NETWORK.debug("▶ PARSED REQUEST    │ Action: {} │ Token: {} │ Data keys: {}",
                action, tokenDisplay, keys);
        }
    }

    /**
     * Log khi request null hoặc JSON không hợp lệ.
     */
    public static void logInvalidRequest(String reason) {
        NETWORK.warn("⚠ INVALID REQUEST   │ {}", reason);
    }

    /**
     * Log response được gửi về client, kèm thời gian xử lý.
     */
    public static void logResponse(String status, long durationMs, String message) {
        if ("OK".equals(status)) {
            NETWORK.info("◀ RESPONSE [{}]     │ {}ms{}", status, durationMs,
                message != null && !message.isEmpty() ? " │ " + message : "");
        } else {
            NETWORK.warn("◀ RESPONSE [{}]  │ {}ms │ {}", status, durationMs,
                message != null ? message : "(no message)");
        }
    }

    // ──────────────────── ROUTING LAYER ────────────────────

    /**
     * Log quyết định routing: action → controller → method.
     */
    public static void logRouting(String action, String controller, String method) {
        if (LogMode.current() == LogMode.DEVELOPMENT) {
            ROUTING.debug("▶ ROUTING           │ {} → {}.{}()", action, controller, method);
        }
    }

    /**
     * Log action không được nhận diện.
     */
    public static void logUnknownAction(String action) {
        ROUTING.warn("⚠ UNKNOWN ACTION    │ {}", action);
    }

    // ──────────────────── CONTROLLER LAYER ────────────────────

    /**
     * Log bắt đầu xử lý ở controller.
     */
    public static void logControllerEnter(String controller, String method, String detail) {
        if (LogMode.current() == LogMode.DEVELOPMENT) {
            CONTROLLER.debug("  ├─ CONTROLLER     │ {}.{}() │ {}", controller, method, detail);
        }
    }

    /**
     * Log kết quả xử lý ở controller.
     */
    public static void logControllerResult(String controller, String method, String result) {
        if (LogMode.current() == LogMode.DEVELOPMENT) {
            CONTROLLER.debug("  └─ CTRL RESULT    │ {}.{}() │ {}", controller, method, result);
        }
    }

    // ──────────────────── SERVICE LAYER ────────────────────

    /**
     * Log bắt đầu xử lý ở service.
     */
    public static void logServiceCall(String service, String method, String detail) {
        if (LogMode.current() == LogMode.DEVELOPMENT) {
            SERVICE.debug("  ├─ SERVICE        │ {}.{}() │ {}", service, method, detail);
        }
    }

    /**
     * Log kết quả xử lý ở service.
     */
    public static void logServiceResult(String service, String method, String result) {
        if (LogMode.current() == LogMode.DEVELOPMENT) {
            SERVICE.debug("  └─ SVC RESULT     │ {}.{}() │ {}", service, method, result);
        }
    }

    // ──────────────────── SESSION LAYER ────────────────────

    /**
     * Log tạo session mới.
     */
    public static void logSessionCreated(String username, String role, String tokenPrefix) {
        SESSION.info("  ├─ SESSION        │ Created │ User: {} ({}) │ Token: {}...",
            username, role, tokenPrefix);
    }

    /**
     * Log validate token thành công.
     */
    public static void logTokenValidated(String username, String role) {
        if (LogMode.current() == LogMode.DEVELOPMENT) {
            SESSION.debug("  ├─ AUTH           │ Token valid │ User: {} ({})", username, role);
        }
    }

    /**
     * Log xóa session.
     */
    public static void logSessionRemoved(String tokenPrefix) {
        SESSION.info("  ├─ SESSION        │ Removed │ Token: {}...", tokenPrefix);
    }

    // ──────────────────── EXCEPTION LAYER ────────────────────

    /**
     * Log business exception ở mức WARN (không cần stack trace).
     */
    public static void logBusinessException(Exception e) {
        EXCEPTION.warn("⚠ EXCEPTION [{}] │ {}", e.getClass().getSimpleName(), e.getMessage());
    }

    /**
     * Log system/unexpected exception ở mức ERROR kèm stack trace.
     */
    public static void logSystemException(Exception e) {
        EXCEPTION.error("✖ SYSTEM ERROR [{}] │ {}", e.getClass().getSimpleName(), e.getMessage(), e);
    }

    // ──────────────────── SERVER LIFECYCLE ────────────────────

    /**
     * Log khởi tạo dependency (DAO, Service, Controller, Router).
     */
    public static void logInit(String component, String detail) {
        SERVER.info("  ✓ INIT            │ {} {}", component, detail != null ? "│ " + detail : "");
    }

    /**
     * Log server bắt đầu lắng nghe.
     */
    public static void logServerListening(int port) {
        SERVER.info("◎ SERVER LISTENING  │ Port: {}", port);
    }

    /**
     * Log server shutdown.
     */
    public static void logServerStopped() {
        SERVER.info("◎ SERVER STOPPED");
    }

    /**
     * Log server lỗi nghiêm trọng.
     */
    public static void logServerError(String message, Exception e) {
        SERVER.error("✖ SERVER ERROR      │ {}", message, e);
    }

    // ──────────────────── HELPERS ────────────────────

    /**
     * Mask token: chỉ hiển thị 8 ký tự đầu.
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) return "[none]";
        if (token.length() <= 8) return token + "***";
        return token.substring(0, 8) + "***";
    }

    /**
     * Mask các giá trị nhạy cảm trong raw JSON string (simple string replace).
     */
    private static String maskSensitiveJson(String json) {
        if (json == null) return "null";
        // Mask password fields: "password":"xxx" → "password":"***"
        String result = json;
        for (String key : SENSITIVE_KEYS) {
            result = result.replaceAll(
                "\"" + key + "\"\\s*:\\s*\"[^\"]*\"",
                "\"" + key + "\":\"***\""
            );
        }
        return result;
    }

    private static String padRight(String s, int n) {
        if (n <= 0) return s;
        return s + " ".repeat(n);
    }
}
