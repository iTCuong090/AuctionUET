# 🅱️ NHIỆM VỤ CHI TIẾT — KHÁNH (Tuần 4: Network mở rộng + Phân quyền + AuctionController)

> **Tuần:** 4 · **Branch:** `feature/tuan-4-khanh-auction-network`
>
> **Tài liệu tham chiếu bắt buộc đọc trước khi code:**
> - `docs/server_architecture_overhaul.md` — Mục 4.4 (Network), Mục 3 (Package)
> - `docs/diagrams_flow_auction.md` — TOÀN BỘ (Create Item, Create/Start/End Auction, Get Auctions)
> - `docs/diagrams_flow_admin_user.md` — Mục 6 (Permission Matrix)
> - `planning/WEEK4/WEEK4_OVERVIEW.md` — Contracts 10, 11

---

## MỤC LỤC

1. [PHẦN A — Hiểu vai trò tuần 4 của Khánh](#phần-a--hiểu-vai-trò-tuần-4-của-khánh)
2. [PHẦN B — Nửa đầu tuần: Phân quyền + ActionType + Controller skeleton](#phần-b--nửa-đầu-tuần-phân-quyền--actiontype--controller-skeleton)
3. [PHẦN C — Nửa sau tuần: AuctionController hoàn chỉnh + Router cập nhật](#phần-c--nửa-sau-tuần-auctioncontroller-hoàn-chỉnh--router-cập-nhật)
4. [PHẦN D — Test bắt buộc](#phần-d--test-bắt-buộc)
5. [PHẦN E — Dependency & Nhắc nhở phối hợp](#phần-e--dependency--nhắc-nhở-phối-hợp)
6. [PHẦN F — Checklist hoàn thành](#phần-f--checklist-hoàn-thành)

---

## PHẦN A — Hiểu vai trò tuần 4 của Khánh

### A.1 Tuần 4 Khánh làm gì?

Tuần 2-3, Khánh đã xây Protocol (Request, Response, ActionType), Socket server (AuctionServer, ClientHandler), và AuthController + RequestRouter.

Tuần 4 Khánh có **2 nhiệm vụ lớn**:

1. **Hệ thống phân quyền (Permission System):** Mỗi Controller method phải kiểm tra quyền trước khi xử lý. Bidder không được tạo sản phẩm, Seller không được quản lý user, v.v.

2. **AuctionController:** Xử lý tất cả request liên quan đến sản phẩm và phiên đấu giá: `CREATE_ITEM`, `CREATE_AUCTION`, `START_AUCTION`, `GET_AUCTIONS`, `GET_AUCTION_DETAIL`.

```
TUẦN 2-3 (đã có):                   TUẦN 4 (MỚI/CẬP NHẬT):
├── protocol/                        ├── protocol/
│   ├── ActionType ✅                │   └── ActionType ← CẬP NHẬT (thêm 5+ actions)
│   ├── Request ✅                   │
│   ├── Response ✅                  │
│   └── MessageSerializer ✅         │
├── controller/                      ├── controller/
│   └── AuthController ✅            │   ├── AuthController ✅ (thêm permission check)
│                                    │   └── AuctionController ← MỚI (6 methods)
└── server/                          └── server/
    ├── AuctionServer ✅                 ├── AuctionServer ← CẬP NHẬT (inject mới)
    ├── ClientHandler ✅                 ├── ClientHandler ✅
    └── RequestRouter ✅                 └── RequestRouter ← CẬP NHẬT (thêm routes)
```

### A.2 Hệ thống phân quyền — Nguyên tắc thiết kế

Phân quyền **đã được thiết kế sẵn** trong Domain Model (Anh làm tuần 2):
- `User.hasPermission(String action)` → `boolean` (abstract)
- `Bidder.hasPermission()` → chỉ cho phép PLACE_BID, VIEW_AUCTION, VIEW_BID_HISTORY
- `Seller.hasPermission()` → chỉ cho phép CREATE_ITEM, CREATE_AUCTION, START_AUCTION, VIEW_AUCTION
- `Admin.hasPermission()` → cho phép MANAGE_USERS, VIEW_AUCTION

**Vai trò của Controller:** Gọi `user.hasPermission(ACTION)` **TRƯỚC** khi gọi Service. Nếu false → trả `Response.error("Bạn không có quyền...")` ngay, KHÔNG gọi Service.

**Pattern chung cho MỌI controller method cần phân quyền:**

```java
public Response handleXxx(Request request) {
    try {
        // Bước 1: Xác thực token
        User user = SessionManager.getInstance().validateToken(request.getToken());

        // Bước 2: Kiểm tra quyền
        if (!user.hasPermission("ACTION_NAME")) {
            return Response.error("Bạn không có quyền thực hiện hành động này");
        }

        // Bước 3: Xử lý logic nghiệp vụ
        // ... gọi Service ...

        // Bước 4: Trả response
        return Response.ok(data);
    } catch (AuthenticationException e) {
        return Response.error("Token không hợp lệ");
    } catch (AuctionException e) {
        return Response.error(e.getMessage());
    } catch (Exception e) {
        return Response.error("Lỗi server: " + e.getMessage());
    }
}
```

---

## PHẦN B — Nửa đầu tuần: Phân quyền + ActionType + Controller skeleton

### B.1 Cập nhật: `ActionType.java`

**Thêm các giá trị mới:**

```java
public enum ActionType {
    // Auth (tuần 2-3 — đã có)
    LOGIN, REGISTER, LOGOUT,

    // User Profile (tuần 4 — mới)
    GET_PROFILE, UPDATE_PROFILE,

    // Item Management (tuần 4 — mới)
    CREATE_ITEM, UPDATE_ITEM, DELETE_ITEM, GET_MY_ITEMS,

    // Auction Management (tuần 4 — mới)
    CREATE_AUCTION, START_AUCTION, GET_AUCTIONS, GET_AUCTION_DETAIL,

    // Bidding (tuần 5 — để sẵn)
    PLACE_BID, GET_BID_HISTORY, SUBSCRIBE, UNSUBSCRIBE,

    // Admin (tuần 5 — để sẵn)
    GET_ALL_USERS, DELETE_USER, UPDATE_ROLE,

    // Utils
    PING
}
```

---

### B.2 Cập nhật: `Bidder.java`, `Seller.java`, `Admin.java` — hasPermission

> [!NOTE]
> Phần này **thuộc về Anh** (Domain), nhưng Khánh cần đảm bảo bảng quyền phù hợp. Khánh cần **phối hợp với Anh** để thống nhất action names.

**Bảng quyền chi tiết (Khánh cần biết để kiểm tra trong Controller):**

| Action | Bidder | Seller | Admin |
|--------|--------|--------|-------|
| `CREATE_ITEM` | ❌ | ✅ | ❌ |
| `UPDATE_ITEM` | ❌ | ✅ | ❌ |
| `DELETE_ITEM` | ❌ | ✅ | ❌ |
| `GET_MY_ITEMS` | ❌ | ✅ | ❌ |
| `CREATE_AUCTION` | ❌ | ✅ | ❌ |
| `START_AUCTION` | ❌ | ✅ | ❌ |
| `VIEW_AUCTION` / `GET_AUCTIONS` | ✅ | ✅ | ✅ |
| `GET_AUCTION_DETAIL` | ✅ | ✅ | ✅ |
| `PLACE_BID` | ✅ | ❌ | ❌ |
| `VIEW_BID_HISTORY` | ✅ | ✅ | ✅ |
| `GET_PROFILE` | ✅ | ✅ | ✅ |
| `UPDATE_PROFILE` | ✅ | ✅ | ✅ |
| `MANAGE_USERS` / `GET_ALL_USERS` | ❌ | ❌ | ✅ |
| `DELETE_USER` | ❌ | ❌ | ✅ |
| `UPDATE_ROLE` | ❌ | ❌ | ✅ |

---

### B.3 File MỚI: `AuctionController.java` (skeleton nửa đầu tuần)

**Đường dẫn:** `network/controller/AuctionController.java`

**Vai trò:** Xử lý TẤT CẢ request liên quan đến sản phẩm và phiên đấu giá.

**Khai báo:**
```java
public class AuctionController
```

**Trường:**

| Trường | Kiểu | Nguồn |
|--------|------|-------|
| `auctionService` | `AuctionService` | Inject qua constructor |
| `sessionManager` | `SessionManager` | `SessionManager.getInstance()` |

**Constructor:**
```java
public AuctionController(AuctionService auctionService)
```

**Nửa đầu tuần — skeleton (chưa gọi Service, chỉ kiểm tra quyền):**

```java
public Response handleCreateItem(Request request) {
    try {
        User user = sessionManager.validateToken(request.getToken());
        if (!user.hasPermission("CREATE_ITEM")) {
            return Response.error("Chỉ Seller mới được đăng sản phẩm");
        }
        // TODO: Gọi auctionService.createItem() (nửa sau tuần)
        return Response.error("Chưa implement");
    } catch (AuthenticationException e) {
        return Response.error("Token không hợp lệ");
    }
}

// Tương tự cho: handleCreateAuction, handleStartAuction, handleGetAuctions, handleGetAuctionDetail
```

---

### B.4 Cập nhật: `AuthController.java` — Thêm permission check

Tuần 2-3, AuthController chưa kiểm tra quyền (vì LOGIN/REGISTER/LOGOUT không cần quyền đặc biệt). Tuần 4, cần review lại và đảm bảo:

- `handleLogin()` → KHÔNG cần permission check (ai cũng login được)
- `handleRegister()` → KHÔNG cần permission check (ai cũng register được)
- `handleLogout()` → Chỉ cần token hợp lệ (đã kiểm tra rồi)

> Không cần sửa lớn, chỉ cần review.

---

## PHẦN C — Nửa sau tuần: AuctionController hoàn chỉnh + Router cập nhật

### C.1 `AuctionController.java` — Hoàn chỉnh 6 methods

#### Method 1: `handleCreateItem`

```java
public Response handleCreateItem(Request request)
```

| Bước | Hành động | Code |
|------|-----------|------|
| 1 | Validate token | `User user = sessionManager.validateToken(request.getToken())` |
| 2 | Check quyền | `if (!user.hasPermission("CREATE_ITEM"))` → error |
| 3 | Lấy dữ liệu | `Map<String, Object> itemData = request.getData()` |
| 4 | Gọi Service | `ItemSchema schema = auctionService.createItem(user, itemData)` |
| 5 | Chuyển DTO | `ItemDTO dto = ItemMapper.toDTO(schema)` |
| 6 | Trả response | `return Response.ok(dto)` |

**Error handling:**

| Exception | Response |
|-----------|---------|
| `AuthenticationException` | `Response.error("Token không hợp lệ")` |
| `IllegalArgumentException` | `Response.error("Dữ liệu sản phẩm không hợp lệ: " + e.getMessage())` |
| `AuctionException` | `Response.error(e.getMessage())` |

---

#### Method 2: `handleCreateAuction`

```java
public Response handleCreateAuction(Request request)
```

| Bước | Code |
|------|------|
| 1 | `User user = sessionManager.validateToken(request.getToken())` |
| 2 | `if (!user.hasPermission("CREATE_AUCTION"))` → error |
| 3 | Lấy: `itemId`, `startTime`, `endTime`, `title`, `description` từ `request.getData()` |
| 4 | Parse `startTime`, `endTime` → `LocalDateTime` |
| 5 | `AuctionSchema schema = auctionService.createAuction(user, itemId, startTime, endTime, title, description)` |
| 6 | Lấy ItemSchema → tạo ItemDTO |
| 7 | `AuctionDTO dto = AuctionMapper.toDTO(schema, itemDTO)` |
| 8 | `return Response.ok(dto)` |

---

#### Method 3: `handleStartAuction`

```java
public Response handleStartAuction(Request request)
```

| Bước | Code |
|------|------|
| 1 | Validate token → User |
| 2 | Check: `user.hasPermission("START_AUCTION")` |
| 3 | `String auctionId = (String) request.getData().get("auctionId")` |
| 4 | `auctionService.startAuction(user, auctionId)` |
| 5 | `return Response.ok("Phiên đấu giá đã bắt đầu")` |

**Error:**
- `AuctionException("Phiên đấu giá không tồn tại")` → error
- `AuctionException("Bạn không phải chủ phiên đấu giá này")` → error
- `AuctionException("Phiên đấu giá không ở trạng thái OPEN")` → error

---

#### Method 4: `handleGetAuctions`

```java
public Response handleGetAuctions(Request request)
```

| Bước | Code |
|------|------|
| 1 | Validate token → User |
| 2 | Check: `user.hasPermission("VIEW_AUCTION")` |
| 3 | `List<AuctionSchema> auctions = auctionService.getAuctions()` |
| 4 | Loop: Với mỗi AuctionSchema → lấy ItemSchema → tạo ItemDTO → tạo AuctionDTO |
| 5 | `return Response.ok(auctionDTOList)` |

> [!TIP]
> **Tối ưu:** Khánh có thể thêm filter parameter (`status`, `sellerId`) trong `request.getData()` để Client có thể lọc. Nếu không có filter → trả tất cả.

---

#### Method 5: `handleGetAuctionDetail`

```java
public Response handleGetAuctionDetail(Request request)
```

| Bước | Code |
|------|------|
| 1 | Validate token |
| 2 | `String auctionId = (String) request.getData().get("auctionId")` |
| 3 | `AuctionSchema schema = auctionService.getAuctionById(auctionId)` |
| 4 | Nếu null → error |
| 5 | Lấy ItemSchema → ItemDTO → AuctionDTO |
| 6 | `return Response.ok(auctionDTO)` |

---

#### Method 6: `handleGetMyItems` (bonus — dành cho Seller xem sản phẩm của mình)

```java
public Response handleGetMyItems(Request request)
```

| Bước | Code |
|------|------|
| 1 | Validate token → User |
| 2 | Check: `user.hasPermission("CREATE_ITEM")` (chỉ Seller có) |
| 3 | `List<ItemSchema> items = auctionService.getItemsBySeller(user.getId())` |
| 4 | Loop: mỗi ItemSchema → ItemDTO |
| 5 | `return Response.ok(itemDTOList)` |

---

### C.2 Cập nhật: `RequestRouter.java`

```java
public Response route(Request request) {
    if (request == null || request.getAction() == null) {
        return Response.error("Invalid request: missing action");
    }

    switch (request.getAction()) {
        // Auth (tuần 2-3)
        case LOGIN:
            return authController.handleLogin(request);
        case REGISTER:
            return authController.handleRegister(request);
        case LOGOUT:
            return authController.handleLogout(request);

        // Item (tuần 4 — MỚI)
        case CREATE_ITEM:
            return auctionController.handleCreateItem(request);
        case GET_MY_ITEMS:
            return auctionController.handleGetMyItems(request);

        // Auction (tuần 4 — MỚI)
        case CREATE_AUCTION:
            return auctionController.handleCreateAuction(request);
        case START_AUCTION:
            return auctionController.handleStartAuction(request);
        case GET_AUCTIONS:
            return auctionController.handleGetAuctions(request);
        case GET_AUCTION_DETAIL:
            return auctionController.handleGetAuctionDetail(request);

        // Utils
        case PING:
            return Response.ok("PONG");

        default:
            return Response.error("Unknown action: " + request.getAction());
    }
}
```

**Cập nhật constructor:**
```java
public RequestRouter(AuthController authController, AuctionController auctionController)
```

---

### C.3 Cập nhật: `AuctionServer.java`

Trong method `start()`, tạo và inject AuctionController:

```java
// Tạo DAO
UserDAO userDAO = DataManager.getInstance().getUserDAO();
ItemDAO itemDAO = DataManager.getInstance().getItemDAO();
AuctionDAO auctionDAO = DataManager.getInstance().getAuctionDAO();

// Tạo Service
AuthService authService = new AuthService(userDAO);
AuctionService auctionService = new AuctionService(itemDAO, auctionDAO);

// Tạo Controller
AuthController authController = new AuthController(authService);
AuctionController auctionController = new AuctionController(auctionService);

// Tạo Router
RequestRouter router = new RequestRouter(authController, auctionController);

// Vòng lặp accept
while (isRunning) {
    Socket clientSocket = serverSocket.accept();
    ClientHandler handler = new ClientHandler(clientSocket, router);
    new Thread(handler).start();
}
```

---

## PHẦN D — Test bắt buộc

### D.1 Test phân quyền (JUnit — `PermissionTest.java`)

| # | Test name | Mô tả |
|---|-----------|-------|
| 1 | `testBidderCannotCreateItem` | Bidder gửi CREATE_ITEM → Response.error chứa "quyền" |
| 2 | `testSellerCanCreateItem` | Seller gửi CREATE_ITEM → Response.ok (hoặc lỗi khác, nhưng KHÔNG phải lỗi quyền) |
| 3 | `testBidderCannotCreateAuction` | Bidder gửi CREATE_AUCTION → từ chối |
| 4 | `testSellerCannotManageUsers` | Seller gửi GET_ALL_USERS → từ chối |
| 5 | `testAdminCannotCreateItem` | Admin gửi CREATE_ITEM → từ chối |
| 6 | `testEveryoneCanViewAuctions` | Cả 3 role gửi GET_AUCTIONS → OK |
| 7 | `testNoTokenReturnsError` | Request không có token → error "Token không hợp lệ" |
| 8 | `testInvalidTokenReturnsError` | Token giả → error |

### D.2 Test AuctionController (JUnit — `AuctionControllerTest.java`)

| # | Test name | Mô tả |
|---|-----------|-------|
| 1 | `testCreateItemResponseFormat` | Response chứa ItemDTO với đủ trường |
| 2 | `testCreateAuctionResponseFormat` | Response chứa AuctionDTO |
| 3 | `testStartAuctionSuccess` | Response.ok("Phiên đấu giá đã bắt đầu") |
| 4 | `testGetAuctionsReturnsList` | Response.data là List<AuctionDTO> |
| 5 | `testRouteCreateItem` | Router chuyển đúng đến handleCreateItem |
| 6 | `testRouteGetAuctions` | Router chuyển đúng đến handleGetAuctions |

### D.3 Test thủ công (E2E qua socket)

```
1. Chạy Server
2. Telnet → REGISTER seller account (role=SELLER)
3. LOGIN → lấy token
4. CREATE_ITEM kèm token → nhận OK + ItemDTO
5. CREATE_AUCTION kèm itemId + token → nhận OK + AuctionDTO
6. START_AUCTION kèm auctionId + token → nhận OK
7. REGISTER bidder account (role=BIDDER)
8. LOGIN bidder → token
9. GET_AUCTIONS kèm bidder token → nhận danh sách auction
10. CREATE_ITEM kèm bidder token → nhận ERROR "Bạn không có quyền"
```

---

## PHẦN E — Dependency & Nhắc nhở phối hợp

### E.1 Nửa đầu tuần: Khánh hầu như KHÔNG phụ thuộc ai

Khánh chỉ cần:
- `ActionType` enum values mới (Khánh tự thêm)
- `User.hasPermission()` đã có từ tuần 2

### E.2 Nửa sau tuần: Khánh phụ thuộc Cường + Anh

| Khánh cần | Ai cung cấp | Deadline cho họ |
|-----------|-------------|----------------|
| `AuctionService` (5 methods) | **Cường** | **Thứ 5** |
| `ItemMapper.toDTO()` | **Anh** | Thứ 4 (merge giữa tuần) |
| `AuctionMapper.toDTO()` | **Anh** | Thứ 4 |
| `ItemDTO`, `AuctionDTO` classes | **Anh** | Thứ 4 |

> [!WARNING]
> **NHẮC NHỞ:**
> - **Thứ 5:** Khánh nhắc Cường: "Cường ơi, push AuctionService chưa?"
> - Nếu Cường chưa push → Khánh tạo mock AuctionService:
>   ```java
>   // Mock tạm
>   public ItemSchema createItem(...) {
>       return new ElectronicsSchema(...); // hardcode
>   }
>   ```

### E.3 Công phụ thuộc Khánh

| Công cần | Khánh phải cung cấp | Deadline |
|----------|---------------------|---------|
| Protocol format cho CREATE_ITEM | Contract 11 (đã thống nhất) | Đầu tuần |
| Protocol format cho GET_AUCTIONS | Contract 11 | Đầu tuần |
| Server chạy được với AuctionController | Server thực | **Thứ 6** |

---

## PHẦN F — Checklist hoàn thành

### Nửa đầu tuần — Deadline: Tối thứ 4

- [ ] `ActionType.java` — Cập nhật thêm 6+ actions mới
- [ ] `AuctionController.java` — Skeleton 6 methods với permission check
- [ ] Bảng quyền thống nhất với Anh (hasPermission action names)
- [ ] **8 permission tests ALL PASS** ✅
- [ ] Code push lên branch

### Nửa sau tuần — Deadline: Tối thứ 7

- [ ] `AuctionController.java` — 6 methods hoàn chỉnh (gọi Service thật)
- [ ] `RequestRouter.java` — Cập nhật routing cho 6+ actions mới
- [ ] `AuctionServer.java` — Inject AuctionController + AuctionService
- [ ] **6 controller tests ALL PASS** ✅
- [ ] Test thủ công E2E qua telnet: 10 bước đều pass
- [ ] Code push lên branch
- [ ] Tham gia meeting cuối tuần, demo live

---

> **Ghi chú:** Phần quan trọng nhất tuần 4 của Khánh là **hệ thống phân quyền**. Nó là pattern xuyên suốt mọi Controller method từ đây về sau. Làm tốt tuần 4 → tuần 5+ chỉ cần copy pattern cho BidController, AdminController.
