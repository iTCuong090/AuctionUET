package com.auctionuet.server.network.server;

import com.auctionuet.server.exception.AuctionException;
import com.auctionuet.server.exception.AuthenticationException;
import com.auctionuet.server.exception.DuplicateUserException;
import com.auctionuet.server.exception.UserNotFoundException;
import com.auctionuet.server.network.protocol.Response;
import com.auctionuet.server.util.AppLogger;

import java.time.format.DateTimeParseException;

/**
 * Xử lý exception tập trung cho toàn bộ server.
 *
 * <p>Phân loại exception thành 2 nhóm:
 * <ul>
 *   <li><b>Business exceptions</b>: Log ở mức WARN (không cần stack trace)</li>
 *   <li><b>System exceptions</b>: Log ở mức ERROR kèm full stack trace</li>
 * </ul>
 */
public class GlobalExceptionHandler {

    public static Response handle(Exception e) {

        // ── Business Exceptions (WARN — không cần stack trace) ──

        if (e instanceof AuthenticationException) {
            AppLogger.logBusinessException(e);
            String msg = e.getMessage() != null && !e.getMessage().isEmpty()
                ? e.getMessage()
                : "Token không hợp lệ hoặc sai thông tin đăng nhập";
            return Response.error(msg);
        }

        if (e instanceof UserNotFoundException) {
            AppLogger.logBusinessException(e);
            return Response.error("User không tồn tại");
        }

        if (e instanceof DuplicateUserException) {
            AppLogger.logBusinessException(e);
            return Response.error("Username đã tồn tại");
        }

        if (e instanceof AuctionException) {
            AppLogger.logBusinessException(e);
            return Response.error(e.getMessage());
        }

        if (e instanceof IllegalArgumentException) {
            AppLogger.logBusinessException(e);
            return Response.error("Dữ liệu không hợp lệ: " + e.getMessage());
        }

        if (e instanceof DateTimeParseException) {
            AppLogger.logBusinessException(e);
            return Response.error("Định dạng thời gian không hợp lệ");
        }

        // ── System / Unexpected Exceptions (ERROR — kèm stack trace) ──
        AppLogger.logSystemException(e);
        return Response.error("Lỗi Server: " + e.getMessage());
    }
}
