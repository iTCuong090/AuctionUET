# AuctionUET — Tài liệu ôn tập vấn đáp Bài tập lớn

> Tài liệu này tổng hợp **mọi thứ cần biết để bảo vệ bài tập lớn** môn Lập trình Nâng cao (UET). Mục tiêu: bất kỳ thành viên nào trong nhóm, khi mở file này ra trước khi vấn đáp, đều có thể trả lời được các câu hỏi từ "vì sao chọn pattern này" tới "nếu admin xóa auction lúc đang chạy thì sao".
>
> **Quy ước đọc:** mỗi phần đều có cấu trúc *Cái gì → Vì sao → Nếu không thì sao → Tradeoff*. Học theo bộ 4 này, không học vẹt.

---

## Mục lục

1. [Tổng quan dự án trong 60 giây](#1-tổng-quan-dự-án-trong-60-giây)
2. [Kiến trúc tổng thể (4 tầng)](#2-kiến-trúc-tổng-thể-4-tầng)
3. [Design Patterns — đã dùng những gì, vì sao, tradeoff](#3-design-patterns--đã-dùng-những-gì-vì-sao-tradeoff)
4. [Ánh xạ kiến thức môn Lập trình Nâng cao (UET)](#4-ánh-xạ-kiến-thức-môn-lập-trình-nâng-cao-uet)
5. [Các quyết định thiết kế có chủ đích (tradeoff)](#5-các-quyết-định-thiết-kế-có-chủ-đích-tradeoff)
6. [Nguyên lý hoạt động của các tính năng chính](#6-nguyên-lý-hoạt-động-của-các-tính-năng-chính)
7. [Các tình huống "Nếu... thì sao?" (What-If)](#7-các-tình-huống-nếu-thì-sao-what-if)
8. [Tự nhận xét điểm mạnh — điểm yếu](#8-tự-nhận-xét-điểm-mạnh--điểm-yếu)
9. [Bộ câu hỏi vấn đáp dự đoán + đáp án ngắn](#9-bộ-câu-hỏi-vấn-đáp-dự-đoán--đáp-án-ngắn)
10. [Cheat sheet — số liệu cần thuộc lòng](#10-cheat-sheet--số-liệu-cần-thuộc-lòng)

---

## 1. Tổng quan dự án trong 60 giây

**AuctionUET** là hệ thống đấu giá trực tuyến (online auction) viết bằng Java 21, theo kiến trúc **Client–Server qua TCP Socket**, dữ liệu trao đổi bằng **JSON (Gson)**.

- **Server** (`auction-server`): chạy `AuctionServer` lắng nghe cổng 8888, xử lý đa luồng — mỗi client một `ClientHandler` thread.
- **Client** (`auction-client`): JavaFX 21 + FXML, theo mô hình **MVC**.
- **Protocol chung** (`auction-protocol`): module Maven riêng chứa `Request`, `Response`, `PushMessage`, các DTO, và `MessageSerializer` — chia sẻ giữa client và server để tránh lệch contract.
- **Persistence**: JSON file (`data/users.json`, `data/items.json`, `data/auctions.json`, `data/bids.json`, `data/transactions.json`) — đọc/ghi qua tầng DAO.

**3 vai trò**: `Bidder` (đấu giá), `Seller` (đăng sản phẩm + phiên), `Admin`.

**Vòng đời 1 phiên đấu giá** (state machine):
```
OPEN → RUNNING → FINISHED → WAITING_PAYMENT → PAID
                          ↘ CANCELED (không ai bid / quá hạn thanh toán / lỗi)
```

**Điểm nâng cao đã làm**:
- Auto-Bidding (proxy bidding kiểu eBay, dùng cơ chế Trạng thái Chờ Pending State bảo vệ người dẫn đầu).
- Anti-Sniping (gia hạn phiên nếu bid trong X giây cuối).
- Realtime push qua TCP (Observer pattern + thread-safe `CopyOnWriteArrayList`).
- Wallet + Frozen Balance + Deposit 10% (escrow).
- Forfeit deposit nếu winner không thanh toán trong 5 phút (`PAYMENT_DEADLINE_MINUTES`).
- Reconcile định kỳ 30 giây để self-heal sau crash/restart.

---

## 2. Kiến trúc tổng thể (4 tầng)

```
┌─────────────────────────────────────────────────────────────┐
│                       CLIENT (JavaFX)                       │
│  View (FXML) ─► Controller (.java) ─► xxxClient (network)   │
│       ▲                                       │             │
│       │ Platform.runLater (UI thread)         ▼             │
│       │                                ServerConnection     │
│       │                                (Singleton + Socket) │
└───────┼─────────────────────────────────────┼───────────────┘
        │                                     │ TCP, JSON
        │     PushMessage (server→client)     │ Request/Response
        │ ◄───────────────────────────────────┤
┌───────┴─────────────────────────────────────▼───────────────┐
│                          SERVER                             │
│  ┌─────────────── Network Layer ──────────────────────────┐ │
│  │ AuctionServer (ServerSocket) ─► ClientHandler thread/n │ │
│  │           │                                            │ │
│  │           ▼                                            │ │
│  │     RequestRouter ─switch─► xxxController              │ │
│  │           │                       │                    │ │
│  │           └─► GlobalExceptionHandler                   │ │
│  └───────────────────────┬─────────────────────────────────┘ │
│                          ▼                                  │
│  ┌──────────────── Domain (Service + Manager) ─────────────┐│
│  │ AuthService, AuctionService, BidService, ItemService,   ││
│  │ UserService, WalletService, TransactionService          ││
│  │ + SessionManager, AuctionManager, DataManager (Singletons)││
│  │ + LiveAuction (in-RAM model + ReentrantLock + Observer) ││
│  └───────────────────────┬─────────────────────────────────┘│
│                          ▼                                  │
│  ┌────────────── Persistence Layer ────────────────────────┐│
│  │ GenericDAO<T> ◄── UserDAO, ItemDAO, AuctionDAO, BidDAO  ││
│  │              ◄── TransactionDAO                         ││
│  │ Schema (BaseSchema → UserSchema/ItemSchema/...)         ││
│  │ JsonFileHelper + GsonFactory                            ││
│  └─────────────────────────────────────────────────────────┘│
│                                                             │
│  data/*.json (file-based "DB")                              │
└─────────────────────────────────────────────────────────────┘
```

**Quy tắc tách tầng** (rất quan trọng khi GV hỏi):

| Tầng | Trách nhiệm | KHÔNG được làm |
|------|-------------|----------------|
| **View (FXML + Controller)** | Hiển thị, validate cơ bản, bắt sự kiện UI | Không gọi DAO trực tiếp, không chứa business logic |
| **Network (Controller, Router, Handler)** | Parse Request, validate token, chuyển sang Service | Không truy cập file JSON, không tính business rule |
| **Domain (Service, Manager, Model)** | Toàn bộ business logic, xử lý concurrency, state machine | Không biết về Socket, không biết về JSON format |
| **Persistence (DAO, Schema)** | CRUD file JSON, giữ data integrity | Không gọi Service, không biết về User session |

**Vì sao tách như vậy?**
- Test được từng tầng độc lập (mock DAO khi test Service, mock Service khi test Controller).
- Đổi từ JSON sang SQL chỉ cần viết `MySqlUserDAO implements GenericDAO<UserSchema>`, các tầng khác không biết.
- Đổi từ TCP sang REST chỉ thay tầng Network, Service nguyên vẹn.

---

## 3. Design Patterns — đã dùng những gì, vì sao, tradeoff

### 3.1 Singleton

**Dùng ở**: `SessionManager`, `AuctionManager`, `DataManager`, `ServerConnection` (client), `SceneManager` (client), `ClientSession` (client).

**Cái gì:** đảm bảo class chỉ có **một instance duy nhất** trong toàn bộ JVM, truy cập qua `getInstance()`.

**Cách hiện thực:**
```java
// Eager Singleton — khởi tạo ngay khi class được load
public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private final Map<String, User> tokenMap = new ConcurrentHashMap<>();
    private SessionManager() {}
    public static SessionManager getInstance() { return INSTANCE; }
}
```

**Vì sao Singleton cho `SessionManager`?**
- `tokenMap` là **shared state toàn server** — tất cả `ClientHandler` thread phải nhìn thấy cùng một bảng token-↔-user. Nếu mỗi handler tự tạo `SessionManager` riêng, login ở thread A sẽ không tồn tại ở thread B.
- Nếu **không** dùng Singleton: phải truyền `SessionManager` qua constructor cho mọi class — vẫn được, nhưng đan dây phức tạp. Hoặc tệ hơn: tạo nhiều instance → token "ma" → user login thành công nhưng request tiếp theo bị "Invalid token".

**Vì sao Singleton cho `AuctionManager`?**
- Bản đồ `liveAuctions: Map<String, LiveAuction>` là **trạng thái runtime trong RAM** của toàn server. Hai instance khác nhau = hai vũ trụ riêng biệt → một thread set status RUNNING, thread khác vẫn thấy OPEN.
- Quan trọng hơn: nó sở hữu **một `ScheduledExecutorService`** chung. Nếu nhiều instance → mỗi instance có pool thread riêng → khó kiểm soát resource, có thể leak.

**Tradeoff của Singleton (GV thường truy ngược):**
- ✅ Giải quyết shared state, tiết kiệm resource (1 thread pool thay vì N).
- ❌ **Khó test** — không inject mock được. Khắc phục: trong `BidService`, ta truyền `AuctionManager` qua constructor (constructor injection) thay vì gọi `AuctionManager.getInstance()` bên trong; test có thể `new BidService(mockManager, ...)`.
- ❌ **Hidden global state** — gây coupling ngầm, vi phạm DI. Trong dự án, ta hạn chế bằng cách chỉ Service mới được biết về Singleton, các tầng còn lại không gọi trực tiếp.
- ❌ Nếu lazy init (`if (instance == null) instance = new ...`) không đồng bộ → race condition tạo 2 instance. Ta dùng **eager init** (`static final INSTANCE = new ...`) để khỏi cần lock — JVM đảm bảo class loader chỉ chạy static block một lần.

> **Câu trả lời mẫu khi GV hỏi "vì sao SessionManager phải là Singleton?"**
> "Vì nó nắm bảng token-user dùng chung cho toàn server. Nhiều `ClientHandler` thread phải nhìn thấy cùng một bảng — nếu mỗi thread có một instance riêng, login ở thread A xong, validateToken ở thread B sẽ fail. Em chọn eager init bằng `static final` để không cần đồng bộ hóa khi truy cập."

---

### 3.2 Observer

**Dùng ở**: `LiveAuction` (Subject) + `AuctionObserver` (interface) + `ClientHandler` (Observer cụ thể), `AuctionManager` (Observer trung gian + relay).

**Cái gì:** Subject nắm danh sách Observer; khi sự kiện xảy ra (bid mới, phiên kết thúc, gia hạn), Subject gọi tuần tự các Observer.

```java
public interface AuctionObserver {
    default void onAuctionStarted(String auctionId, LocalDateTime start, LocalDateTime end) {}
    void onBidPlaced(String auctionId, BidRecord record);
    void onAuctionEnded(String auctionId, String winnerId, double finalPrice);
    void onAuctionExtended(String auctionId, LocalDateTime newEndTime);
}
```

**Vì sao Observer?**
- **Realtime update** — yêu cầu 3.2.4 trong TASK.md cấm dùng polling. Khi bid mới đến server, *mọi client đang xem phiên* phải được cập nhật ngay — Subject (`LiveAuction`) tự đẩy event qua chain Observer → tới `ClientHandler.sendPush()` → client nhận `PushMessage`.
- **Decouple** model với network: `LiveAuction` không biết gì về Socket/JSON. Nó chỉ biết "có ai đó muốn nghe khi bid xảy ra".

**Chain Observer trong dự án:**
```
LiveAuction (Subject)
   └─ AuctionManager (Observer trung gian — relay)
         └─ ClientHandler #1 (Observer cuối — gửi push qua socket)
         └─ ClientHandler #2
         └─ ClientHandler #N
```
Cách relay 2 tầng cho phép `LiveAuction` chỉ giữ duy nhất 1 observer (`AuctionManager`), còn `AuctionManager` mới giữ map `auctionId → Set<ClientHandler>` — khi client SUBSCRIBE/UNSUBSCRIBE, không động vào `LiveAuction`.

**Vì sao dùng `CopyOnWriteArrayList` cho `observers`?**
- Đọc-nhiều-ghi-ít: thường lặp qua observers để notify, hiếm khi add/remove.
- Cho phép một thread iterate (notify) trong khi thread khác add — không `ConcurrentModificationException`.
- Tradeoff: mỗi lần add/remove copy toàn bộ array → đắt nếu danh sách to. Trong auction điển hình < 100 viewer, chấp nhận được.

**Nếu không có Observer?**
- Phải dùng polling: client gửi `GET_AUCTION_DETAIL` mỗi 1-2 giây → tốn băng thông gấp 50 lần, độ trễ cảm nhận 1-2 giây thay vì <100ms.
- Hoặc nhồi logic gọi `ClientHandler` trực tiếp vào `LiveAuction` → vi phạm separation of concerns, không test được unit.

---

### 3.3 Factory Method (biến thể)

**Dùng ở**:
- `AuthService.login()`: switch theo `UserRole` → tạo đúng subclass (`Bidder`/`Seller`/`Admin`).
- `ItemMapper.toNewSchema()`: switch theo `ItemType` → tạo đúng `ElectronicsSchema`/`ArtSchema`/`VehicleSchema`.

**Ví dụ:**
```java
User user = switch (schema.getRole()) {
    case BIDDER -> new Bidder(schema.getId(), schema.getUsername());
    case SELLER -> new Seller(schema.getId(), schema.getUsername());
    case ADMIN  -> new Admin(schema.getId(), schema.getUsername());
};
```

**Vì sao?** Caller không cần biết logic chọn subclass — chỉ nhận về `User`. Khi cần thêm role mới (ví dụ `Moderator`), chỉ sửa factory + thêm class, code gọi `authService.login()` không đổi.

**Tradeoff:**
- ✅ Open/Closed Principle: thêm role không sửa code gọi.
- ❌ Hiện tại `switch` còn nằm rải rác (login, register, mapper). Lý tưởng tập trung vào một `UserFactory` class. Đây là một điểm có thể refactor.

---

### 3.4 Template Method (qua kế thừa)

**Dùng ở**: `BaseSchema` (abstract) → `UserSchema`, `ItemSchema`, `AuctionSchema`, `BidSchema`, `TransactionSchema` kế thừa.

`BaseSchema` cung cấp khung chung: `id`, `createdAt`, `updatedAt`, `equals/hashCode` dựa trên `id`. Schema con chỉ thêm trường riêng.

**Vì sao?** Mọi entity đều cần id + timestamps để DAO `findById`, `update` (set `updatedAt = now`) hoạt động đồng nhất. Không lặp code 5 lần.

---

### 3.5 DAO (Data Access Object)

**Dùng ở**: `GenericDAO<T extends BaseSchema>` interface + 5 implementation cụ thể.

**Vì sao?**
- Tầng Service không biết dữ liệu lưu ở JSON hay MySQL. Nó chỉ gọi `userDAO.findByUsername(...)`.
- Test: tạo `UserDAO("test-data/users.json")` (đường dẫn custom qua constructor) → unit test không đụng dữ liệu production.

```java
public UserDAO() { this.filePath = "data/users.json"; }
public UserDAO(String filePath) { this.filePath = filePath; }  // dùng cho test
```

**Tradeoff:**
- ❌ DAO hiện đọc *toàn bộ* file mỗi lần `findById` (lệch O(N) cho mỗi truy vấn). Comment trong `UserDAO.java` thừa nhận đây là "tạm thời và chấp nhận được". Với dataset BTL (vài chục user), OK. Với prod (1M user), không bao giờ.
- ❌ Không có index, không có transaction. Nếu file ghi nửa chừng → corrupt.

---

### 3.6 DTO (Data Transfer Object)

**Dùng ở**: toàn bộ `auction-protocol/dto/`. `UserDTO`, `ItemDTO`, `AuctionDTO`, `BidDTO`, `WalletResponseDTO`, `TransactionDTO`...

**Vì sao tách Domain ↔ Schema ↔ DTO?** (rất hay bị hỏi)

| Tầng đối tượng | Mục đích | Ví dụ |
|----------------|----------|-------|
| **Schema** | Mô tả 1:1 với file JSON, có `hashedPassword`, `passwordSalt` | `UserSchema` |
| **Domain Model** | Object dùng trong business logic, có behavior (`hasPermission()`) | `Bidder`, `LiveAuction` |
| **DTO** | Object truyền qua mạng, **không có thông tin nhạy cảm** | `UserDTO` (chỉ id, username, role) |

Lý do then chốt: `UserSchema` có `hashedPassword`. Nếu trả thẳng nó qua mạng → leak hash. DTO chỉ giữ field cần thiết.

**Mapper** (`UserMapper`, `ItemMapper`, `AuctionMapper`, `BidMapper`): chuyển đổi 3 chiều.

**Nếu không có DTO?** → Hoặc leak data nhạy cảm, hoặc phải bịt từng field bằng `@Expose(serialize=false)` (vẫn dễ sót).

---

### 3.7 MVC (Model-View-Controller) — phía Client

- **View**: file `.fxml` (UI declarative).
- **Controller**: class `xxxController.java` trong `view/` (xử lý sự kiện).
- **Model**: `ClientSession`, các DTO nhận từ server.

**Vì sao?** Yêu cầu 3.4 trong TASK.md. Tách biệt UI khỏi logic giúp Scene Builder và code Java làm song song được.

---

### 3.8 Strategy (ẩn) — qua `hasPermission(Permission)`

Mỗi subclass `User` override `hasPermission()` với một bộ rule riêng. Đây là biến thể của Strategy: hành vi "kiểm tra quyền" khác nhau theo subclass.

```java
// Bidder
return switch (action) {
    case PLACE_BID, VIEW_AUCTION, VIEW_BID_HISTORY,
         GET_PROFILE, UPDATE_PROFILE, SUBSCRIBE, UNSUBSCRIBE,
         DEPOSIT, WITHDRAW, GET_WALLET, GET_MY_TRANSACTIONS, PAY_AUCTION,
         SET_AUTO_BID, CANCEL_AUTO_BID -> true;
    default -> false;
};
```

---

### 3.9 Bảng tổng hợp Pattern + Vị trí + Tradeoff

| Pattern | Vị trí cụ thể | Giải bài toán | Tradeoff |
|---------|--------------|--------------|---------|
| Singleton | `SessionManager`, `AuctionManager`, `DataManager`, `ServerConnection`, `SceneManager`, `ClientSession` | Shared state toàn process | Khó test, hidden global state |
| Observer | `LiveAuction` ↔ `AuctionObserver` ↔ `AuctionManager` ↔ `ClientHandler` | Realtime push không polling | Notify đồng bộ — observer chậm làm chậm cả chain |
| Factory Method | `AuthService.login()`, `ItemMapper.toNewSchema()` | Chọn subclass theo enum | Switch rải rác, nên tập trung |
| Template Method | `BaseSchema` → subclasses | Khung chung id + timestamps | — |
| DAO | `GenericDAO<T>` + 5 impl | Tách persistence khỏi domain | Đọc cả file mỗi truy vấn |
| DTO | `auction-protocol/dto/` | Truyền dữ liệu an toàn qua mạng | Phải maintain mapper |
| MVC | Client (FXML + Controller + Model) | UI ↔ logic tách biệt | — |
| Strategy (ẩn) | `User.hasPermission()` override | Phân quyền theo role | — |

---

## 4. Ánh xạ kiến thức môn Lập trình Nâng cao (UET)

### 4.1 Cơ bản về OOP & OOP trong Java

| Khái niệm OOP | Áp dụng ở đâu | Chứng minh |
|---------------|--------------|-----------|
| **Class & Object** | Toàn bộ — domain model, controller, service | — |
| **Encapsulation** | `LiveAuction` các field `private`/`private final`; getter không phải setter; trả `new ArrayList<>(bidHistory)` thay vì list nội bộ | `getBidHistory()` trả copy → caller không sửa được state nội bộ |
| **Inheritance** | `User → Bidder/Seller/Admin`; `BaseSchema → UserSchema/...`; `ItemSchema → ElectronicsSchema/ArtSchema/VehicleSchema` | Cây kế thừa 3 cấp |
| **Polymorphism** | `User u = new Bidder(...); u.hasPermission(...)` — gọi theo dynamic type. `AuctionObserver` interface: `LiveAuction` không biết observer cụ thể là `ClientHandler` hay `AuctionManager` | — |
| **Abstraction** | `abstract class User`, `abstract class BaseSchema`, `abstract class ItemSchema`, `interface GenericDAO<T>`, `interface AuctionObserver` | — |
| **Composition over Inheritance** | `LiveAuction` *có* `ReentrantLock` — không kế thừa | — |
| **`final` keyword** | Field bất biến (`id`, `username` trong User; `bidderId`, `amount`, `timestamp` trong BidRecord) | Đảm bảo thread-safe không cần lock cho immutable object |
| **Generics** | `GenericDAO<T extends BaseSchema>`, `JsonFileHelper.readList(String, Class<T>)` | Type-safe, tránh cast |
| **Enum + behavior** | `UserRole`, `AuctionStatus`, `Permission`, `BidType`, `ItemType`, `TransactionType` | Type-safe constants |
| **Sealed interface / pattern matching** | `switch` expression trong `AuthService.login()` (Java 21 feature) | Modern Java |
| **Record** | `BidService.DepositHoldResult` (private record) | Immutable value type |
| **Lambda + functional interface** | `Consumer<String> startAuctionCallback` trong `AuctionManager`; lambda trong `scheduler.schedule(() -> ...)` | — |

### 4.2 Phân tích và tư duy thiết kế hướng đối tượng

| Nguyên lý | Áp dụng ở đâu |
|----------|--------------|
| **SOLID — S** (Single Responsibility) | `AuthService` chỉ xác thực; `WalletService` chỉ tiền; `BidService` chỉ bid. Không gộp |
| **SOLID — O** (Open/Closed) | Thêm role mới = thêm subclass + sửa factory, không sửa chỗ gọi `hasPermission` |
| **SOLID — L** (Liskov) | `Bidder`, `Seller`, `Admin` có thể thay thế `User` ở mọi chỗ |
| **SOLID — I** (Interface Segregation) | `AuctionObserver` có `default` cho `onAuctionStarted` → observer nào không quan tâm thì không phải override |
| **SOLID — D** (Dependency Inversion) | Service phụ thuộc *interface* `GenericDAO`, không phụ thuộc `UserDAO` cụ thể; constructor injection (`new AuthService(userDAO, userService)`) |
| **Separation of Concerns** | 4 tầng (View/Network/Domain/Persistence) |
| **DRY** | `BaseSchema` chung id + timestamps; helper `requireUser`, `requireItem`, `requireAuction` trong các Service |
| **YAGNI / KISS** | Không có cache layer phức tạp, không có ORM — đủ dùng |
| **Tell, don't ask** | `liveAuction.placeBid(user, amount)` thay vì lấy state ra rồi check ngoài |

### 4.3 Multi-threading

| Kỹ thuật | Áp dụng ở đâu | Vì sao |
|----------|--------------|-------|
| **`Thread` mới mỗi client** | `AuctionServer` accept loop: `new Thread(handler, "client-" + ip).start()` | Mỗi client một luồng — đơn giản, đủ tốt cho ~100 client. Trade-off: không có thread pool → DoS nếu quá nhiều kết nối |
| **`ReentrantLock`** | `LiveAuction.bidLock` bảo vệ toàn bộ `placeBid`, `addAutoBid`, `resolveAutoBids` | Chống lost update khi 2 bidder bid đồng thời. Reentrant → cùng thread có thể gọi `placeBid` → `resolveAutoBids` (cũng lock) mà không deadlock |
| **`ConcurrentHashMap`** | `SessionManager.tokenMap`, `AuctionManager.liveAuctions`, `LiveAuction.autoBids` | Read-mostly, viết nhỏ — không cần block reader |
| **`CopyOnWriteArrayList`** | `LiveAuction.observers` | Iterate nhiều (mỗi bid), modify ít (subscribe/unsubscribe) |
| **`ConcurrentHashMap.newKeySet()`** | `depositedBidders` trong `LiveAuction`, observer set trong `AuctionManager` | Thread-safe Set không cần lock |
| **`AtomicLong`** | `AutoBidConfig.REGISTRATION_SEQUENCE` — tạo số thứ tự đăng ký auto-bid duy nhất | Atomic increment, không cần synchronized |
| **`AtomicInteger`** | `AuctionServer.activeConnections` — đếm số kết nối hiện tại | — |
| **`ScheduledExecutorService`** | `AuctionManager.scheduler` (pool size 2) — schedule auto-start, auto-end, payment deadline, periodic reconcile | Không phải tạo `Thread.sleep` blocking |
| **`synchronized` method** | `WalletService.deposit/withdraw/holdAuctionDeposit/...`, `BidService.placeBid/setAutoBid`, `AuctionService.startAuction/endAuction/payAuction/...`, `ClientHandler.sendResponse/sendPush` | Đảm bảo serial — chỉ 1 transaction tiền tệ chạy tại 1 thời điểm. Đơn giản hơn lock từng row |
| **`synchronized(this)` block** | `AuctionManager.startPeriodicReconcile()` (guard `reconcileTask` init) | — |
| **`BlockingQueue<String>`** | Client `ServerConnection.responseQueue` — phân biệt response và push trên 1 socket | Reader thread bỏ message vào queue, caller `sendRequest` poll với timeout 10s |
| **Daemon thread** | Client listener thread `setDaemon(true)` | JVM exit khi UI thread thoát, không bị "treo" |

**Câu hỏi "vì sao `placeBid` cần đồng bộ?"** → Bài kinh điển *lost update*:
1. Thread A đọc `currentHighestBid = 100`.
2. Thread B đọc `currentHighestBid = 100`.
3. A check `120 > 100` ✓ → set `currentHighestBid = 120`.
4. B check `110 > 100` ✓ → set `currentHighestBid = 110` (đè mất 120!).

Bid của A bị mất. Người thắng cuộc sai. Với `ReentrantLock`: B phải chờ A thả lock, đọc lại `currentHighestBid = 120`, thấy `110 > 120` sai → ném `InvalidBidException`.

### 4.4 Testing và Refactoring

| Loại test | Ví dụ trong dự án |
|-----------|------------------|
| **Unit test (logic thuần)** | `PasswordUtilsTest`, `ValidationUtilsTest`, `UserModelTest`, `DomainModelTest`, `MapperTest` |
| **Test với mock DAO** | `AuctionServiceTest`, `AuthServiceTest`, `WalletServiceTest` (truyền `UserDAO` với file test riêng) |
| **Integration test E2E** | `AuthIntegrationTest`, `AuctionIntegrationTest` chạy thật qua socket — `TestHelper` start server thật |
| **JUnit 5** | `@Test`, `@BeforeEach`, `@AfterEach`, `@DisplayName` |

**Refactoring đã làm:** (xem `docs/week5_refactor_report.md`)
- Tách `AuctionService.endAuction` (gần 200 dòng) thành các helper nhỏ.
- Extract `requireUser/requireItem/requireAuction` để loại lặp.
- Đưa ngoại lệ tập trung về `GlobalExceptionHandler`.

### 4.5 Lập trình mạng (Networking)

| Khía cạnh | Hiện thực |
|----------|----------|
| **Protocol** | TCP socket (`ServerSocket` 8888), text-based JSON, 1 message = 1 dòng (đọc bằng `BufferedReader.readLine`) |
| **Connection model** | Persistent (giữ socket suốt phiên login). Không phải connection-per-request như HTTP |
| **Server thread model** | 1 thread/client (không pool). Đủ cho BTL |
| **Bi-directional push** | Server có thể chủ động gửi `PushMessage` (BID_UPDATE, AUCTION_STARTED/ENDED/EXTENDED) qua cùng socket |
| **Serialization** | Gson, custom `LocalDateTimeAdapter`, pretty print disabled cho network (1 dòng) |
| **Request/Response phân biệt với Push** | Server gắn `type` field; client `ServerConnection` đọc dòng → check `type == "PUSH"` → callback push listener; còn lại bỏ vào `responseQueue` |
| **Timeout** | Client `responseQueue.poll(10, SECONDS)` → tránh hang vô hạn |

**Vì sao chọn TCP raw thay vì HTTP/REST?**
- Cần push từ server. REST request-response, muốn push phải dùng WebSocket/SSE.
- BTL yêu cầu networking "tay" → demo socket trực tiếp rõ ràng hơn.
- Tradeoff: không có HTTP middleware (rate-limit, caching, gateway). Tự viết hết.

### 4.6 JavaFX

| Khía cạnh | Hiện thực |
|----------|----------|
| **FXML declarative UI** | 14 view (Login, Register, Dashboard, AuctionList, AuctionDetail, BiddingView, CreateItem, CreateAuction, Profile, Wallet, ...) |
| **Controller injection** | `@FXML private TextField usernameField;` — FXMLLoader bind theo `fx:id` |
| **CSS theming** | `ThemeManager` cho dark/light |
| **Background thread + Platform.runLater** | Login: `new Thread(() -> { call server; Platform.runLater(() -> UI update); }).start()` — không block UI thread |
| **Scene management** | `SceneManager` (Singleton) cho `switchScene(fxmlName)` |
| **Realtime UI update** | `BiddingController` lắng `PushListener`, dùng `Platform.runLater` để update label giá, table bid history |

**Vì sao bắt buộc `Platform.runLater`?** JavaFX có UI thread riêng (JavaFX Application Thread). Mọi thay đổi UI từ thread khác sẽ ném `IllegalStateException: Not on FX application thread`. Background thread gửi request → khi response về, dùng `Platform.runLater(() -> label.setText(...))` để chuyển update về UI thread.

---

## 5. Các quyết định thiết kế có chủ đích (tradeoff)

> Mỗi mục đều có dạng *Cái gì → Vì sao → Hệ quả nếu làm khác*. Đây là phần GV thích đào sâu nhất.

### 5.1 Vì sao `LiveAuction.placeBid()` dùng `ReentrantLock` thay vì `synchronized`?

- **Reentrant**: `placeBid` → `resolveAutoBids` cùng thread, cùng cần lock. `ReentrantLock` (như `synchronized`) cho phép re-entry. Nếu không cùng kiểu reentrant → deadlock.
- **`ReentrantLock` linh hoạt hơn `synchronized`**: hỗ trợ `tryLock(timeout)`, fairness, multiple condition variable. Hiện tại ta chưa khai thác hết, nhưng giữ chỗ cho mở rộng.
- **Tránh lock cả object**: `synchronized(this)` lock object — bất kỳ ai cũng có thể vô tình `synchronized(liveAuction)` từ bên ngoài → deadlock khó debug. `ReentrantLock` là field private → controlled.

### 5.2 Vì sao `WalletService` mọi method là `synchronized`?

- Mọi thao tác tiền tệ (deposit, withdraw, hold, refund, forfeit, payout) phải **atomic**: đọc số dư → tính → ghi, phải không có thread khác chen ngang. Nếu không: 2 lệnh `withdraw` đồng thời → cả hai thấy balance đủ → cả hai trừ → âm.
- `synchronized` method = `synchronized(this)` = lock toàn instance. Vì `WalletService` là singleton (1 instance) → mọi thao tác tiền của mọi user serial.
- **Tradeoff**: hiệu năng — thao tác của user A chặn user B. Với BTL OK. Prod sẽ phải lock theo user (key-based lock) hoặc DB row-lock.

### 5.3 Vì sao `AuctionManager` dùng `ScheduledExecutorService` (pool=2) thay vì `Timer`?

- `Timer` chạy single thread — một task ném exception → kill cả timer.
- `Timer` không scale: chạy lệch giờ nếu task trước trễ.
- `ScheduledExecutorService.scheduleWithFixedDelay` đảm bảo khoảng cách giữa các lần chạy đều, bất chấp task trước dài.
- Pool 2 vì có 2 nhóm task: per-auction (start/end/payment) + periodic reconcile.

### 5.4 Vì sao có `reconcileAuctionsFromDatabase()` chạy mỗi 30s + lúc startup?

**Vấn đề**: nếu server crash giữa khi `LiveAuction` đang chạy:
- File JSON còn lưu status `RUNNING` nhưng RAM mất sạch — `liveAuctions` map rỗng.
- Khi server khởi động lại, `LiveAuction` không được nạp lại → client đặt bid → không tìm thấy phiên đang chạy.

**Giải pháp**: `reconcileAuctionsFromDatabase()` quét toàn bộ `data/auctions.json`:
- Phiên `OPEN` mà thời gian start đã qua → start ngay.
- Phiên `RUNNING` mà chưa quá `endTime` → hydrate lại vào RAM (`auctionManager.loadAuction(schema)`) → reschedule end task.
- Phiên `RUNNING` mà đã quá `endTime` → end ngay, vào `WAITING_PAYMENT` hoặc `CANCELED`.
- Phiên `WAITING_PAYMENT` chưa quá deadline → reschedule expire task; quá hạn → `expirePaymentDeadline` ngay.
- Phiên `PAID`/`CANCELED` → cancel mọi task còn dangling.

Đây là **self-healing**. Chạy lại mỗi 30s vì:
- Phòng trường hợp clock drift, thread bị dừng vì OS.
- Phòng race nếu có instance khác sửa file (không xảy ra trong BTL nhưng giữ tính cẩn thận).

### 5.5 Vì sao deposit 10% phải freeze (không chỉ check balance)?

- Nếu chỉ check `balance >= 10%` lúc bid: bidder bid xong, rút sạch tiền → đến lúc thanh toán không còn → hệ thống bị "hét giá ảo" (shill bidding).
- Freeze (`balance -= 10%; frozenBalance += 10%`): tiền vẫn của user nhưng không rút được. Đủ ràng buộc kinh tế.
- Khi phiên kết thúc:
  - Loser → refund (`frozenBalance → balance`).
  - Winner → giữ frozen. Khi thanh toán: `frozenBalance` áp vào tổng giá, phần còn lại trừ từ `balance`. Tiền của seller `+= totalPrice`. Cả ba transaction lẫn nhau qua `synchronized` đảm bảo không lost update.
  - Winner quá hạn 5 phút → **forfeit**: `frozenBalance -= depositAmount` (mất luôn). Phiên thành `CANCELED`.

### 5.6 Vì sao chia 3 lớp: `Schema` ↔ `Domain Model` ↔ `DTO`?

Đã giải thích ở §3.6. Tóm tắt:
- **Schema** = on-disk representation, tối ưu cho JSON.
- **Domain** = runtime behavior, có method (`placeBid`, `hasPermission`).
- **DTO** = wire representation, chỉ field cần thiết, không lộ password hash.

Nhược: 3 lần map cho cùng 1 entity. Phải maintain `xxxMapper`. **Đó là cái giá của separation.**

### 5.7 Vì sao `BidRecord` immutable (`final` mọi field, không setter)?

- Thread-safe miễn phí — không thread nào sửa được sau khi tạo.
- An toàn khi lưu vào nhiều list (`bidHistory`, observer event).
- Không có "lost update" với immutable.

### 5.8 Vì sao `auction-protocol` là module Maven riêng?

- **Shared contract**: Client gửi `Request`, Server parse cùng class `Request`. Nếu mỗi bên định nghĩa riêng → field lệch là vỡ.
- Module riêng → cả `auction-client` và `auction-server` cùng `dependency` nó → đảm bảo cùng version.
- Refactor 1 lần: thêm action mới → thêm enum trong `ActionType`, cả 2 bên thấy ngay.

### 5.9 Vì sao `AuthService` không tự new `UserDAO()` mà nhận qua constructor?

```java
public AuthService(UserDAO userDAO, UserService userService) { ... }
```
- **Dependency Injection**: test có thể inject mock DAO hoặc DAO trỏ file `test-data/users.json`.
- Đoạn comment trong `AuthService.java` ghi rõ: "Vì các đối tượng userDao có thể custom đường dẫn file database phục vụ test."

### 5.10 Vì sao `AutoBidConfig` có `registrationOrder` (AtomicLong) ngoài `registeredAt`?

- `LocalDateTime.now()` có thể trả cùng nano nếu 2 thread đăng ký gần nhau → comparator hòa → priority queue không deterministic.
- `AtomicLong.incrementAndGet()` đảm bảo thứ tự nghiêm ngặt giữa các đăng ký.

### 5.11 Vì sao Auto-Bid dùng Trạng thái Chờ (Pending State) thay vì chạy vòng lặp đấu giá vô hạn?

- Tránh xung đột luồng và hiện tượng nghẽn mạng (bidding wars) khi hai robot liên tục tự động trả giá đè nhau.
- Chỉ cho phép người dẫn đầu kích hoạt Auto-Bid, giúp hệ thống luôn hoạt động theo luồng tuần tự, bất đồng bộ và kiểm soát mức giá tối đa một cách an toàn và tối ưu.

### 5.12 Vì sao có `endAuction` vẫn cần `removeLiveAuction` và `cancelAllTasks`?

- Auction kết thúc xong, không nên giữ trong `liveAuctions` map → tránh memory leak.
- Hủy mọi ScheduledFuture còn pending → tránh task chạy lại trên phiên đã đóng (race).

### 5.13 Vì sao response và push đi chung 1 socket?

- Tiết kiệm tài nguyên — không phải mở 2 socket per client.
- Client phân biệt bằng field `type` (`"PUSH"` vs `"RESPONSE"`) → reader thread chia dòng vào 2 hướng (`pushListener` và `responseQueue`).
- Tradeoff: nếu response queue tắc (caller chưa poll) → reader thread vẫn chạy → push không bị chặn (vì push không enqueue, gọi callback ngay).

### 5.14 Vì sao client `ServerConnection.sendRequest` là `synchronized`?

- Nhiều UI controller có thể gửi request song song (`AuctionListController` đang refresh, user click "Bid"). Nếu không sync → 2 lệnh `out.println` xen kẽ ký tự → JSON hỏng.
- `BlockingQueue` đảm bảo response đúng người, nhưng phải đảm bảo *gửi không lẫn*.

### 5.15 Vì sao `getBidHistory()` trả `new ArrayList<>(bidHistory)` mà không phải `bidHistory` trực tiếp?

- Encapsulation: caller không sửa được state nội bộ.
- Iterate ngoài `bidLock` an toàn — caller có snapshot riêng.

---

## 6. Nguyên lý hoạt động của các tính năng chính

### 6.1 Đăng nhập (Login)

1. Client `LoginController.handleLogin()`:
   - Validate input (không rỗng).
   - Spawn background thread → `AuthClient.login(username, password)`.
2. `ServerConnection.sendRequest(Request(LOGIN, {username, password}))` → JSON qua socket.
3. Server `ClientHandler.processLine` → `RequestRouter.route` → `AuthController.handleLogin`.
4. `AuthService.login`:
   - `userDAO.findByUsername(username)` — không có → `UserNotFoundException`.
   - `PasswordUtils.verify(password, salt, hashedPassword)` (SHA-256 + salt) — sai → `AuthenticationException`.
   - Switch role → tạo `Bidder`/`Seller`/`Admin` (Factory).
   - `SessionManager.createSession(user)` → trả `UUID` token.
   - Trả `LoginResponseDTO(token, UserDTO)`.
5. Exception bị `RequestRouter` catch → `GlobalExceptionHandler.handle()` → `Response.error(...)`.
6. Client nhận response → `Platform.runLater` → `ClientSession.login(token, userDTO)` + `SceneManager.switchScene("DashboardView")`.

### 6.2 Tạo + bắt đầu phiên đấu giá

1. Seller `CreateAuctionController` → `AuctionClient.createAuction(token, itemId, startTime, endTime, title, desc, antiSnipingWindow, antiSnipingExtension)`.
2. Server `AuctionService.createAuction`:
   - Check `Permission.CREATE_AUCTION` (chỉ Seller có).
   - `requireItem(itemId)` + `requireItemOwner(seller, item)` — không phải chủ item → reject.
   - Validate `endTime > startTime`, `startTime > now`.
   - `auctionDAO.save(new AuctionSchema(... status=OPEN ...))`.
   - `auctionManager.scheduleAuctionStart(id, startTime)` → khi tới giờ, callback `onAuctionStartDue` chạy.
   - Tăng `item.auctionCount` (để biết item đã đem đấu giá bao lần).
3. Khi tới `startTime`, callback gọi `AuctionService.onAuctionStartDue`:
   - Re-check còn `OPEN` không, thời gian còn hợp lệ không.
   - `startAuctionNow`: set status `RUNNING`, `auctionManager.loadAuction(schema, item.startingPrice)` → tạo `LiveAuction` trong RAM, schedule end task.
   - `notifyAuctionStarted` qua observer chain → mọi client subscribe nhận push `AUCTION_STARTED`.

### 6.3 Đặt bid (Manual Bid)

1. Client `BiddingController` → `BidClient.placeBid(token, auctionId, amount)`.
2. Server `BidController.handlePlaceBid` → validate `Permission.PLACE_BID`.
3. `BidService.placeBid` (`synchronized`):
   - `requireLiveAuction(auctionId)` — không có (đã end) → `IllegalArgumentException` → `GlobalExceptionHandler` trả "Dữ liệu không hợp lệ: ...".
   - `requireItemSchema` để tính deposit 10% startingPrice.
   - `ensureDepositFrozen`: nếu bidder chưa cọc → `walletService.holdAuctionDeposit(bidderId, 10%, auctionId, ...)`. Update `liveAuction.markDeposited(bidderId)` + persist `AuctionDepositRefSchema` (lưu transactionId để rollback sau).
   - Gọi `liveAuction.placeBid(bidder, amount, autoRecord -> saveAutoBidRecord(...))`:
     - **Acquire `bidLock`**.
     - Check status `RUNNING`.
     - Check `amount > currentPrice` (nếu không → `InvalidBidException` → **rollback deposit nếu vừa hold**).
     - Check không tự bid (seller).
     - Update `currentHighestBid`, `currentWinnerId`, add `BidRecord` vào `bidHistory`.
     - **Anti-sniping check** (`extendIfSniping`): nếu `endTime - now ≤ antiSnipingWindow` → `endTime += antiSnipingExtension` + notify observer `onAuctionExtended` (server reschedule auto-end qua `AuctionManager.extendAuction`).
     - **Notify observers**: `onBidPlaced` → push BID_UPDATE tới mọi client subscribe.
     - **Resolve auto-bids**: xem 6.5.
     - Release lock.
   - `syncAuctionState`: cập nhật `AuctionSchema` (highestBid, winnerId, endTime) vào file.
   - Lưu `BidSchema` qua `bidDAO`.

### 6.4 Anti-Sniping (gia hạn cuối phiên)

```java
public boolean extendIfSniping() {
    LocalDateTime now = LocalDateTime.now();
    long secondsLeft = Duration.between(now, endTime).getSeconds();
    if (secondsLeft <= antiSnipingWindowSeconds && secondsLeft > 0) {
        this.endTime = this.endTime.plusSeconds(antiSnipingExtensionSeconds);
        for (AuctionObserver obs : observers) obs.onAuctionExtended(id, endTime);
        return true;
    }
    return false;
}
```

- Mỗi auction lưu cấu hình `antiSnipingWindowSeconds` (X) và `antiSnipingExtensionSeconds` (Y) trong schema.
- Khi `placeBid` thành công, `extendIfSniping()` chạy bên trong `bidLock` → atomic với bid.
- Khi gia hạn → `AuctionManager.extendAuction` → cancel end task cũ, schedule end task mới với `newEndTime` → end vẫn xảy ra đúng lúc.

**Vấn đề tinh tế**: ai cập nhật `endTime` trong DB? → `syncAuctionState` sau bid set `auctionSchema.setEndTime(liveAuction.getEndTime())`. Reconcile cũng đọc đúng từ schema khi server restart.

### 6.5 Auto-Bidding (Proxy Bidding kiểu eBay)

**Mô hình**: Chỉ người đang dẫn đầu (high bidder) mới được phép thiết lập hoặc kích hoạt Auto-Bid.

**Cấu trúc dữ liệu**:
- `LiveAuction.autoBids: Map<bidderId, AutoBidConfig>` — lưu cấu hình Auto-Bid của người dùng.
- `LiveAuction.pendingAutoBid: PendingAutoBid` — cấu trúc lưu giữ trạng thái phản hồi chờ xử lý (Pending State) khi người dẫn đầu cũ bị vượt mặt.

**Thuật toán phản ứng và khớp Auto-Bid**:
1. Khi một bidder B mới đặt một lượt bid thủ công và vượt lên dẫn đầu:
   - Hệ thống xác định xem người dẫn đầu cũ A có đang bật Auto-Bid hay không.
   - Nếu có, hệ thống tạo một đối tượng trạng thái phản ứng chờ `PendingAutoBid` đại diện cho A.
2. Một tác vụ chạy bất đồng bộ ngoài luồng chính của client B (để tránh nghẽn giao diện của B) sẽ gửi yêu cầu xử lý cú phản hồi tự động này thông qua `resolvePendingAutoBid(sequenceId, ...)` dưới sự bảo vệ của `bidLock`.
3. Trong `resolvePendingAutoBid`:
   - Hệ thống kiểm tra tính hợp lệ và tính toán giá đấu tự động tiếp theo cho A: `nextAutoBidAmount = currentHighestBid + A.increment`.
   - Nếu giá đấu tiếp theo này vượt quá giới hạn tối đa `maxBid` đã cài đặt của A, hệ thống sẽ tự động hủy kích hoạt cấu hình Auto-Bid của A.
   - Ngược lại, hệ thống sẽ tự động ghi nhận cú bid `AUTO` của A, nâng `currentHighestBid` lên mức mới và đưa A trở lại vị trí dẫn đầu.
4. Quá trình này hoàn toàn tuần tự, rõ ràng và triệt tiêu hoàn toàn khả năng xảy ra vòng lặp vô hạn (bidding wars) giữa 2 robot tự động.

**Xử lý xung đột bid đồng thời với auto-bid**: vì cả `placeBid` (manual) và `resolvePendingAutoBid` đều giữ `bidLock`, các bid không bao giờ chen vào giữa.

### 6.6 Concurrent Bidding (đấu giá đồng thời)

Yêu cầu 3.2.2 + 3.2.3 trong TASK.md: không lost update, không rollback, không 2 người cùng thắng.

**Cách giải:**
- `LiveAuction.placeBid` toàn bộ trong `bidLock.lock()` → 2 thread vào chỉ 1 chạy, thread kia chờ.
- Khi A vào, B chờ. A check `amount > currentPrice` đúng → set state → giải lock. B vào, đọc lại `currentPrice` đã là giá A → nếu giá B không đủ vượt → ném `InvalidBidException`. Không lost update.
- `BidService.placeBid` cũng `synchronized` ở mức service → bảo vệ thêm các thao tác wallet (cọc tiền) bao quanh `liveAuction.placeBid`.

**Câu hỏi truy ngược**: "Tại sao có 2 lớp khóa (synchronized ở BidService + ReentrantLock ở LiveAuction)?"
- BidService.synchronized bảo vệ deposit hold + persist DB (multi-table consistency).
- LiveAuction.bidLock bảo vệ in-RAM state của riêng phiên đó.
- Nếu chỉ một: hoặc deposit không atomic với bid; hoặc 2 phiên khác nhau không thể xử lý song song (BidService.synchronized chặn cả 2 phiên → không scale).

> Thực tế hiện trạng: `BidService` `synchronized` chặn 2 bid của 2 phiên *khác nhau* — đây là điểm yếu, scale kém. Cải tiến: thay bằng lock theo `auctionId`. Sẽ ghi trong §8.

### 6.7 Realtime Update (Push)

1. Khi client mở `BiddingView`, `BiddingController.initialize` → `BidClient.subscribe(token, auctionId)` → server `BidController.handleSubscribe` → `AuctionManager.addObserver(auctionId, clientHandler)`.
2. Khi bid xảy ra: `LiveAuction.notifyObservers(record)` → gọi `AuctionManager.onBidPlaced(auctionId, record)` → relay tới mọi `ClientHandler` trong observer set.
3. `ClientHandler.onBidPlaced` build `PushEvents.BidUpdatePush(auctionId, bidDTO)` → `sendPush(PushMessage(BID_UPDATE, dto))` → JSON dòng đơn qua socket.
4. Client `ServerConnection` listener thread thấy `type="PUSH"` → callback `pushListener.onPushMessage` → `BiddingController.handlePush` → `Platform.runLater` → update label giá, table history, biểu đồ.
5. `UNSUBSCRIBE` khi user thoát view.

### 6.8 Vòng đời thanh toán (Wallet + Deposit + Payment)

```
[Bidder có balance ≥ 10% × startingPrice]
            │
            ▼ (bid lần đầu)
[holdAuctionDeposit: balance -= 10%; frozenBalance += 10%; lưu AuctionDepositRefSchema]
            │
            ▼ (bid n lần — chỉ hold 1 lần)
[Phiên kết thúc]
   ├─ Có winner:
   │    ├─ refundDepositsExcept(winner) → các loser: frozen → balance
   │    └─ status = WAITING_PAYMENT; schedule expire +5 phút
   │
   └─ Không ai bid:
        ├─ refund tất cả (không có winner)
        └─ status = CANCELED

[WAITING_PAYMENT — chờ winner thanh toán]
   ├─ Winner gọi PAY_AUCTION trong 5 phút:
   │    ├─ payAuctionRemaining(winner, totalPrice - depositAmount): balance -= remaining
   │    ├─ forfeitAuctionDeposit(winner, depositAmount): frozenBalance -= depositAmount (đã chuyển vào tổng)
   │    ├─ payoutSeller(seller, totalPrice): balance += totalPrice
   │    └─ status = PAID
   │
   └─ Winner không thanh toán → expirePaymentDeadline (scheduler):
        ├─ forfeitAuctionDeposit(winner, depositAmount) — winner mất 10% deposit
        └─ status = CANCELED
```

**Mọi method `WalletService` đều `synchronized`** → các bước trên không bị xen kẽ giữa các thread.

Mọi giao dịch được ghi `TransactionSchema` (TransactionType: WALLET_DEPOSIT, WALLET_WITHDRAW, AUCTION_DEPOSIT_HOLD/REFUND/FORFEIT, AUCTION_PAYMENT, SELLER_PAYOUT) với `balanceBefore/After`, `frozenBefore/After` → audit trail.

### 6.9 Auto-end và Self-healing

- `scheduleAuctionEnd(id, endTime)` đặt task vào `ScheduledExecutorService`.
- Khi đến giờ → callback `AuctionService.endAuction(id)`.
- Nếu server crash trước khi task chạy:
  - Khi restart, `reconcileAuctionsFromDatabase()` quét → phiên `RUNNING` với `endTime` đã qua → end ngay (catch-up).
- Nếu task đã trong queue nhưng status đã đổi (do reconcile chạy trước) → `endAuction` check `status != RUNNING` → early return.

---

## 7. Các tình huống "Nếu... thì sao?" (What-If)

GV rất thích đào loại câu này. Mỗi tình huống đều có cấu trúc *điều gì xảy ra → hệ thống xử lý ra sao → điểm tinh tế*.

### 7.1 Nếu Admin xóa Auction trong khi đang RUNNING?

**Hiện trạng**: chưa có action `DELETE_AUCTION` cho Admin. Nếu thêm:
- File `auctions.json` xóa entry, nhưng `liveAuctions` trong RAM còn → bid tiếp tục cập nhật RAM → khi `syncAuctionState` ghi lại schema → schema "phục sinh" (race).
- Cách đúng: `auctionService.deleteAuction(admin, auctionId)`:
  1. Acquire lock của auction nếu có.
  2. `auctionManager.cancelAllTasks(id)` + `removeLiveAuction(id)`.
  3. `refundDepositsExcept(null)` — hoàn cọc cho tất cả.
  4. `auctionDAO.delete(id)` (hoặc set status `CANCELED` để giữ audit trail).
  5. Notify observers `onAuctionEnded(null winner)`.

**Tinh tế khi trả lời**: phải bảo vệ atomic giữa "xóa file" và "xóa RAM" — chuẩn là transition status thành `CANCELED` thay vì xóa hẳn để giữ history.

### 7.2 Nếu 2 bidder bid cùng giá cùng lúc?

- `bidLock` đảm bảo chỉ 1 thread vào `placeBid` tại 1 thời điểm.
- Thread A vào trước → set `currentHighestBid = X`, `currentWinnerId = A`. Trả `BidRecord`.
- Thread B vào sau → đọc lại `currentPrice = X`. Check `B.amount > X`? Nếu cùng giá → `amount <= currentPrice` → `InvalidBidException("Giá phải lớn hơn giá hiện tại: X")`. Deposit của B vừa hold sẽ được rollback ở `BidService.placeBid` catch block.
- → Không có 2 người cùng thắng. A thắng do tới trước.

### 7.3 Nếu network bị ngắt giữa lúc client đang nhận response?

- Client `ServerConnection.sendRequest` → `responseQueue.poll(10s)` → timeout → throw `Exception("Server khong phan hoi (timeout)!")`.
- Server đã commit DB (deposit, bid history) → state đúng. Lần sau client reconnect + refresh sẽ thấy bid đã ghi nhận.
- **Nếu mất kết nối giữa lúc gửi (response thực sự chưa về)**: server không bị duplicate vì 1 request = 1 socket write từ client. Tuy nhiên client có thể re-submit nếu user click lại → có thể tạo bid duplicate. Cải tiến: idempotency key (hiện chưa có).

### 7.4 Nếu Seller cố bid vào phiên của chính mình?

```java
if (bidder.getId().equals(sellerId))
    throw new InvalidBidException("Người bán không được tự đấu giá");
```
Ném ngay trong `LiveAuction.placeBid` — không kể vai trò Bidder hay Seller (Seller cũng có thể có `PLACE_BID` permission về mặt enum). Đây là rule business chứ không phải permission.

### 7.5 Nếu Server crash giữa lúc xử lý `placeBid`?

- Nếu crash *trước* khi `auctionDAO.update(schema)` → DB chưa có giá mới. Khi restart, reconcile đọc schema cũ → state cũ. **Bid bị mất** (không lưu).
- Nếu crash *sau* `auctionDAO.update` nhưng *trước* `bidDAO.save` → schema có `highestBid` mới nhưng không có `BidSchema` tương ứng. `bidHistory` thiếu 1 record. **Inconsistent**.
- **Điểm yếu thừa nhận**: không có distributed transaction. Cách giảm thiểu hiện tại: `BidService.placeBid` ghi schema *sau* khi `liveAuction.placeBid` trả `BidRecord`, và `bidDAO.save` chạy sau `syncAuctionState`. Đảo ngược: nếu để `bidDAO.save` chạy *trước* `syncAuctionState`, crash giữa sẽ leak BidSchema không có schema mới.

**Hướng cải tiến**: dùng SQLite + transaction, hoặc append-only write-ahead log.

### 7.6 Nếu Winner không thanh toán?

- Schedule `expirePaymentDeadline` chạy sau 5 phút.
- Đến giờ → `walletService.forfeitAuctionDeposit(winnerId, depositAmount, auctionId, ref.transactionId)` — winner mất 10% deposit.
- `status = CANCELED`.
- Hiện tại deposit forfeit *chưa* chuyển sang cho Seller (vì có thể được xem là "phí phạt cho hệ thống"). Đây là quyết định business; có thể đổi thành `payoutSeller(seller, depositAmount, ...)` nếu muốn.

### 7.7 Nếu Bidder có 0đ trong ví?

- `placeBid` → `ensureDepositFrozen` → `walletService.holdAuctionDeposit` → check `balance < amount` → throw `IllegalArgumentException("So du khong du")`.
- `GlobalExceptionHandler` → `Response.error("Dữ liệu không hợp lệ: So du khong du")`.
- Bid không được tạo, `liveAuction` không bị thay đổi.

### 7.8 Nếu cùng 1 user đăng nhập từ 2 máy khác nhau?

- Mỗi lần login → `SessionManager.createSession(user)` → trả token mới (UUID). 2 token cùng map về 1 User.
- 2 client đều hoạt động bình thường (token khác nhau, validate ra cùng user).
- Nếu muốn enforce 1-device-1-user: gọi `sessionManager.invalidateByUserId(user.id)` trước `createSession` → mọi token cũ bị xóa, client cũ thấy "Token không hợp lệ" lần request tiếp theo.

### 7.9 Nếu 2 thread cùng register username giống nhau?

- `AuthService.register` không `synchronized`. Race khả thi:
  - T1 `findByUsername("a")` = null.
  - T2 `findByUsername("a")` = null.
  - T1 `userDAO.save` → file có "a".
  - T2 `userDAO.save` → file có "a" lần 2 (duplicate).
- **Điểm yếu thừa nhận**. Cách fix: `synchronized` `register` hoặc lock cấp `UserDAO`.

### 7.10 Nếu file JSON bị xóa giữa lúc server đang chạy?

- `JsonFileHelper.readList` đọc file không tồn tại → trả list rỗng. Các bid tiếp theo "thấy" auction không tồn tại → `requireLiveAuction` (lookup theo RAM) vẫn ok nếu phiên đang RUNNING (vì RAM còn), nhưng `syncAuctionState` ghi lại schema → file phục sinh, mất user/item khác.
- Đây là edge case nghiêm trọng — JSON-file-as-DB không chống chịu manipulation từ ngoài.

### 7.11 Nếu Client gửi JSON sai format?

- `MessageSerializer.deserialize` catch `JsonSyntaxException` → trả `null`.
- `ClientHandler.processLine` check `request == null || request.getAction() == null` → log "JSON khong hop le" → trả `Response.error("Invalid JSON format or missing action")`. Connection không bị đóng.

### 7.12 Nếu Bidder bid xong rồi rút tiền trong ví?

- Số tiền cọc đã ở `frozenBalance`, không thể rút (`withdraw` chỉ trừ `balance`).
- Phần `balance` còn lại rút bình thường. Khi thanh toán cuối phiên, `payAuctionRemaining` cần `balance >= remaining` → có thể fail.
- → Bidder bị forfeit deposit nếu không nạp đủ.

### 7.13 Nếu phiên có nhiều auto-bid cùng `maxBid` lớn nhất?

- Comparator phá hòa bằng `registeredAt`, rồi `registrationOrder`.
- Người đăng ký auto-bid sớm hơn được giữ vị trí thắng (eBay convention).
- Khi giá leo đến `maxBid`: cả 2 không thể bid cao hơn, người sớm hơn vẫn dẫn đầu.

### 7.14 Nếu Seller muốn sửa item đang được dùng trong auction RUNNING?

- Hiện tại `ItemService.updateItem` không kiểm tra. **Điểm yếu**.
- Đúng phải: nếu item đang được dùng trong auction `OPEN` hoặc `RUNNING` → reject hoặc chỉ cho sửa các field không ảnh hưởng (mô tả, ảnh).

### 7.15 Nếu Anti-sniping kích vô tận (mỗi bid mới đều gia hạn)?

- Đúng — đây là intended behavior! Mỗi bid trong cửa sổ X giây cuối đều thêm Y giây.
- Tradeoff: phiên có thể không bao giờ kết thúc nếu có user spam bid mỗi 5 giây.
- Cải tiến (chưa có): hard cap — tối đa N lần gia hạn, hoặc tổng cộng không quá tổng thời gian Z. eBay giới hạn 5 phút mỗi lần snipe.

### 7.16 Nếu User thoát app giữa lúc subscribe?

- Server không phát hiện ngay. Lần đẩy push tiếp theo → `out.println` trên socket đã đóng → `PrintWriter` set error flag, không ném exception (default behavior). Push bị nuốt.
- Khi `ClientHandler.run` thoát do `IOException` → `cleanup` chạy nhưng *không* remove khỏi observer set. **Memory leak nhẹ** — observer dead vẫn nằm trong map.
- **Cải tiến cần**: trong `cleanup` gọi `AuctionManager.removeObserver(allAuctions, this)` hoặc track list auction đã subscribe rồi remove.

### 7.17 Nếu thời gian server và client lệch nhau?

- Server dùng `LocalDateTime.now()` cho mọi check (auction start/end, deposit timestamp).
- Client chỉ hiển thị — không dùng cho business logic.
- → Lệch giờ client không ảnh hưởng business. Nhưng nếu server lệch giờ → toàn hệ thống lệch (auction kết thúc sớm/muộn).

### 7.18 Nếu Admin đổi role của user đang login?

- Hiện chưa hỗ trợ. Nếu có: cần `sessionManager.invalidateByUserId(userId)` để buộc user login lại — vì `User` đối tượng trong session vẫn là role cũ.

### 7.19 Nếu nhiều client subscribe cùng 1 auction (>1000)?

- `Set<AuctionObserver>` lưu mọi observer. Notify loop O(N).
- Mỗi `sendPush` là một `out.println` (socket write). Notify đồng bộ → bid thứ 1001 chậm vì phải đợi 1000 lần write.
- **Cải tiến**: async notify (push qua executor), hoặc `WeakReference` cho observer.

### 7.20 Nếu Maven build failed do conflicting dependencies?

- `auction-protocol` là parent shared. Cả `auction-client` và `auction-server` cùng phiên bản Gson, JUnit. Không có `provided`/`runtime` lẫn nhau.
- Nếu lệch: dùng `mvn dependency:tree` để xem cây phụ thuộc.

---

## 8. Tự nhận xét điểm mạnh — điểm yếu

> Phần này GV chấm cao nhất khi nhóm tự nhận diện được. Không khoe — phải nói thật.

### 8.1 Điểm mạnh

1. **Tách tầng 4 lớp rõ ràng** (View / Network / Domain / Persistence) — kiểm thử và mở rộng dễ.
2. **`auction-protocol` là module riêng** — contract giữa client/server không lệch.
3. **Concurrency design có chủ đích**: 2 lớp khóa (`BidService.synchronized` + `LiveAuction.bidLock`), chọn đúng `ReentrantLock` cho re-entry, `ConcurrentHashMap`/`CopyOnWriteArrayList` cho read-heavy structure.
4. **Self-healing** qua `reconcileAuctionsFromDatabase` — server crash & restart không kẹt state.
5. **Audit trail đầy đủ**: mỗi giao dịch tiền tệ tạo `TransactionSchema` với `balanceBefore/After` — dễ debug + đối soát.
6. **Anti-sniping + Auto-bid** chạy *trong* `bidLock` của bid trigger → atomic không cần lock riêng.
7. **Push qua TCP cùng socket** — tiết kiệm tài nguyên, đơn giản hơn WebSocket.
8. **Exception phân loại**: `GlobalExceptionHandler` tách Business (WARN, không stack trace) vs System (ERROR, full stack) — log không bị nhiễu.
9. **CI/CD**: GitHub Actions chạy `mvn test` mỗi PR (`.github/workflows/ci.yml`).
10. **Test phân tầng**: Unit test (`PasswordUtilsTest`...), Service test với mock DAO (`AuthServiceTest`...), Integration test E2E qua socket (`AuthIntegrationTest`, `AuctionIntegrationTest`).
11. **DI qua constructor** ở Service (không gọi Singleton từ bên trong Service) → test inject dễ.
12. **Immutable `BidRecord`, `Item`** → thread-safe miễn phí.
13. **DTO che `hashedPassword`/`passwordSalt`** không lộ qua mạng.
14. **Conventional commits + branch-per-feature** — git history rõ ràng.

### 8.2 Điểm yếu (thừa nhận thẳng)

1. **JSON-file-as-DB**: đọc cả file mỗi `findById` → O(N) mọi truy vấn. Không có index, không có transaction. Crash giữa write → file có thể corrupt. Comment trong `UserDAO.java` thừa nhận đây là tạm thời.
2. **`BidService.placeBid` `synchronized` ở mức class**: 2 phiên đấu giá khác nhau không thể xử lý bid song song. Scale kém. Cải tiến: lock theo `auctionId`.
3. **`AuthService.register` không synchronized** → race tạo 2 username trùng.
4. **`ItemService.updateItem` không kiểm tra item có đang trong auction RUNNING** → seller có thể đổi giá khởi điểm giữa chừng (về schema; runtime không bị vì `LiveAuction` đã copy `startingPrice` lúc start).
5. **Subscribe leak**: client disconnect không clean observer khỏi `AuctionManager`. Push tới socket đóng → bị nuốt nhưng observer reference vẫn còn → memory leak nhẹ.
6. **Anti-sniping không có cap**: phiên có thể gia hạn vô tận nếu spam bid.
7. **Không có rate limit** ở tầng socket → DoS dễ.
8. **Mỗi client một thread** (không pool) → quá 200 client là vỡ trận.
9. **Forfeit deposit không trả cho Seller** — đây là quyết định business chưa chốt, hiện tại tiền forfeit "biến mất" về phía hệ thống.
10. **Auto-bid logic có nhánh phức tạp (`canClaimCurrentPriceTie`, `calculateProxyAutoBidAmount`)** — đã test nhưng chưa property-based test. Edge case có thể còn sót.
11. **Không có CSRF token / replay attack protection** — nếu attacker capture được token, có thể replay request.
12. **Password hash không dùng PBKDF2/bcrypt/scrypt** — SHA-256+salt một vòng, dễ brute-force trên GPU. Đủ cho BTL, không đủ cho prod.
13. **Không có pagination** ở `GET_AUCTIONS` — trả toàn bộ list.
14. **Logger ghi vào file flat** (`logs/`), không rotate.
15. **Refactor chưa loại hết duplicate**: `AuthService.login()` switch role + `UserMapper.toDomain` switch role — nên tập trung vào 1 `UserFactory`.
16. **Không có `equals/hashCode` cho `BidRecord`** — nếu lưu vào `Set` không hoạt động đúng (hiện chỉ dùng `List` nên không lộ).
17. **Logging level cố định** — không config dynamic qua file.
18. **AuctionService `synchronized` ở mức instance**: tương tự BidService — scale kém.
19. **Không có integration test cho Wallet/Payment flow đầy đủ** — chỉ unit test `WalletServiceTest`.
20. **Hardcode constants**: `ESCROW_DEPOSIT_RATE = 0.10`, `PAYMENT_DEADLINE_MINUTES = 5`, `RECONCILE_INTERVAL_SECONDS = 30` — nên đưa vào config file.

### 8.3 Đã đáp ứng tới đâu so với bảng chấm (TASK.md §157)

| Hạng mục | Điểm | Trạng thái |
|----------|------|-----------|
| Thiết kế lớp + cây kế thừa | 0.5 | ✅ User/Schema/Item/Auction hierarchy |
| Nguyên tắc OOP (4) | 1 | ✅ Đầy đủ |
| Design Pattern | 1 | ✅ Singleton, Observer, Factory, Template, DAO, DTO, MVC |
| Quản lý người dùng + sản phẩm | 1 | ✅ Register/Login + CRUD Item (chưa có update/delete cho item) |
| Chức năng đấu giá | 1 | ✅ Create/Start/Bid/End |
| Xử lý lỗi & ngoại lệ | 1 | ✅ Custom exception + GlobalExceptionHandler |
| Concurrent bidding | 1 | ✅ ReentrantLock + synchronized 2 lớp |
| Realtime update | 0.5 | ✅ Push qua TCP, Observer chain |
| Client–Server architecture | 0.5 | ✅ Module hóa rõ |
| MVC | 0.5 | ✅ JavaFX + FXML + Controller |
| Maven, coding convention | 0.5 | ✅ Multi-module Maven |
| Unit Test (JUnit) | 0.5 | ✅ 9 test files |
| CI/CD | 0.5 | ✅ GitHub Actions `ci.yml` |
| **Auto-Bidding** | 0.5 | ✅ Pending State + proxy logic |
| **Anti-sniping** | 0.5 | ✅ X-Y config per auction |
| **Bid history visualization** | 0.5 | ⚠️ Có biểu đồ (cần kiểm tra `BiddingController`) |
| **Tự sáng tạo** | 0.5 | ✅ Wallet + Deposit + Forfeit + Payment deadline + Reconcile |
| **TỔNG** | **10+1** | |

---

## 9. Bộ câu hỏi vấn đáp dự đoán + đáp án ngắn

> Đáp án trong 1-3 câu — đủ ngắn để học thuộc, đủ chất để pass.

### Patterns & OOP

**Q1. Vì sao chọn Singleton cho SessionManager?**
A: Bảng token-↔-user là shared state toàn server. Nhiều ClientHandler thread phải nhìn cùng một map; nếu mỗi thread tạo instance riêng, login ở thread A xong, validate ở thread B sẽ fail. Eager init bằng `static final` đảm bảo thread-safe không cần lock.

**Q2. Sự khác nhau giữa Schema, Domain Model, DTO?**
A: Schema = on-disk (1:1 với JSON, có hashedPassword). Domain = runtime có behavior (`placeBid`, `hasPermission`). DTO = wire format, đã lược bỏ field nhạy cảm. Mapper chuyển 3 chiều.

**Q3. Liệt kê đầy đủ design pattern + vị trí.**
A: Singleton (SessionManager, AuctionManager, DataManager, ServerConnection, SceneManager, ClientSession). Observer (LiveAuction ↔ AuctionObserver ↔ AuctionManager ↔ ClientHandler). Factory Method (AuthService.login, ItemMapper.toNewSchema). Template Method (BaseSchema). DAO (GenericDAO<T> + 5 impl). DTO (auction-protocol/dto/). MVC (JavaFX). Strategy ẩn (User.hasPermission override).

**Q4. Tại sao Observer chứ không phải polling?**
A: Yêu cầu 3.2.4 cấm polling. Observer cho phép push <100ms vs 1-2s polling, đỡ băng thông gấp ~50 lần.

**Q5. Polymorphism trong dự án xuất hiện ở đâu?**
A: `User u = new Bidder(...); u.hasPermission(Permission.PLACE_BID)` — gọi theo dynamic type. `AuctionObserver` interface — LiveAuction notify không biết observer cụ thể là ClientHandler hay AuctionManager. `GenericDAO<T>` — service gọi `userDAO.save()` không quan tâm DAO là JSON hay SQL.

### Concurrency

**Q6. Vì sao `LiveAuction.placeBid` cần `ReentrantLock`?**
A: Để chống lost update khi 2 bidder bid đồng thời. ReentrantLock cho phép cùng thread re-acquire (placeBid → resolveAutoBids cùng cần lock) — `synchronized` cũng reentrant nhưng ReentrantLock linh hoạt hơn (tryLock, fairness).

**Q7. Vì sao mọi method WalletService `synchronized`?**
A: Tiền phải atomic: đọc-tính-ghi. 2 lệnh withdraw đồng thời mà không sync → cả hai thấy đủ balance → trừ cả hai → âm.

**Q8. CopyOnWriteArrayList với ConcurrentHashMap khác gì?**
A: `CopyOnWriteArrayList` cho read nhiều, write ít (mỗi write copy array). `ConcurrentHashMap` segmented locking, đọc concurrent thoải mái. Trong dự án: observer list = COWArrayList; tokenMap/liveAuctions = CHM.

**Q9. Có deadlock không?**
A: Khả năng thấp vì lock luôn theo thứ tự cố định: BidService (instance) → LiveAuction (bidLock). WalletService độc lập, gọi *trước* khi vào LiveAuction.placeBid. Không có cross-lock giữa 2 LiveAuction.

**Q10. Vì sao ScheduledExecutorService pool=2?**
A: 2 nhóm task: per-auction (start/end/payment, có thể nhiều task song song) + periodic reconcile. Single thread không đủ — nếu reconcile chạy lâu sẽ block end task.

### Architecture

**Q11. Vì sao tách auction-protocol thành module Maven riêng?**
A: Shared contract giữa client/server. Cùng dependency 1 module → cùng version. Refactor enum ActionType thêm action mới → cả hai bên thấy ngay, không lệch.

**Q12. Tầng nào được phép truy cập DAO?**
A: Chỉ Service trong domain layer. Controller chỉ gọi Service. View không bao giờ thấy DAO. Nếu cho Controller gọi DAO trực tiếp → business logic rò xuống tầng network.

**Q13. Server đa luồng kiểu gì?**
A: 1 thread cho accept loop; mỗi client accept tạo 1 thread mới (`new Thread(handler).start()`) không qua pool. Đơn giản, đủ cho BTL. Cải tiến: `ExecutorService` pool có giới hạn.

**Q14. Vì sao TCP raw thay vì HTTP?**
A: Cần push từ server → REST phải thêm WebSocket/SSE. TCP raw chỉ một kết nối persistent đẩy cả request/response/push. Tradeoff: tự viết toàn bộ middleware (auth, rate-limit, gateway).

### Business logic

**Q15. Auto-bid hoạt động thế nào?**
A: Hệ thống áp dụng quy tắc: **chỉ người dẫn đầu (high bidder) mới được phép kích hoạt Auto-Bid**. Khi đối thủ đặt bid thủ công vượt mặt, server tạo một trạng thái chờ (`PendingAutoBid`) cho người dẫn đầu cũ. Sau đó, một thread bất đồng bộ gọi `resolvePendingAutoBid` trong `bidLock` để nâng giá đè lên (bằng `currentHighestBid + increment`). Nếu vượt quá `maxBid`, cấu hình Auto-Bid tự động bị tắt. Cơ chế này loại bỏ hoàn toàn bidding wars vô hạn của 2 robot tự động.

**Q16. Anti-sniping?**
A: Mỗi auction lưu (X, Y). Trong `placeBid`, sau khi update giá nếu `endTime - now ≤ X giây` → `endTime += Y giây`, notify `onAuctionExtended` → AuctionManager cancel end task cũ + schedule cái mới.

**Q17. State machine của Auction?**
A: OPEN (mới tạo, chưa start) → RUNNING (đang nhận bid, sau khi `startAuction` hoặc tới `startTime` qua scheduler) → FINISHED (đã hết giờ) → WAITING_PAYMENT (có winner) → PAID (winner thanh toán). Nhánh CANCELED nếu không ai bid, hoặc winner quá hạn 5 phút không trả.

**Q18. Deposit 10% là gì?**
A: Trước khi bid lần đầu trong 1 phiên, hệ thống freeze 10% giá khởi điểm: `balance -= 10%; frozenBalance += 10%`. Ngăn "hét giá ảo". Cuối phiên: loser được refund (frozen → balance); winner giữ frozen, dùng làm partial payment khi `payAuction`. Nếu winner không trả trong 5 phút → forfeit (mất frozen).

**Q19. Vì sao có reconcileAuctionsFromDatabase chạy mỗi 30s?**
A: Self-healing. Server crash mất state RAM nhưng schema JSON còn → restart phải nạp lại LiveAuction cho phiên RUNNING, kết thúc phiên đã quá giờ, expire payment quá hạn. Định kỳ 30s phòng clock drift.

**Q20. Vì sao password cần salt?**
A: Chống rainbow table — 2 user cùng password khác nhau hash khác nhau. SHA-256(password + salt). Tradeoff: nên dùng PBKDF2/bcrypt thay vì SHA-256 1 vòng — chưa làm vì BTL không yêu cầu.

### Tinh tế nhỏ

**Q21. Vì sao `getBidHistory()` trả `new ArrayList<>(bidHistory)`?**
A: Encapsulation. Trả reference gốc → caller có thể `bidHistory.clear()` phá state. Trả copy → caller chỉ thấy snapshot.

**Q22. Vì sao `BidRecord` immutable?**
A: Thread-safe miễn phí. Lưu vào nhiều list (`bidHistory`, observer payload) không lo race. Không setter → không lost update.

**Q23. AtomicLong trong AutoBidConfig dùng làm gì?**
A: `REGISTRATION_SEQUENCE` đảm bảo mỗi config có số thứ tự đăng ký duy nhất và nghiêm ngặt khi người dùng cập nhật cấu hình, làm mốc deterministic so sánh hoặc định danh nếu có tích hợp mở rộng trong tương lai.

**Q24. Vì sao có 2 lớp synchronized (BidService + LiveAuction)?**
A: BidService.synchronized bảo vệ multi-resource transaction (deposit hold + bid + ghi DB). LiveAuction.bidLock bảo vệ in-RAM state riêng phiên. Nếu chỉ 1 lớp: hoặc không atomic deposit-bid, hoặc 2 phiên không xử lý song song được.

**Q25. PushMessage và Response phân biệt thế nào trên client?**
A: Cùng dòng JSON qua 1 socket. Server gắn field `type` ("PUSH" hoặc "RESPONSE"). Client listener thread đọc dòng, check type: PUSH → callback pushListener; còn lại → enqueue responseQueue cho sendRequest poll.

**Q26. GlobalExceptionHandler phân loại exception ra sao?**
A: Business exception (Authentication, UserNotFound, Duplicate, Auction, IllegalArgument, DateTimeParse) → log WARN, không stack trace. System exception → log ERROR + full stack. Để log không bị nhiễu khi user nhập sai mật khẩu hàng trăm lần.

**Q27. Vì sao ServerConnection.sendRequest synchronized?**
A: Nhiều UI controller có thể gửi request song song. Nếu không sync → 2 lệnh `out.println` xen ký tự → JSON hỏng phía server.

**Q28. `equals/hashCode` cài ở đâu?**
A: `BaseSchema` cài dựa trên `id` → 2 schema cùng id là bằng nhau. Domain model (`User`, `Item`...) không override → so sánh reference. Tradeoff: nếu cần compare domain cần biết là so reference, không phải value.

### Test & CI/CD

**Q29. Test E2E chạy thế nào?**
A: `TestHelper` start `AuctionServer` thật trên port test (vd 8889) trong `@BeforeEach`. Test mở socket client thật, send Request JSON, assert Response. `@AfterEach` shutdown server.

**Q30. CI/CD chạy gì?**
A: `.github/workflows/ci.yml` chạy `mvn clean test` trên mỗi PR và push main. Pass = mọi JUnit pass. Setup JDK 21, cache Maven.

**Q31. Mock DAO trong unit test?**
A: Truyền vào constructor `new AuthService(new UserDAO("test-data/users.json"), userService)`. File test riêng → không đụng production data. Sau test `Files.delete(...)` để clean.

### Truy ngược tinh tế

**Q32. Vì sao không gộp Bidder/Seller/Admin thành 1 class User với 1 enum role?**
A: Có thể, nhưng `hasPermission` switch theo enum mãi sẽ vi phạm Open/Closed — thêm role là sửa switch. Subclass override polymorphic + có chỗ thêm field/behavior riêng cho từng role sau này (vd Bidder có `creditScore`).

**Q33. Vì sao không dùng Spring/Spring Boot?**
A: BTL cấp độ giáo trình UET — muốn demo nguyên lý raw. Spring sẽ giấu Singleton, DI, MVC qua annotation, khó vấn đáp.

**Q34. Vì sao auction-protocol có Request/Response chứ không phải REST DTOs?**
A: Vì socket raw, không có Content-Type hay status code HTTP. Tự định nghĩa: Request có `action` (enum) + `data` (map) + `token`. Response có `status` (OK/ERROR) + `data` + `message`. PushMessage là loại Response đặc biệt (`type="PUSH"`).

**Q35. Vì sao tách BidController khỏi AuctionController?**
A: SRP. AuctionController quản lý lifecycle (create/start/end). BidController xử lý đặt giá, auto-bid, subscribe. Mỗi controller < 100 dòng.

---

## 10. Cheat sheet — số liệu cần thuộc lòng

- **Cổng server**: 8888.
- **Deposit rate**: 10% (`ESCROW_DEPOSIT_RATE = 0.10` trong `BidService`).
- **Payment deadline**: 5 phút (`AuctionManager.PAYMENT_DEADLINE_MINUTES`).
- **Reconcile interval**: 30s (`AuctionManager.RECONCILE_INTERVAL_SECONDS`).
- **Scheduler pool size**: 2.
- **Response timeout client**: 10 giây (`responseQueue.poll(10, SECONDS)`).
- **Password hash**: SHA-256 + 16-byte salt → 32 hex chars.
- **Token**: `UUID.randomUUID()` (36 chars).
- **Status auction**: OPEN → RUNNING → FINISHED → WAITING_PAYMENT → PAID/CANCELED.
- **6 vai trò trong Permission enum**: 24 quyền tổng (xem `Permission.java`).
- **Số file Java khoảng**: ~80 (server + client + protocol).
- **Số fxml**: 13.
- **Số DAO**: 5 (User, Item, Auction, Bid, Transaction).
- **Số Service**: 7 (Auth, User, Item, Auction, Bid, Wallet, Transaction).
- **Số Controller server**: 6 (Auth, User, Item, Auction, Bid, Wallet, Admin).
- **Số Singleton**: 6 (SessionManager, AuctionManager, DataManager + ServerConnection, SceneManager, ClientSession client).
- **Số TransactionType**: WALLET_DEPOSIT, WALLET_WITHDRAW, AUCTION_DEPOSIT_HOLD, AUCTION_DEPOSIT_REFUND, AUCTION_DEPOSIT_FORFEIT, AUCTION_PAYMENT, SELLER_PAYOUT.

---

## Phụ lục A — Bảng tra cứu file theo chức năng

| Cần xem | File |
|---------|------|
| Login logic | `auction-server/.../domain/service/AuthService.java` |
| Bid logic + concurrency | `auction-server/.../domain/model/LiveAuction.java`, `domain/service/BidService.java` |
| Auto-bid algorithm | `LiveAuction.resolveAutoBids`, `domain/model/AutoBidConfig.java` |
| Anti-sniping | `LiveAuction.extendIfSniping` |
| Wallet | `domain/service/WalletService.java` |
| State machine + reconcile | `domain/service/AuctionService.java`, `domain/manager/AuctionManager.java` |
| Socket server | `network/server/AuctionServer.java`, `ClientHandler.java`, `RequestRouter.java` |
| Exception central | `network/server/GlobalExceptionHandler.java` |
| Push/Realtime client | `auction-client/.../network/ServerConnection.java` |
| Singleton session | `domain/manager/SessionManager.java` |
| Permission rule | `auction-protocol/.../enums/Permission.java`, các subclass `User` |
| Schema/DTO/Domain mapping | `mapper/UserMapper.java`, `mapper/ItemMapper.java`, ... |
| Test unit | `auction-server/src/test/java/.../UnitTest/`, `JUnitTest/` |
| Test integration | `auction-server/src/test/java/.../IntegrationTest/` |
| CI | `.github/workflows/ci.yml` |

---

## Phụ lục B — Lời khuyên vấn đáp

1. **Mỗi câu trả lời nên gồm 3 nhịp**: *(1) ý chính gì → (2) vì sao chọn → (3) tradeoff/alternative*.
2. **Khi GV truy ngược**: đừng phòng thủ. Thừa nhận điểm yếu (đã liệt kê §8.2) trước khi GV tìm ra → ghi điểm trung thực.
3. **Không bịa**: nếu không biết, nói "phần đó em chưa làm/chưa rõ, em đoán là... vì..." — có ý đoán có cơ sở vẫn tốt hơn im lặng.
4. **Mở file thật khi mô tả**: chỉ chính xác file + dòng. `LiveAuction.java:83 — bidLock.lock()` thuyết phục hơn "trong class LiveAuction có lock".
5. **Phối hợp nhóm**: phân chia chuyên môn (xem `members/*.md`) — câu thuộc tầng nào, người phụ trách trả lời chính. Nhưng *mọi người đều phải hiểu tổng quan* (yêu cầu kỷ luật ở TASK.md §14).
6. **Demo chạy thật**: chuẩn bị sẵn data/users.json có Bidder/Seller test sẵn, server đã start, client mở 2 cửa sổ để chứng minh realtime push trong demo.
7. **Câu khó nhất GV có thể hỏi**: "Nếu phải rewrite lại từ đầu, em đổi gì?" → trả lời theo §8.2 (top 5): JSON → SQLite, lock theo auctionId, password hash → PBKDF2, observer cleanup, rate limit.
