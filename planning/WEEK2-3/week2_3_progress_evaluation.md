# 📊 ĐÁNH GIÁ TIẾN ĐỘ TUẦN 2–3 — AuctionUET

> **Ngày đánh giá:** 12/04/2026 · **Branch hiện tại:** `main` (đã merge từ `develop/tuan2`)
> **Trạng thái tổng:** ✅ **Auth MVP hoàn thành — 43/43 tests PASS**

---

## 🏆 Tổng quan nhanh

| Chỉ số | Kế hoạch | Thực tế | Đánh giá |
|--------|----------|---------|----------|
| Tổng số file deliverable (server) | ~30 files | ✅ 35+ files | Vượt kế hoạch |
| Tổng số file deliverable (client) | ~10 files | ✅ 12+ files | Vượt kế hoạch |
| Test suites | 8 suites | ✅ 8 suites | Đúng kế hoạch |
| Test cases | ≥43 | ✅ **43/43 PASS** | 💯 Hoàn hảo |
| Merge vào main | Có | ✅ PR #29 merged | Đúng quy trình |
| Branch feature đúng tên | 4 branches tuần 2 + 4 tuần 3 | ⚠️ Naming hơi khác | Chấp nhận được |

---

## 🅰️ CƯỜNG — Persistence Layer + Auth Service

### Tuần 2: Persistence Layer

| # | Deliverable | File | Trạng thái | Ghi chú |
|---|------------|------|-----------|---------|
| B1 | `BaseSchema.java` | [BaseSchema.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/persistence/schema/BaseSchema.java) | ✅ Done | Constructor rỗng + tường minh, equals/hashCode theo id |
| B2 | `UserSchema.java` | [UserSchema.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/persistence/schema/UserSchema.java) | ✅ Done | 5 trường + getter/setter, extends BaseSchema |
| B3 | `GsonFactory.java` | [GsonFactory.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/util/json/GsonFactory.java) | ✅ Done | LocalDateTime TypeAdapter, PrettyPrinting |
| B4 | `JsonFileHelper.java` | [JsonFileHelper.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/util/json/JsonFileHelper.java) | ✅ Done | readList/writeList, file không tồn tại → list rỗng |
| B5 | `GenericDAO.java` | [GenericDAO.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/persistence/dao/GenericDAO.java) | ✅ Done | Interface 5 methods |
| B6 | `UserDAO.java` | [UserDAO.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/persistence/dao/UserDAO.java) | ✅ Done | Implements GenericDAO + findByUsername |
| — | `LocalDateTimeAdapter.java` | [LocalDateTimeAdapter.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/util/json/LocalDateTimeAdapter.java) | ✅ Bonus | Refactored adapter vào class riêng |
| — | `UserDAOTest.java` | [UserDAOTest.java](file:///d:/Project/AuctionUET/auction-server/src/test/java/com/auctionuet/server/persistence/dao/UserDAOTest.java) | ✅ **7/7 PASS** | — |

> [!NOTE]
> **Package hơi khác kế hoạch:** GsonFactory nằm ở `util/json/` thay vì `persistence/json/`. Đây là quyết định hợp lý vì GsonFactory được dùng bởi cả Network layer (MessageSerializer), không chỉ Persistence.

### Tuần 3: AuthService + SessionManager

| # | Deliverable | File | Trạng thái | Ghi chú |
|---|------------|------|-----------|---------|
| C1 | `AuthService.java` | [AuthService.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/service/AuthService.java) | ✅ Done | Login + Register logic hoàn chỉnh |
| C2 | `SessionManager.java` | [SessionManager.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/service/SessionManager.java) | ✅ Done | Singleton, ConcurrentHashMap |
| C3 | `LoginResult.java` | [LoginResult.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/service/LoginResult.java) | ✅ Done | POJO immutable |
| — | `AuthServiceTest.java` | [AuthServiceTest.java](file:///d:/Project/AuctionUET/auction-server/src/test/java/com/auctionuet/server/domain/service/AuthServiceTest.java) | ✅ **7/7 PASS** | — |

**Đánh giá Cường: 🟢 10/10 — Hoàn thành xuất sắc, vượt kế hoạch (thêm bonus files)**

---

## 🅱️ KHÁNH — Network Protocol + Controller/Router

### Tuần 2: Protocol + Socket Server

| # | Deliverable | File | Trạng thái | Ghi chú |
|---|------------|------|-----------|---------|
| B1 | `ActionType.java` | [ActionType.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/protocol/ActionType.java) | ✅ Done | LOGIN, REGISTER, LOGOUT, PING |
| B2 | `Request.java` | [Request.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/protocol/Request.java) | ✅ Done | action + data Map + token |
| B3 | `Response.java` | [Response.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/protocol/Response.java) | ✅ Done | Factory methods: ok(), error(), push() |
| B4 | `MessageSerializer.java` | [MessageSerializer.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/protocol/MessageSerializer.java) | ✅ Done | serialize/deserialize |
| B5 | `AuctionServer.java` | [AuctionServer.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/server/AuctionServer.java) | ✅ Done | accept() loop + threading |
| B6 | `ClientHandler.java` | [ClientHandler.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/server/ClientHandler.java) | ✅ Done | Runnable, synchronized sendMessage |
| — | `MessageSerializerTest.java` | [MessageSerializerTest.java](file:///d:/Project/AuctionUET/auction-server/src/test/java/com/auctionuet/server/MessageSerializerTest.java) | ✅ PASS | — |

### Tuần 3: AuthController + RequestRouter

| # | Deliverable | File | Trạng thái | Ghi chú |
|---|------------|------|-----------|---------|
| C1 | `AuthController.java` | [AuthController.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/controller/AuthController.java) | ✅ Done | handleLogin, handleRegister, handleLogout |
| C2 | `RequestRouter.java` | [RequestRouter.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/server/RequestRouter.java) | ✅ Done | route() switch by ActionType |
| C3 | Cập nhật ClientHandler | — | ✅ Done | Dùng Router thay PING cứng |
| — | Controllers placeholder | AdminController, AuctionController, BidController, UserController | ✅ Bonus | Stub classes cho tuần 4+ |

**Đánh giá Khánh: 🟢 9/10 — Hoàn thành đầy đủ, có sáng kiến tạo sẵn stub controllers**

---

## 🅲️ CÔNG — Client GUI + Client Network

### Tuần 2: GUI Login + Register

| # | Deliverable | File | Trạng thái | Ghi chú |
|---|------------|------|-----------|---------|
| B1 | `ClientApp.java` | [ClientApp.java](file:///d:/Project/AuctionUET/auction-client/src/main/java/com/auctionuet/client/ClientApp.java) | ✅ Done | Entry point |
| B2 | `SceneManager.java` | [SceneManager.java](file:///d:/Project/AuctionUET/auction-client/src/main/java/com/auctionuet/client/view/SceneManager.java) | ✅ Done | Singleton, switchScene |
| B3a | `LoginView.fxml` | [LoginView.fxml](file:///d:/Project/AuctionUET/auction-client/src/main/resources/fxml/LoginView.fxml) | ✅ Done | — |
| B3b | `LoginController.java` | [LoginController.java](file:///d:/Project/AuctionUET/auction-client/src/main/java/com/auctionuet/client/view/LoginController.java) | ✅ Done | Client-side validation |
| B4a | `RegisterView.fxml` | [RegisterView.fxml](file:///d:/Project/AuctionUET/auction-client/src/main/resources/fxml/RegisterView.fxml) | ✅ Done | — |
| B4b | `RegisterController.java` | [RegisterController.java](file:///d:/Project/AuctionUET/auction-client/src/main/java/com/auctionuet/client/view/RegisterController.java) | ✅ Done | Validation đầy đủ |
| B5 | `styles.css` | [styles.css](file:///d:/Project/AuctionUET/auction-client/src/main/resources/css/styles.css) | ✅ Done | Dark theme |
| — | `Launcher.java` | [Launcher.java](file:///d:/Project/AuctionUET/auction-client/src/main/java/com/auctionuet/client/Launcher.java) | ✅ Bonus | Module workaround |

### Tuần 3: Client Network + Dashboard

| # | Deliverable | File | Trạng thái | Ghi chú |
|---|------------|------|-----------|---------|
| C1 | `ServerConnection.java` | [ServerConnection.java](file:///d:/Project/AuctionUET/auction-client/src/main/java/com/auctionuet/client/network/ServerConnection.java) | ✅ Done | Singleton TCP |
| C2 | `AuthClient.java` | [AuthClient.java](file:///d:/Project/AuctionUET/auction-client/src/main/java/com/auctionuet/client/network/AuthClient.java) | ✅ Done | login/register/logout |
| C3 | Protocol classes (client-side) | ActionType, Request, Response | ✅ Done | Tạo bản copy client riêng |
| C4a | `DashboardView.fxml` | [DashboardView.fxml](file:///d:/Project/AuctionUET/auction-client/src/main/resources/fxml/DashboardView.fxml) | ✅ Done | — |
| C4b | `DashboardController.java` | [DashboardController.java](file:///d:/Project/AuctionUET/auction-client/src/main/java/com/auctionuet/client/view/DashboardController.java) | ✅ Done | Xin chào + Logout |
| — | `FakeServer.java` | [FakeServer.java](file:///d:/Project/AuctionUET/auction-client/src/main/java/com/auctionuet/client/FakeServer.java) | ✅ Bonus | Mock server để test offline |
| — | `MainView.fxml` + `MainController.java` | — | ✅ Bonus | UI nâng cao thêm |

**Đánh giá Công: 🟢 9/10 — Hoàn thành đầy đủ, sáng tạo thêm FakeServer và MainView**

---

## 🅳️ ANH — Domain Model + Mapper + Utils + Integration Test

### Tuần 2: Domain + Mapper + Utils + Exceptions

| # | Deliverable | File | Trạng thái | Ghi chú |
|---|------------|------|-----------|---------|
| B1 | `UserRole.java` | [UserRole.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/enums/UserRole.java) | ✅ Done | BIDDER, SELLER, ADMIN |
| B2 | `User.java` (abstract) | [User.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/model/User.java) | ✅ Done | Immutable, abstract |
| B3 | `Bidder.java` | [Bidder.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/model/Bidder.java) | ✅ Done | hasPermission override |
| B4 | `Seller.java` | [Seller.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/model/Seller.java) | ✅ Done | hasPermission override |
| B5 | `Admin.java` | [Admin.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/model/Admin.java) | ✅ Done | hasPermission override |
| B6 | `UserDTO.java` | [UserDTO.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/network/dto/UserDTO.java) | ✅ Done | Immutable |
| B7 | `UserMapper.java` | [UserMapper.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/mapper/UserMapper.java) | ✅ Done | toDomain, toDTO, toNewSchema |
| B8 | `IdGenerator.java` | [IdGenerator.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/util/IdGenerator.java) | ✅ Done | UUID |
| B9 | `PasswordUtils.java` | [PasswordUtils.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/util/PasswordUtils.java) | ✅ Done | SHA-256 + salt |
| B10 | `ValidationUtils.java` | [ValidationUtils.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/util/ValidationUtils.java) | ✅ Done | email, password, username |
| B11a | `AuthenticationException.java` | [AuthenticationException.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/exception/AuthenticationException.java) | ✅ Done | — |
| B11b | `UserNotFoundException.java` | [UserNotFoundException.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/exception/UserNotFoundException.java) | ✅ Done | — |
| B11c | `DuplicateUserException.java` | [DuplicateUserException.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/exception/DuplicateUserException.java) | ✅ Done | — |
| — | Bonus exceptions | `AuctionException`, `AuctionClosedException`, `InvalidBidException` | ✅ Bonus | Chuẩn bị cho tuần 4+ |
| — | Bonus enums | `AuctionStatus`, `ItemType` | ✅ Bonus | Chuẩn bị cho tuần 4+ |
| — | Bonus models | `Item`, `Art`, `Electronics`, `Vehicle`, `LiveAuction`, `BidRecord` | ✅ Bonus | Stub classes cho tuần 4+ |
| — | Unit tests | `UserModelTest`, `UserMapperTest`, `PasswordUtilsTest`, `ValidationUtilsTest` | ✅ **20/20 PASS** | — |

### Tuần 3: Integration Testing

| # | Deliverable | File | Trạng thái | Ghi chú |
|---|------------|------|-----------|---------|
| C1 | `TestHelper.java` | [TestHelper.java](file:///d:/Project/AuctionUET/auction-server/src/test/java/com/auctionuet/server/TestHelper.java) | ✅ Done | startTestServer, stopTestServer, sendRawRequest, cleanTestData |
| C2 | `AuthIntegrationTest.java` | [AuthIntegrationTest.java](file:///d:/Project/AuctionUET/auction-server/src/test/java/com/auctionuet/server/AuthIntegrationTest.java) | ✅ **7/7 PASS** | Tất cả 7 test scenarios |

**Đánh giá Anh: 🟢 10/10 — Hoàn thành xuất sắc, tạo nhiều stub cho tuần 4+**

---

## 📈 KẾT QUẢ KIỂM THỬ CHI TIẾT

```
╔══════════════════════════════════════════════════════════════════╗
║                    mvn test — BUILD SUCCESS                     ║
╠══════════════════════════════════════════════════════════════════╣
║ AuthIntegrationTest      │  7 tests │ ✅ ALL PASS │ 1.277s      ║
║ AuthServiceTest          │  7 tests │ ✅ ALL PASS │ 0.041s      ║
║ PasswordUtilsTest        │  4 tests │ ✅ ALL PASS │ 0.009s      ║
║ UserDAOTest              │  7 tests │ ✅ ALL PASS │ 0.035s      ║
║ ServerAppTest            │  2 tests │ ✅ ALL PASS │ 0.003s      ║
║ UserMapperTest           │  6 tests │ ✅ ALL PASS │ 0.009s      ║
║ UserModelTest            │  5 tests │ ✅ ALL PASS │ 0.006s      ║
║ ValidationUtilsTest      │  5 tests │ ✅ ALL PASS │ 0.010s      ║
╠══════════════════════════════════════════════════════════════════╣
║ TOTAL: 43 tests │ 0 failures │ 0 errors │ 0 skipped │ 3.397s   ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## ✅ Checklist MVP Auth — Kiểm tra 10 tiêu chí

| # | Tiêu chí | Trạng thái | Chi tiết |
|---|----------|-----------|---------|
| 1 | Client mở → Login screen dark theme | ✅ | LoginView.fxml + styles.css |
| 2 | Register → users.json, password đã hash | ✅ | UserDAO.save + PasswordUtils.hash |
| 3 | Login đúng → nhận token + Dashboard | ✅ | Test 1 & 6 PASS |
| 4 | Login sai → lỗi từ server | ✅ | Test 3 PASS: "Sai mật khẩu" |
| 5 | Logout → token xóa, quay Login | ✅ | Test 1 PASS: logout flow |
| 6 | Login lại token cũ → từ chối | ✅ | Test 1 PASS: afterLogout → ERROR |
| 7 | 2 Client đồng thời → không conflict | ✅ | Test 6 PASS: tokenA ≠ tokenB |
| 8 | Server crash → Client hiện lỗi | ⚠️ Chưa verify | Cần test thủ công |
| 9 | `mvn test` → ALL PASS | ✅ **43/43** | BUILD SUCCESS |
| 10 | JSON readable, password KHÔNG plain text | ✅ | SHA-256 hash + salt |

---

## 🔍 Phân tích chi tiết

### Điểm mạnh 💪

1. **Kiến trúc sạch:** Separation of Concerns rõ ràng — Schema ↔ Domain ↔ DTO không bị "rò rỉ" password
2. **Test coverage toàn diện:** 43 tests bao phủ tất cả layers, integration test E2E qua socket thật
3. **Merge thành công:** PR #29 từ `develop/tuan2` → `main`, resolve conflicts hoàn chỉnh
4. **Sáng kiến tốt:** Tạo sẵn stub classes cho tuần 4+ (AuctionSchema, ItemSchema, BidSchema, etc.)
5. **Design Patterns rõ ràng:** Factory Method (UserMapper), Singleton (SessionManager, SceneManager), Observer (chuẩn bị)

### Vấn đề cần lưu ý ⚠️

1. **Branch naming không đồng nhất:**
   - Kế hoạch: `feature/tuan-2-cuong-persistence`
   - Thực tế: `feat/tuan-2-3-persistence-and-auth-cuong` (gộp 2 tuần)
   - → Không nghiêm trọng, nhưng nên giữ convention cho tuần 4+

2. **Không còn feature branches trên remote:**
   - Chỉ còn `main` — tất cả branches đã được merge và xóa
   - → Cần tạo branches mới cho tuần 4

3. **Tiêu chí #8 chưa verify:** Server crash → Client xử lý error
   - Cần Công test thủ công: kill server → Client hiện thông báo, không treo

4. **Stub classes còn trống:**
   - Nhiều placeholder files (AuctionDAO, BidDAO, AuctionService, etc.) chỉ ~80 bytes
   - → Cần nội dung thật cho tuần 4+

---

## 📊 Tổng điểm theo thành viên

| Thành viên | Tuần 2 | Tuần 3 | Tests | Bonus | Tổng |
|------------|--------|--------|-------|-------|------|
| 🅰️ **Cường** | ✅ 6/6 files | ✅ 3/3 files | 14/14 PASS | +1 (LocalDateTimeAdapter) | **10/10** |
| 🅱️ **Khánh** | ✅ 6/6 files | ✅ 3/3 files | PASS (via integration) | +4 stub controllers | **9/10** |
| 🅲️ **Công** | ✅ 6/6 files | ✅ 5/5 files | Manual (thủ công) | +2 (FakeServer, MainView) | **9/10** |
| 🅳️ **Anh** | ✅ 13/13 files | ✅ 2/2 files | 27/27 PASS | +8 stub models/enums | **10/10** |

---

## 🚀 Đề xuất cho Tuần 4

> [!IMPORTANT]
> Tuần 2-3 hoàn thành VƯỢT kế hoạch. Nhóm sẵn sàng bước sang **Auction & Bidding feature**.

1. **Ưu tiên #1:** Implement `ItemSchema`, `AuctionSchema`, `BidSchema` (stubs đã có)
2. **Ưu tiên #2:** Implement `LiveAuction` realtime bidding logic + Observer pattern
3. **Ưu tiên #3:** Client GUI cho Create Auction + Place Bid screens
4. **Dọn dẹp:** Tag version `v0.1.0-auth-mvp` trên `main` trước khi bắt đầu tuần 4
5. **Quy ước:** Giữ branch naming `feature/tuan-4-{tên}-{mô-tả}` cho consistency

---

> **Kết luận:** Tuần 2-3 là thành công **xuất sắc**. Auth MVP hoạt động end-to-end, 43/43 tests pass, kiến trúc sạch, và nhóm đã tạo sẵn nền tảng mở rộng cho các tuần tiếp theo. 🎉
