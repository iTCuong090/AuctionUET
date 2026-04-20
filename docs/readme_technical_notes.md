# 📖 PHAO CỨU SINH — Buổi kiểm tra tiến độ BTL ngày 21/4

> Tài liệu tổng hợp **mọi thứ cần biết** về dự án AuctionUET: ký hiệu, thuật ngữ, kiến trúc, design patterns, câu hỏi khó, kịch bản trình bày, và gợi ý trả lời cho từng thành viên.

---

## Mục lục

- [A. Ký hiệu UML & Mermaid](#a-ký-hiệu-uml--mermaid)
- [B. Thuật ngữ kiến trúc](#b-thuật-ngữ-kiến-trúc)
- [C. Thuật ngữ mạng & giao tiếp](#c-thuật-ngữ-mạng--giao-tiếp)
- [D. Thuật ngữ xác thực & phân quyền](#d-thuật-ngữ-xác-thực--phân-quyền)
- [E. Design Patterns — Giải thích từ code thật](#e-design-patterns--giải-thích-từ-code-thật)
- [F. Thuật ngữ đa luồng](#f-thuật-ngữ-đa-luồng-concurrency)
- [G. Bảng viết tắt](#g-bảng-viết-tắt)
- [H. Kịch bản trình bày 8 phút](#h-kịch-bản-trình-bày-8-phút)
- [I. Ngân hàng câu hỏi khó & gợi ý trả lời](#i-ngân-hàng-câu-hỏi-khó--gợi-ý-trả-lời)
- [J. Câu hỏi riêng cho từng thành viên](#j-câu-hỏi-riêng-cho-từng-thành-viên)
- [K. Checklist trước buổi vấn đáp](#k-checklist-trước-buổi-vấn-đáp)

---

## A. Ký hiệu UML & Mermaid

### A.1. Access Modifiers (Phạm vi truy cập)

| Ký hiệu | Ý nghĩa | Java keyword | Ví dụ trong dự án |
|:--------:|----------|:------------:|-------------------|
| `+` | **public** | `public` | `+getId() String` → ai cũng gọi được |
| `-` | **private** | `private` | `-String username` → chỉ class đó thấy |
| `#` | **protected** | `protected` | `#String id` trong `BaseSchema` → class con dùng được |
| `~` | **package-private** | *(mặc định)* | Hiếm dùng trong dự án |

### A.2. Ký hiệu đặc biệt Mermaid

| Hậu tố | Render ra | Ý nghĩa | Ví dụ |
|--------|-----------|----------|-------|
| `$` | **Gạch chân** | **Static** — thuộc về class, không cần tạo object | `+getInstance() SessionManager$` |
| `*` | *In nghiêng* | **Abstract** — class con bắt buộc phải override | `+hasPermission(String)* boolean` |

> [!WARNING]
> **Bẫy phổ biến:** Gạch chân **KHÔNG PHẢI** abstract! Gạch chân = Static. Abstract = In nghiêng.

### A.3. Stereotype — Nhãn `<<...>>`

| Nhãn | Ý nghĩa | Ví dụ |
|------|----------|-------|
| `<<abstract>>` | Không tạo object trực tiếp, làm class cha | `BaseSchema`, `User`, `ItemSchema` |
| `<<interface>>` | Chỉ khai báo method, không có thân hàm | `GenericDAO<T>`, `AuctionObserver` |
| `<<enumeration>>` | Tập hằng số cố định | `UserRole`, `AuctionStatus`, `ItemType` |
| `<<Singleton>>` | Chỉ tồn tại đúng 1 instance | `SessionManager`, `AuctionManager`, `DataManager` |

### A.4. Mũi tên quan hệ

| Mermaid | UML | Ý nghĩa | Ví dụ | Cách giải thích |
|---------|-----|----------|-------|-----------------|
| `<\|--` | ──▷ nét liền | **Kế thừa** | `User <\|-- Bidder` | "Bidder **là một loại** User" |
| `<\|..` | ──▷ nét đứt | **Implement** | `GenericDAO <\|.. UserDAO` | "UserDAO **cài đặt** interface GenericDAO" |
| `-->` | ──▸ nét liền | **Association** | `User --> UserRole` | "User **có** thuộc tính role" |
| `..>` | ──▸ nét đứt | **Dependency** | `UserDAO ..> JsonFileHelper` | "UserDAO **sử dụng** helper, nhưng không giữ lâu dài" |

> [!TIP]
> **Mẹo nhớ:** Nét liền = quan hệ mạnh/cố định. Nét đứt = quan hệ nhẹ/tạm thời.

### A.5. Multiplicity (Bội số)

`LiveAuction --> BidRecord : contains *` → Một LiveAuction chứa **nhiều** (0..*) BidRecord.

### A.6. Ký hiệu Sequence Diagram

| Mũi tên | Ý nghĩa |
|---------|----------|
| `->>` nét liền | **Gọi hàm** (synchronous) — bên gọi chờ kết quả |
| `-->>` nét đứt | **Trả kết quả** (return) |

**Khối màu `rect`:**

| Màu | Ý nghĩa |
|-----|----------|
| 🟦 Xanh dương nhạt | **Background Thread** — chạy nền, tránh block UI |
| 🟩 Xanh lá nhạt | **Server xử lý** — logic server-side |
| 🟥 Đỏ nhạt | **Platform.runLater** — quay về UI Thread để cập nhật giao diện |
| 🟨 Vàng nhạt | **RAM + Scheduler** — nạp vào bộ nhớ + lên lịch tự động |

**Cấu trúc:** `alt...else...end` = rẽ nhánh (tương đương if-else). `autonumber` = đánh số tự động.

---

## B. Thuật ngữ kiến trúc

### B.1. Phân tầng 4 Layer

```
┌─────────────────────────────────────────────┐
│  NETWORK LAYER (Controller + Router)        │  ← Nhận request, route, trả response
│  AuthController, ItemController, ...        │
├─────────────────────────────────────────────┤
│  SERVICE LAYER (Business Logic)             │  ← Xử lý logic nghiệp vụ
│  AuthService, AuctionService, ItemService   │
├─────────────────────────────────────────────┤
│  DOMAIN LAYER (Model + Enum)                │  ← Đối tượng/quy tắc nghiệp vụ
│  User, LiveAuction, BidRecord, ...          │
├─────────────────────────────────────────────┤
│  PERSISTENCE LAYER (Schema + DAO)           │  ← Đọc/ghi dữ liệu JSON
│  UserSchema, UserDAO, JsonFileHelper, ...   │
└─────────────────────────────────────────────┘
```

**Nguyên tắc:** Tầng trên chỉ gọi tầng dưới, KHÔNG đi ngược lại. VD: `AuthController` → `AuthService` → `UserDAO` ✅. `UserDAO` → `AuthController` ❌.

### B.2. Schema vs Model vs DTO — Phân biệt rõ ràng

| | Schema | Model (Domain) | DTO |
|---|--------|----------------|-----|
| **Ở tầng nào** | Persistence | Domain | Network |
| **Mục đích** | Ánh xạ cấu trúc file JSON | Chứa logic nghiệp vụ | Truyền dữ liệu qua mạng |
| **Có password?** | ✅ Có (`hashedPassword`, `salt`) | ❌ Không | ❌ Không |
| **Có logic?** | ❌ Chỉ getter/setter | ✅ Có (`placeBid()`, `hasPermission()`) | ❌ Chỉ dữ liệu |
| **Ví dụ** | `UserSchema` | `User`, `LiveAuction` | `UserDTO` |

**Tại sao cần 3 loại?**
- `UserSchema` chứa `hashedPassword` — nếu gửi thẳng cho client = **lộ mật khẩu**
- `UserDTO` chỉ có `id`, `username`, `role` — an toàn để truyền qua mạng
- `User` (domain) có method `hasPermission()` — phục vụ logic phân quyền

### B.3. Mapper — Người chuyển đổi

```
Schema ←──[toDomain()]──→ Model ←──[toDTO()]──→ DTO
       ←──[toNewSchema()]──┘
```

**Code thật** (file [UserMapper.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/mapper/UserMapper.java)):
```java
// Factory Method: dựa vào role → tạo đúng subclass
public static User toDomain(UserSchema schema) {
    return switch (schema.getRole()) {
        case BIDDER -> new Bidder(id, username);
        case SELLER -> new Seller(id, username);
        case ADMIN  -> new Admin(id, username);
    };
}
```

---

## C. Thuật ngữ mạng & giao tiếp

| Thuật ngữ | Giải thích | Trong dự án |
|-----------|------------|-------------|
| **TCP Socket** | Kênh truyền dữ liệu tin cậy giữa Client-Server | Server mở `ServerSocket` trên port 8888, Client tạo `Socket("localhost", 8888)` |
| **JSON** | Định dạng text trao đổi dữ liệu | `{"action":"LOGIN","data":{"username":"abc","password":"123"}}` |
| **Serialize** | Java Object → JSON String | `gson.toJson(request)` trong `ServerConnection` |
| **Deserialize** | JSON String → Java Object | `MessageSerializer.deserialize(line)` trong `ClientHandler` |
| **Request** | Client gửi lên Server | Gồm: `action` (ActionType), `data` (Map), `token` |
| **Response** | Server trả về Client | Gồm: `status` ("OK"/"ERROR"), `message`, `data` |
| **ActionType** | Enum liệt kê hành động | `LOGIN`, `REGISTER`, `CREATE_ITEM`, `START_AUCTION`... |
| **RequestRouter** | Bộ "tổng đài" | Nhận Request → dựa vào `action` → `switch-case` → gọi đúng Controller |
| **ClientHandler** | 1 thread / 1 client | Mỗi client kết nối → tạo 1 `ClientHandler` chạy trên thread riêng |

**Luồng giao tiếp 1 request:**
```
Client: gson.toJson(request) → out.println(json) → TCP → Server
Server: in.readLine() → MessageSerializer.deserialize(json) → router.route(req)
      → Controller.handleXxx() → Service → DAO → Response
      → MessageSerializer.serialize(response) → out.println(json) → TCP → Client
Client: in.readLine() → gson.fromJson(json, Response.class) → Xử lý
```

---

## D. Thuật ngữ xác thực & phân quyền

### D.1. Token-based Authentication

```
Login thành công → Server tạo UUID token → lưu vào ConcurrentHashMap(token→User)
                 → gửi token cho Client → Client gửi kèm token trong mọi request sau
Server nhận request → validateToken(token) → tìm trong HashMap → biết đó là ai
```

**Code thật** ([SessionManager.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/service/SessionManager.java)):
```java
public String createSession(User user) {
    String token = UUID.randomUUID().toString();  // VD: "550e8400-e29b-..."
    tokenMap.put(token, user);                    // Lưu vào ConcurrentHashMap
    return token;
}
```

### D.2. SHA-256 + Salt

**Vấn đề:** Nếu hash password thuần (VD: `hash("123456")` = `abc123...`), hacker tra bảng Rainbow Table là ra.

**Giải pháp:** Thêm **Salt** (chuỗi ngẫu nhiên 16 byte) trước khi hash:
```
hash("123456" + "a1b2c3d4...") = "hoàn toàn khác abc123..."
```
Mỗi user có salt riêng → cùng password nhưng hash khác nhau → Rainbow Table vô dụng.

**Code thật** ([PasswordUtils.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/util/PasswordUtils.java)):
```java
public static String hash(String password, String salt) {
    MessageDigest hash = MessageDigest.getInstance("SHA-256");
    hash.update(password.getBytes(StandardCharsets.UTF_8));
    hash.update(salt.getBytes(StandardCharsets.UTF_8));
    byte[] hashPassword = hash.digest();
    return convertToHex(hashPassword);
}
```

### D.3. hasPermission — Đa hình phân quyền

Mỗi role override khác nhau:

| Role | Quyền |
|------|-------|
| **Seller** | `CREATE_ITEM`, `CREATE_AUCTION`, `START_AUCTION`, `VIEW_AUCTION`, ... |
| **Bidder** | `PLACE_BID`, `VIEW_AUCTION`, `VIEW_BID_HISTORY`, `SUBSCRIBE`, ... |
| **Admin** | Tất cả (`return true`) |

**Đây là Polymorphism (đa hình):** Cùng method `hasPermission()`, nhưng mỗi subclass hành xử khác nhau.

---

## E. Design Patterns — Giải thích từ code thật

### E.1. Singleton

> **Ý nghĩa:** Đảm bảo 1 class chỉ có đúng **1 instance** trong toàn bộ chương trình.

**Hai kiểu trong dự án:**

| Kiểu | Khởi tạo khi nào | Thread-safe? | Ví dụ |
|------|-------------------|:------------:|-------|
| **Eager** | Class được load | ✅ Có | `SessionManager`, `AuctionManager`, `DataManager` |
| **Lazy** | Lần gọi đầu tiên | ❌ Không (chưa xử lý) | `ServerConnection`, `SceneManager` |

**Eager Singleton** ([SessionManager.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/service/SessionManager.java)):
```java
private static final SessionManager INSTANCE = new SessionManager(); // Tạo ngay
private SessionManager() {}  // Constructor private → không ai new được
public static SessionManager getInstance() { return INSTANCE; }
```

**Lazy Singleton** ([ServerConnection.java](file:///d:/Project/AuctionUET/auction-client/src/main/java/com/auctionuet/client/network/ServerConnection.java)):
```java
private static ServerConnection instance;     // Chưa tạo
public static ServerConnection getInstance() {
    if (instance == null) instance = new ServerConnection();  // Tạo khi cần
    return instance;
}
```

> [!WARNING]
> **Câu hỏi bẫy:** *"Lazy Singleton của ServerConnection có thread-safe không?"*
> **Trả lời:** Không hoàn toàn. Nhưng ở client chỉ có 1 thread chính gọi `getInstance()`, nên chấp nhận được. Nếu cần thread-safe thì dùng `synchronized` hoặc chuyển sang Eager.

### E.2. Observer Pattern

> **Ý nghĩa:** Khi đối tượng Subject thay đổi → tự động thông báo cho tất cả Observer đang "lắng nghe".

**Trong dự án:**
- **Subject:** `LiveAuction` (quản lý danh sách observers)
- **Observer:** `AuctionObserver` (interface)
- **Sự kiện:** `onBidPlaced(record)`, `onAuctionEnded(id, winner, price)`

**Code thật** ([LiveAuction.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/model/LiveAuction.java)):
```java
private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

private void notifyObservers(BidRecord record) {
    for (AuctionObserver obs : observers) {
        obs.onBidPlaced(record);       // Mỗi observer tự xử lý theo cách riêng
    }
}
```

> [!IMPORTANT]
> `CopyOnWriteArrayList` = List an toàn đa luồng. Khi add/remove observer, nó tạo bản sao mảng mới → thread khác đang duyệt không bị `ConcurrentModificationException`.

### E.3. Factory Method

> **Ý nghĩa:** Tạo đối tượng mà **không cần biết trước chính xác class nào** — quyết định tại runtime dựa vào dữ liệu.

**Ví dụ 1 — UserMapper** ([UserMapper.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/mapper/UserMapper.java)):
```java
public static User toDomain(UserSchema schema) {
    return switch (schema.getRole()) {
        case BIDDER -> new Bidder(id, username);   // role=BIDDER → tạo Bidder
        case SELLER -> new Seller(id, username);   // role=SELLER → tạo Seller
        case ADMIN  -> new Admin(id, username);    // role=ADMIN  → tạo Admin
    };
}
```

**Ví dụ 2 — ItemMapper** ([ItemMapper.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/mapper/ItemMapper.java)):
```java
public static ItemSchema toNewSchema(ItemDTO dto, String sellerId) {
    return switch (type) {
        case ELECTRONICS -> new ElectronicsSchema(...);
        case ART         -> new ArtSchema(...);
        case VEHICLE     -> new VehicleSchema(...);
    };
}
```

**Lợi ích:** Controller/Service không cần biết logic tạo object, chỉ gọi `UserMapper.toDomain()` → nhận đúng loại.

### E.4. Template Method (BaseSchema)

> **Ý nghĩa:** Class cha định nghĩa "khung" chung, class con chỉ cần bổ sung phần riêng.

```java
public abstract class BaseSchema {
    protected String id;               // Tất cả entity đều có id
    protected LocalDateTime createdAt;  // Tất cả đều có thời gian tạo
    protected LocalDateTime updatedAt;  // Tất cả đều có thời gian cập nhật
}

public class UserSchema extends BaseSchema {
    private String username;           // Chỉ User mới có
    private String hashedPassword;     // Chỉ User mới có
}
```

### E.5. DAO Pattern

> **Ý nghĩa:** Tách logic đọc/ghi dữ liệu ra khỏi logic nghiệp vụ.

```java
// Interface chung — bất kỳ entity nào cũng có 5 thao tác CRUD
public interface GenericDAO<T extends BaseSchema> {
    void save(T entity);
    T findById(String id);
    List<T> findAll();
    void update(T entity);
    void delete(String id);
}

// Implementation cụ thể cho User
public class UserDAO implements GenericDAO<UserSchema> {
    // Đọc/ghi file users.json
}
```

**Tại sao cần?** Nếu đổi từ JSON → MySQL, chỉ viết lại `UserDAO` (VD: thay `JsonFileHelper.readList()` → `JDBC.executeQuery()`). `AuthService` và `AuthController` → **KHÔNG CẦN SỬA BẤT CỨ DÒNG NÀO**.

### E.6. MVC (phía Client)

| Thành phần | Vai trò | Trong dự án |
|-----------|---------|-------------|
| **Model** | Dữ liệu + trạng thái | `ClientSession`, `UserDTO`, `ItemDTO` |
| **View** | Giao diện người dùng | Các file `.fxml` (`LoginView.fxml`, `DashboardView.fxml`...) |
| **Controller** | Xử lý sự kiện, kết nối Model & View | `LoginController`, `DashboardController`... |

**Luồng:** Người dùng bấm nút Login (View) → `LoginController.handleLogin()` (Controller) → gọi `AuthClient.login()` → cập nhật `ClientSession` (Model) → chuyển sang `DashboardView.fxml` (View).

### E.7. DTO Pattern

> Truyền dữ liệu an toàn qua mạng — **lọc bỏ thông tin nhạy cảm**.

| Gửi qua mạng | KHÔNG gửi |
|--------------|-----------|
| `UserDTO(id, username, role)` | `hashedPassword`, `passwordSalt`, `email` |
| `ItemDTO(id, name, price, type)` | `sellerId` nội bộ |

---

## F. Thuật ngữ đa luồng (Concurrency)

| Thuật ngữ | Giải thích | Trong dự án |
|-----------|------------|-------------|
| **Thread** | Luồng thực thi độc lập | Mỗi client kết nối → `new Thread(handler).start()` trong `AuctionServer` |
| **Background Thread** | Luồng nền trên client | `new Thread(() -> { ... }).start()` trong `LoginController` |
| **Platform.runLater()** | Chuyển code về JavaFX UI Thread | `Platform.runLater(() -> { errorLabel.setText(...); })` |
| **ConcurrentHashMap** | HashMap thread-safe | `SessionManager.tokenMap` — nhiều ClientHandler cùng đọc/ghi |
| **ReentrantLock** | Khóa cho critical section | `LiveAuction.bidLock` — chỉ 1 thread đặt giá tại 1 thời điểm |
| **ScheduledExecutorService** | Lên lịch chạy task tương lai | `AuctionManager.scheduler` — tự động gọi `endAuction()` khi hết giờ |
| **CopyOnWriteArrayList** | List thread-safe | `LiveAuction.observers` — add/remove observer an toàn |

**Code minh chứng — ReentrantLock** ([LiveAuction.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/model/LiveAuction.java#L48-L78)):
```java
public BidRecord placeBid(User bidder, double amount) {
    bidLock.lock();           // Khóa — các thread khác phải chờ
    try {
        // Validate + update state (chỉ 1 thread thực hiện)
        if (amount <= currentHighestBid) throw new InvalidBidException(...);
        this.currentHighestBid = amount;
        this.currentWinnerId = bidder.getId();
        notifyObservers(record);
        return record;
    } finally {
        bidLock.unlock();     // MỌI TRƯỜNG HỢP đều unlock (kể cả exception)
    }
}
```

> [!IMPORTANT]
> **Tại sao `finally`?** Nếu exception xảy ra mà không unlock → deadlock (khóa vĩnh viễn, mọi thread sau đều chờ mãi).

**Code minh chứng — ScheduledExecutorService** ([AuctionManager.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/manager/AuctionManager.java#L28-L38)):
```java
long delayMs = Duration.between(LocalDateTime.now(), schema.getEndTime()).toMillis();
if (delayMs > 0) {
    scheduler.schedule(() -> endAuction(auction.getId()), delayMs, TimeUnit.MILLISECONDS);
    // Sau delayMs mili giây → tự động gọi endAuction()
}
```

---

## G. Bảng viết tắt

| Viết tắt | Đầy đủ | Tiếng Việt |
|----------|--------|------------|
| **DAO** | Data Access Object | Đối tượng truy xuất dữ liệu |
| **DTO** | Data Transfer Object | Đối tượng truyền tải dữ liệu |
| **MVC** | Model-View-Controller | Mô hình kiến trúc phần mềm |
| **CRUD** | Create-Read-Update-Delete | 4 thao tác cơ bản trên dữ liệu |
| **POJO** | Plain Old Java Object | Đối tượng Java thuần (chỉ có field + getter/setter) |
| **TCP** | Transmission Control Protocol | Giao thức truyền tin cậy |
| **UUID** | Universally Unique Identifier | Chuỗi định danh duy nhất (VD: `550e8400-...`) |
| **CI/CD** | Continuous Integration / Delivery | Tự động build + test khi push code |
| **E2E** | End-to-End | Kiểm thử xuyên suốt (Client → Server → DB → quay lại) |
| **MVP** | Minimum Viable Product | Sản phẩm tối thiểu hoạt động được |
| **FXML** | FX Markup Language | Ngôn ngữ XML mô tả giao diện JavaFX |

---

## H. Kịch bản trình bày 8 phút

| Phần | Thời gian | Nội dung | Ai nói |
|:----:|:---------:|----------|:------:|
| 1 | 1 phút | **Giới thiệu dự án:** "Hệ thống đấu giá trực tuyến, kiến trúc Client-Server, 4 thành viên, 4 tuần phát triển" | Team Lead |
| 2 | 2 phút | **Kiến trúc hệ thống:** Chỉ vào sơ đồ UML README, giải thích 4 layer, luồng dữ liệu Login | Khánh/Cường |
| 3 | 2 phút | **Demo chạy thật:** Chạy Server → Chạy Client → Register → Login → Tạo Item → Tạo Auction → Start → Xem danh sách | Công |
| 4 | 2 phút | **Mở code giải thích:** Design Patterns (chỉ file cụ thể), Concurrency (chỉ ReentrantLock trong LiveAuction) | Anh/Cường |
| 5 | 1 phút | **Tiến độ + Kế hoạch:** 4 tuần hoàn thành, tuần 5 sẽ nâng cấp (Bidding realtime, UI đẹp hơn) | Team Lead |

> [!TIP]
> **Mẹo:** Demo chạy thật **ấn tượng hơn rất nhiều** so với chỉ nói lý thuyết. Chuẩn bị sẵn Server đang chạy trước khi vào phòng.

---

## I. Ngân hàng câu hỏi khó & gợi ý trả lời

### 🎯 Mảng 1: Khả năng áp dụng lý thuyết OOP

---

**Q1: "Dự án áp dụng 4 tính chất OOP ở đâu?"**

| Tính chất | Ở đâu | Giải thích |
|-----------|-------|------------|
| **Tính đóng gói (Encapsulation)** | `User`: thuộc tính `private final`, truy cập qua getter | Dữ liệu được bảo vệ, không ai sửa trực tiếp |
| **Tính kế thừa (Inheritance)** | `User` ← `Bidder`, `Seller`, `Admin`. `BaseSchema` ← `UserSchema`, `ItemSchema`... | Tái sử dụng code chung (id, timestamps) |
| **Tính đa hình (Polymorphism)** | `user.hasPermission("CREATE_ITEM")` → mỗi subclass trả kết quả khác nhau | Cùng method, hành vi khác nhau tùy đối tượng thực tế |
| **Tính trừu tượng (Abstraction)** | `GenericDAO<T>` (interface), `User` (abstract class) | Ẩn chi tiết, chỉ lộ ra "hợp đồng" cần tuân thủ |

---

**Q2: "Tại sao User là abstract class mà không phải interface?"**

> Vì `User` có thuộc tính chung (`id`, `username`, `role`) và constructor — interface không có được. Đồng thời `User` bắt buộc các class con phải override `hasPermission()` và `getDisplayInfo()` — đây là phần abstract.

---

**Q3: "Generics `<T extends BaseSchema>` trong GenericDAO nghĩa là gì?"**

> `GenericDAO<T>` là interface dùng **Type Parameter** (kiểu tổng quát). `T extends BaseSchema` nghĩa là T phải là `BaseSchema` hoặc class con của nó. Nhờ vậy:
> - `GenericDAO<UserSchema>` → save/find trả về `UserSchema`
> - `GenericDAO<ItemSchema>` → save/find trả về `ItemSchema`
> - Compile-time type safety: không thể nhầm lẫn kiểu

---

**Q4: "Switch expression trong Java 21 khác gì switch statement truyền thống?"**

> Switch expression (dùng `->`) trong code dự án:
> ```java
> return switch (schema.getRole()) {
>     case BIDDER -> new Bidder(id, username);
>     case SELLER -> new Seller(id, username);
>     case ADMIN  -> new Admin(id, username);
> };
> ```
> - Không cần `break` → tránh bug fall-through
> - Trả về giá trị (có thể gán vào biến)
> - Compiler kiểm tra exhaustiveness — nếu thiếu case cho enum → lỗi compile

---

**Q5: "Design Pattern nào được áp dụng? Giải thích tại sao chọn pattern đó?"**

> *(Xem mục E ở trên để trả lời chi tiết cho từng pattern)*
>
> Tóm tắt: **7 patterns** — Singleton (quản lý 1 instance), Observer (realtime notify), Factory Method (tạo đúng subclass), Template Method (khung chung BaseSchema), DAO (tách persitence), MVC (client), DTO (dữ liệu an toàn qua mạng).

---

**Q6: "Tại sao dùng `ReentrantLock` mà không dùng `synchronized`?"**

> `ReentrantLock` linh hoạt hơn `synchronized`:
> - Hỗ trợ `tryLock()` — thử khóa, nếu không được thì bỏ qua (tránh block vĩnh viễn)
> - Hỗ trợ `Condition` variables — chờ điều kiện cụ thể
> - Explicit lock/unlock — rõ ràng hơn, dễ debug hơn
> - Có thể unlock ở method khác (synchronized không được)
>
> Trong dự án, `LiveAuction.placeBid()` là critical section phức tạp: validate → update → notify → return. `ReentrantLock` cho phép kiểm soát chặt chẽ hơn.

---

**Q7: "ConcurrentHashMap khác HashMap thường ở điểm nào?"**

> - `HashMap`: Không thread-safe. Nếu 2 thread cùng put → có thể corrupt data, infinite loop.
> - `ConcurrentHashMap`: Thread-safe nhờ **segment locking** (Java 8+ dùng CAS + synchronized per-bucket). Nhiều thread đọc/ghi đồng thời mà không block nhau ở các key khác nhau.
> - Trong dự án: `SessionManager.tokenMap` dùng `ConcurrentHashMap` vì nhiều `ClientHandler` (nhiều thread) cùng gọi `createSession()`, `validateToken()` đồng thời.

---

### 🎯 Mảng 2: Tiến độ hiện tại

---

**Q8: "Dự án hiện tại đã hoàn thành được những gì?"**

> **4 tuần phát triển, 2 Feature chính:**
>
> | Feature | Trạng thái | Chi tiết |
> |---------|:----------:|---------|  
> | **Auth (Tuần 2-3)** | ✅ Hoàn thành | Register, Login, Logout, Session Management, Password hashing |
> | **Auction (Tuần 4)** | ✅ Hoàn thành | Tạo Item (3 loại), Tạo Auction, Start Auction, Xem danh sách, Auto-end scheduled |
>
> **Infrastructure:** Maven multi-module, CI/CD (GitHub Actions), Integration Tests, JSON Database, TCP Socket protocol.

---

**Q9: "Tại sao dùng JSON file mà không dùng Database?"**

> - Dự án ưu tiên **đơn giản, không phụ thuộc phần mềm ngoài** (không cần cài MySQL/PostgreSQL)
> - JSON file dễ debug: mở file → thấy data ngay
> - Kiến trúc DAO đã **trừu tượng hóa** hoàn toàn → nếu cần chuyển sang DB thật, chỉ viết lại implementation DAO, **không sửa Service/Controller**
> - Với quy mô BTL (~vài trăm records), JSON file hoàn toàn đủ performance

---

**Q10: "Nêu một luồng dữ liệu end-to-end, ví dụ Login?"**

> 1. User nhập username/password → `LoginController.handleLogin()` validate trên client
> 2. Tạo **Background Thread** → `AuthClient.login()` → đóng gói thành `Request(LOGIN, {username, password})`
> 3. `ServerConnection.sendRequest()` → serialize JSON → gửi qua **TCP Socket**
> 4. Server: `ClientHandler.run()` → `readLine()` → deserialize → `RequestRouter.route()`
> 5. Router: `switch(LOGIN)` → `AuthController.handleLogin()`
> 6. `AuthService.login()` → `UserDAO.findByUsername()` → đọc `users.json` → tìm user
> 7. `PasswordUtils.verify()` kiểm tra password (SHA-256 + salt)
> 8. Đúng → `UserMapper.toDomain(schema)` → `SessionManager.createSession(user)` → tạo token UUID
> 9. `AuthController` → `UserMapper.toDTO(user)` → `Response.ok({token, userDTO})`
> 10. Response → serialize JSON → gửi TCP → Client
> 11. Client: `Platform.runLater()` → parse token + UserDTO → `ClientSession.login()` → `SceneManager.switchScene("Dashboard")`

---

### 🎯 Mảng 3: Kế hoạch hoàn thiện

---

**Q11: "Dự án còn thiếu gì? Kế hoạch tuần 5 trở đi?"**

> | Tuần | Kế hoạch | Chi tiết |
> |------|----------|---------|
> | **5** | **Place Bid + Realtime** | Bidder đặt giá → Server broadcast cho tất cả client đang xem (dùng Observer + push notification qua socket) |
> | **5** | **Auction Detail** | Xem chi tiết phiên: giá hiện tại, lịch sử bid, countdown timer |
> | **6** | **Hoàn thiện + Polish** | UI/UX cải thiện, error handling toàn diện, thêm test, documentation |
> | **6** | **Tính năng phụ** | Profile user, lịch sử tham gia, thông báo kết quả |

---

**Q12: "Realtime bidding sẽ implement thế nào?"**

> - Hiện tại dùng **request-response** (Client hỏi → Server trả lời)
> - Phiên bản realtime: Server giữ reference đến tất cả `ClientHandler` đang xem phiên
> - Khi có bid mới → `AuctionObserver.onBidPlaced()` → Server **push** `Response.push("NEW_BID", bidDTO)` cho tất cả client
> - Client lắng nghe trên thread riêng, khi nhận push → `Platform.runLater()` cập nhật UI
> - Cơ chế đã chuẩn bị sẵn: `AuctionObserver` interface + `LiveAuction.addObserver()` + `Response.push()`

---

### 🎯 Mảng 4: Khó khăn gặp phải

---

**Q13: "Khó khăn lớn nhất trong dự án là gì?"**

> 1. **Gson deserialize abstract class:** `ItemSchema` là abstract → Gson không biết tạo `ElectronicsSchema` hay `ArtSchema`. Giải quyết bằng `RuntimeTypeAdapterFactory` — đăng ký subtype mappings.
>
> 2. **Thread synchronization:** Ban đầu `LiveAuction.placeBid()` không có lock → 2 thread đặt giá đồng thời bị race condition. Fix bằng `ReentrantLock`.
>
> 3. **JavaFX threading rule:** Cập nhật UI từ Background Thread → crash. Phải dùng `Platform.runLater()` cho mọi UI update.
>
> 4. **Merge conflict:** 4 người code song song trên 4 branch → merge vào `develop` gây conflict, đặc biệt ở `RequestRouter` (ai cũng thêm case mới). Giải quyết bằng code review + quy ước vùng code.

---

**Q14: "RuntimeTypeAdapterFactory giải quyết vấn đề gì?"**

> Gson mặc định deserialize theo **class bạn khai báo**. Nếu khai báo `List<ItemSchema>` → Gson tạo `ItemSchema` → nhưng `ItemSchema` là abstract → **crash**.
>
> `RuntimeTypeAdapterFactory` cho phép đăng ký:
> ```
> Nếu JSON có "type": "ELECTRONICS" → tạo ElectronicsSchema
> Nếu JSON có "type": "ART"         → tạo ArtSchema
> Nếu JSON có "type": "VEHICLE"     → tạo VehicleSchema
> ```
> Nó đọc trường `type` trong JSON và tự chọn đúng class con để khởi tạo.

---

**Q15: "Tại sao Client có cả FakeServer?"**

> `FakeServer` ([FakeServer.java](file:///d:/Project/AuctionUET/auction-client/src/main/java/com/auctionuet/client/FakeServer.java)) tồn tại vì nhóm phát triển **song song**: Công (client GUI) và Khánh (server) code cùng lúc.
>
> Công cần test giao diện nhưng Server chưa xong → tạo FakeServer giả lập response → client chạy được mà không cần chờ server hoàn thiện. Đây là áp dụng **Dependency Inversion** — client không phụ thuộc server cụ thể.
>
> Trong `LoginController`, code xử lý cả 2 trường hợp (mock data vs server thật) để tương thích.

---

**Q16: "Tại sao `getBidHistory()` trả về `new ArrayList<>(bidHistory)` thay vì trả thẳng `bidHistory`?"**

> Đây là **Defensive Copy** — trả về bản sao thay vì bản gốc. Nếu trả bản gốc, bên ngoài có thể `.add()`, `.remove()` trực tiếp → phá vỡ tính nhất quán dữ liệu trong LiveAuction. Trả bản sao → bên ngoài sửa bao nhiêu cũng không ảnh hưởng bản gốc.

---

**Q17: "Có viết Unit Test không? Test những gì?"**

> Có. `AuthIntegrationTest` test E2E qua socket thật:
> - Register user mới → Login → nhận token → Logout  
> - Duplicate username → trả lỗi
> - Login sai password → trả lỗi
> - User không tồn tại → trả lỗi
> - Invalid JSON → trả lỗi
> - Multiple clients đồng thời
> - PING request

---

**Q18: "Explains `sendMessage` được khai báo `synchronized` — tại sao?"**

> ```java
> public synchronized void sendMessage(Response response) {
>     String json = MessageSerializer.serialize(response);
>     out.println(json);
> }
> ```
> Vì `PrintWriter.println()` không phải atomic operation. Nếu 2 thread cùng gọi `sendMessage()` đồng thời (VD: 1 thread xử lý request thường + 1 thread push notification), 2 chuỗi JSON có thể bị **xen kẽ ký tự** → Client nhận được JSON hỏng. `synchronized` đảm bảo mỗi lần chỉ 1 thread ghi vào output stream.

---

## J. Câu hỏi riêng cho từng thành viên

### 👤 Cường (Persistence + Service + Manager)

**Q: "UserDAO hoạt động thế nào? Giải thích method save()."**
> Đọc toàn bộ `users.json` ra List → add entity mới → ghi đè lại file. Đơn giản nhưng chấp nhận được cho BTL (không cần ACID transaction).

**Q: "AuctionManager dùng ScheduledExecutorService để làm gì?"**
> Khi phiên auction bắt đầu (`startAuction`), tính thời gian còn lại = `endTime - now()` → `scheduler.schedule(endAuction, delay)`. Sau đúng `delay` ms → tự động gọi `endAuction()`, set status FINISHED, notify observers. Không cần thread liên tục kiểm tra.

**Q: "Nếu server crash giữa chừng, auction data có mất không?"**
> `AuctionSchema` đã được lưu vào JSON file khi `createAuction()` và `startAuction()`. Tuy nhiên, `LiveAuction` (bid history trong RAM) sẽ mất. Đây là trade-off hiện tại, giải pháp: persist bid vào `bids.json` mỗi khi `placeBid()` thành công.

---

### 👤 Khánh (Network + Controller + Router)

**Q: "RequestRouter.route() hoạt động thế nào?"**
> Switch-case theo `request.getAction()` (ActionType enum): LOGIN → `authController.handleLogin()`, CREATE_ITEM → `itemController.handleCreateItem()`... Mỗi case delegate cho đúng Controller. Unknown action → `Response.error()`.

**Q: "ClientHandler là gì? Tại sao implement Runnable?"**
> Mỗi client kết nối tạo 1 `ClientHandler`. Implement `Runnable` để chạy trên **thread riêng** (`new Thread(handler).start()`). Method `run()` chứa vòng lặp `while`: đọc JSON → deserialize → route → trả response. Nhờ vậy nhiều client xử lý **đồng thời**, không block nhau.

**Q: "Tại sao AuthController bắt exception rồi trả Response.error() thay vì throw?"**
> Vì Controller là **ranh giới cuối cùng** trước khi gửi response về client. Nếu throw exception → `ClientHandler` crash → mất kết nối. Bắt exception → chuyển thành `Response.error("message")` → client nhận lỗi rõ ràng, kết nối vẫn giữ nguyên.

---

### 👤 Công (Client GUI + JavaFX)

**Q: "Tại sao handleLogin() tạo Thread mới?"**
> JavaFX có quy tắc: **UI Thread (JavaFX Application Thread) không được block**. Gọi `ServerConnection.sendRequest()` là blocking I/O (chờ server trả lời). Nếu gọi trên UI Thread → giao diện **đông cứng** (không bấm được gì, spinner không quay). Tạo Background Thread → I/O chạy nền → UI vẫn mượt.

**Q: "Platform.runLater() dùng để làm gì?"**
> JavaFX yêu cầu mọi thao tác UI (setText, setVisible, switchScene...) phải chạy trên **JavaFX Application Thread**. Nhưng response trả về trên Background Thread. `Platform.runLater(() -> {...})` = "đưa đoạn code này về UI Thread để thực thi". Không dùng → crash exception.

**Q: "SceneManager hoạt động thế nào?"**
> Singleton giữ reference đến `primaryStage`. Khi gọi `switchScene("/fxml/DashboardView.fxml")` → load FXML → tạo Scene mới → `stage.setScene(newScene)`. Tất cả Controller đều gọi `SceneManager.getInstance().switchScene()` để chuyển trang.

**Q: "Mock data trong LoginController có ý nghĩa gì?"**
> Khi Server thật chưa code xong, dùng FakeServer → response.data là string "OK" chứ không phải JSON object. Code kiểm tra: nếu data không phải JSON ({...}) → tự tạo UserDTO giả → vẫn chuyển sang Dashboard được. Khi server thật hoạt động → code đi nhánh else, parse token + UserDTO thật.

---

### 👤 Anh (Domain + Mapper + Testing)

**Q: "Tại sao Item là class phẳng (không kế thừa) còn ItemSchema lại có subclass?"**
> `ItemSchema` cần subclass (`ElectronicsSchema`, `ArtSchema`, `VehicleSchema`) vì mỗi loại có **thuộc tính riêng** để lưu trữ (brand, artist, make...). Nhưng trong Domain layer, `Item` chỉ cần thuộc tính chung (name, price, type) + `ItemType` enum để phân biệt. Logic đặc thù theo loại nằm ở `ItemMapper` (Factory Method).

**Q: "Tại sao dùng `final` cho thuộc tính trong User, Item, BidRecord?"**
> Immutable object — không thể sửa sau khi tạo. Lợi ích:
> - **Thread-safe miễn phí**: không sợ bị sửa từ thread khác
> - **Dễ suy luận**: data không tự thay đổi → ít bug tinh vi
> - **Defensive**: không ai vô tình `.setUsername()` → phá vỡ logic

**Q: "AuctionObserver interface có 2 method, giải thích?"**
> - `onBidPlaced(BidRecord)`: Gọi mỗi khi có bid mới thành công. Dùng để push cập nhật giá cho các client đang xem.
> - `onAuctionEnded(auctionId, winnerId, finalPrice)`: Gọi khi phiên kết thúc. Dùng để thông báo ai thắng, giá cuối.
>
> Bất kỳ class nào implement interface này → đăng ký với LiveAuction → tự động nhận thông báo. Đây là **Open-Closed Principle**: thêm listener mới mà không sửa LiveAuction.

**Q: "ItemMapper.fromRequestData() xử lý kiểu dữ liệu an toàn thế nào?"**
> Client gửi data dưới dạng `Map<String, Object>` → Gson parse JSON number thành `Double` (không phải `int`).
> - `safeInt()` helper: kiểm tra null → kiểm tra instanceof Number → ép kiểu an toàn → catch NumberFormatException
> - startingPrice: kiểm tra `instanceof Number` hoặc `instanceof String` → parse linh hoạt
> - type: parse enum `ItemType.valueOf(typeStr)` → bắt `IllegalArgumentException` nếu sai

---

## K. Checklist trước buổi vấn đáp

- [ ] Server đã chạy được (`mvn exec:java` hoặc run `ServerApp.main()`)
- [ ] Client đã chạy được (`mvn javafx:run` hoặc run `ClientApp.main()`)
- [ ] Demo path hoạt động: Register → Login → Create Item → Create Auction → Start → Xem danh sách
- [ ] Mở sẵn README trên GitHub để chỉ sơ đồ UML
- [ ] Mỗi người biết trả lời ít nhất 3-4 câu ở mục J
- [ ] Biết giải thích ít nhất 2 Design Patterns (Singleton + 1 cái nữa)
- [ ] Biết giải thích luồng Login end-to-end (câu Q10)
- [ ] Code đã push lên GitHub trước 23:59 ngày 20/4
