# Thêm Logger & Debugger nâng cao cho AuctionUET Server

## Tổng quan

Hiện tại server chỉ dùng `System.out.println` rải rác, console trống và không có thông tin hữu ích khi debug. Mục tiêu là thêm một hệ thống logging chuyên nghiệp với 2 chế độ **DEVELOPMENT** và **PRODUCTION**, giúp theo dõi toàn bộ request lifecycle từ khi nhận raw JSON → parse → route → controller → service → response.

## Thiết kế tổng quan

### Thư viện: SLF4J + Logback

- **SLF4J** (Simple Logging Facade for Java) — facade API chuẩn.
- **Logback** — implementation mạnh nhất, native support cho SLF4J, hỗ trợ:
  - Console output có **màu sắc** (ANSI colors)
  - File rotation
  - Pattern layout tùy chỉnh
  - Cấu hình qua XML

### Hai chế độ Log

| Chế độ | Level | Mô tả |
|--------|-------|-------|
| **DEVELOPMENT** | `DEBUG` | Log **tất cả**: raw JSON nhận vào, action parsed, controller/service xử lý, data chi tiết, thời gian xử lý, response status |
| **PRODUCTION** | `INFO` | Log tóm tắt: action + status + timing. Không log raw data hay chi tiết sensitive |

### Output mẫu (DEV mode)

```
╔══════════════════════════════════════════════════════════════╗
║  AuctionUET Server v1.0-SNAPSHOT                            ║
║  Mode: DEVELOPMENT | Port: 8888                             ║
║  Started at: 2026-05-08 14:49:08                            ║
╚══════════════════════════════════════════════════════════════╝

14:49:10.123 [Thread-1] ▶ CLIENT CONNECTED  │ IP: 192.168.1.5
14:49:10.456 [Thread-1] ▶ RAW RECEIVED      │ {"action":"LOGIN","data":{"username":"seller1","password":"***"},"token":null}
14:49:10.457 [Thread-1] ▶ PARSED REQUEST    │ Action: LOGIN | Token: [none] | Data keys: [username, password]
14:49:10.458 [Thread-1] ▶ ROUTING           │ LOGIN → AuthController.handleLogin()
14:49:10.460 [Thread-1]   ├─ SERVICE        │ AuthService.login() | username=seller1
14:49:10.462 [Thread-1]   ├─ SESSION        │ Token created: abc-123... | User: seller1 (SELLER)
14:49:10.463 [Thread-1] ◀ RESPONSE [OK]     │ 7ms | Login thành công
14:49:15.100 [Thread-1] ▶ RAW RECEIVED      │ {"action":"CREATE_ITEM","data":{...},"token":"abc-123..."}
14:49:15.101 [Thread-1] ▶ PARSED REQUEST    │ Action: CREATE_ITEM | Token: abc-1*** | Data keys: [name, startingPrice, ...]
14:49:15.102 [Thread-1] ▶ ROUTING           │ CREATE_ITEM → ItemController.handleCreateItem()
14:49:15.103 [Thread-1]   ├─ AUTH           │ Token validated | User: seller1 (SELLER)
14:49:15.105 [Thread-1]   ├─ SERVICE        │ ItemService.createItem() | item=Laptop Gaming
14:49:15.108 [Thread-1] ◀ RESPONSE [OK]     │ 7ms
14:49:20.000 [Thread-1] ▶ EXCEPTION         │ AuctionException: Không có quyền CREATE_ITEM
14:49:20.001 [Thread-1] ◀ RESPONSE [ERROR]  │ 3ms | Không có quyền CREATE_ITEM
14:50:00.000 [Thread-1] ✖ CLIENT DISCONNECTED │ IP: 192.168.1.5 | Duration: 50s
```

---

## Proposed Changes

### 1. Maven Dependencies

#### [MODIFY] [pom.xml](file:///d:/Project/AuctionUET/pom.xml) (Parent POM)

Thêm dependency management cho SLF4J + Logback:
- `slf4j-api` 2.0.9
- `logback-classic` 1.4.14 (includes logback-core)

#### [MODIFY] [pom.xml](file:///d:/Project/AuctionUET/auction-server/pom.xml) (Server module)

Thêm dependency references (không cần version, kế thừa từ parent).

---

### 2. Logback Configuration

#### [NEW] [logback.xml](file:///d:/Project/AuctionUET/auction-server/src/main/resources/logback.xml)

File config Logback với:
- **Console Appender**: colored output, custom pattern `%d{HH:mm:ss.SSS} [%thread] %highlight(%-5level) %cyan(%logger{20}) │ %msg%n`
- **File Appender** (rolling): ghi vào `logs/server.log`, rotate daily, giữ 7 ngày
- Default root level = `DEBUG` (chuyển qua biến môi trường `LOG_LEVEL` hoặc system property)

---

### 3. Core Logger Utility

#### [NEW] [AppLogger.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/util/AppLogger.java)

Utility class cung cấp:
- `enum LogMode { DEVELOPMENT, PRODUCTION }` — chuyển đổi qua system property `-Dlog.mode=DEV|PROD`
- Helper methods tiện ích:
  - `logIncoming(rawJson)` — log raw JSON nhận vào (DEV only)
  - `logParsedRequest(request)` — log action, token mask, data keys
  - `logRouting(action, controllerName, methodName)` — log routing decision
  - `logServiceCall(serviceName, methodName, details)` — log service processing
  - `logResponse(status, durationMs, message)` — log response sent
  - `logException(exception)` — log exception chi tiết
  - `logConnection(event, ip)` — log client connect/disconnect
  - `maskSensitive(data)` — mask password, token trước khi log
  - `logBanner(port)` — print server startup banner

---

### 4. Tích hợp vào các Layer

#### [MODIFY] [ServerApp.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/ServerApp.java)

- Thay `System.out.println` bằng `AppLogger.logBanner()`
- Log startup info, port, mode

#### [MODIFY] [AuctionServer.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/server/AuctionServer.java)

- Log dependency initialization (DAO, Service, Controller, Router)
- Log server listening
- Log new client connection
- Log server shutdown
- Log server errors

#### [MODIFY] [ClientHandler.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/server/ClientHandler.java)

- `logIncoming(rawJson)` khi nhận raw line
- `logParsedRequest(request)` sau deserialize
- Log invalid JSON
- `logResponse()` khi gửi response
- Log client disconnect + session duration
- **Đo thời gian xử lý** (start → end) cho mỗi request

#### [MODIFY] [RequestRouter.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/server/RequestRouter.java)

- `logRouting(action, controller, method)` cho mỗi case trong switch
- Log unknown action
- Log exception khi catch

#### [MODIFY] [GlobalExceptionHandler.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/server/GlobalExceptionHandler.java)

- Thay `e.printStackTrace()` bằng structured logger
- Log exception type + message ở WARN level cho business exceptions
- Log full stack trace ở ERROR level cho unexpected exceptions

#### [MODIFY] [AuthController.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/controller/AuthController.java)

- Log login attempt (username, masked password)
- Log register attempt (username, role)
- Log logout

#### [MODIFY] [ItemController.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/controller/ItemController.java)

- Log createItem (user, item name)
- Log getMyItems (user, count returned)

#### [MODIFY] [AuctionController.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/controller/AuctionController.java)

- Log createAuction, startAuction, getAuctions, getAuctionDetail
- Thay `System.err.println` bằng `logger.warn()`

#### [MODIFY] [AuthService.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/service/AuthService.java)

- Log login flow: user lookup → password verify → session create
- Log register flow: validation → duplicate check → save

#### [MODIFY] [ItemService.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/service/ItemService.java)

- Log createItem, getItemsBySellerId, getItemById

#### [MODIFY] [AuctionService.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/service/AuctionService.java)

- Log createAuction, startAuction, endAuction, query methods

#### [MODIFY] [SessionManager.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/service/SessionManager.java)

- Log createSession, validateToken, removeSession, invalidateByUserId
- Log active session count

---

## Open Questions

> [!IMPORTANT]
> **Chế độ mặc định**: Mình sẽ để mặc định là `DEVELOPMENT` mode. Chuyển sang PRODUCTION bằng cách truyền `-Dlog.mode=PROD` khi chạy. Bạn OK với cách này không?

> [!NOTE]
> **File log**: Mình sẽ thêm file appender ghi log ra `auction-server/logs/server.log` (auto rotate). File này sẽ được gitignore. Bạn có cần file log không hay chỉ console là đủ?

---

## Verification Plan

### Automated Tests
- Build project: `mvn clean compile` — đảm bảo không lỗi compilation
- Chạy server và kiểm tra console output có đúng format, có đủ thông tin

### Manual Verification
- Start server ở DEV mode → kết nối client → thực hiện login, create item, create auction
- Kiểm tra console log hiển thị đầy đủ lifecycle của mỗi request
- Test error cases (sai password, thiếu quyền) → kiểm tra exception logging
- Chuyển sang PROD mode (`-Dlog.mode=PROD`) → kiểm tra log rút gọn
