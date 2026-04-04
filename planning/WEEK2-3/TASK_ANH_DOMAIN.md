# 🅳️ NHIỆM VỤ CHI TIẾT — ANH (Domain Model + Mapper + Utils + Integration Test)

> **Tuần:** 2–3 · **Branch tuần 2:** `feature/tuan-2-anh-domain` · **Branch tuần 3:** `feature/tuan-3-anh-integration`
>
> **Tài liệu tham chiếu bắt buộc đọc trước khi code:**
> - `docs/server_architecture_overhaul.md` — Mục 4.2 (Domain), Mục 4.3 (Mapper)
> - `docs/diagrams_class_hierarchy.md` — Mục 3 (User hierarchy), Mục 7 (Toàn cảnh 3 tầng)
> - `docs/diagrams_flow_auth.md` — TOÀN BỘ (hiểu data flow qua Mapper)
> - `planning/WEEK2-3/WEEK2_3_REVISED.md` — Contract 2

---

## MỤC LỤC

1. [PHẦN A — Hiểu vai trò của từng lớp Anh phải code](#phần-a--hiểu-vai-trò-của-từng-lớp-anh-phải-code)
2. [PHẦN B — Tuần 2: Chi tiết từng file phải viết](#phần-b--tuần-2-chi-tiết-từng-file-phải-viết)
3. [PHẦN C — Tuần 3: Integration Testing](#phần-c--tuần-3-integration-testing)
4. [PHẦN D — Test bắt buộc](#phần-d--test-bắt-buộc)
5. [PHẦN E — Dependency & Nhắc nhở phối hợp](#phần-e--dependency--nhắc-nhở-phối-hợp)
6. [PHẦN F — Checklist hoàn thành](#phần-f--checklist-hoàn-thành)

---

## PHẦN A — Hiểu vai trò của từng lớp Anh phải code

### A.1 Tổng quan: Anh phụ trách những gì?

Anh phụ trách 4 nhóm class nằm ở TẦM GIỮA của kiến trúc:

```
Tầng Persistence (Cường)     ← Schema (chứa password, metadata)
       ↕ chuyển đổi qua
  [ MAPPER — Anh viết ]      ← Cầu nối duy nhất giữa 2 tầng
       ↕
Tầng Domain (Anh viết)       ← Model chạy trong RAM (KHÔNG có password)
       ↕ chuyển đổi qua
  [ MAPPER — Anh viết ]
       ↕
Tầng Network (Khánh)         ← DTO gửi qua mạng (KHÔNG có password)
```

Anh còn viết:
- **Utilities** (IdGenerator, PasswordUtils, ValidationUtils) — dùng bởi tất cả tầng
- **Exceptions** (AuthenticationException, UserNotFoundException...) — dùng bởi tất cả tầng
- **Integration Tests** (tuần 3) — test xuyên suốt toàn bộ hệ thống

### A.2 Domain Model là gì? Tại sao cần tách khỏi Schema?

**Domain Model** là đối tượng "phiên bản sạch" chạy trong RAM của server. Nó đại diện cho 1 user **đang hoạt động** trong hệ thống.

**So sánh Schema vs Domain:**

| Đặc điểm | UserSchema (Cường) | User (Anh) |
|-----------|-------------------|------------|
| Chứa password | ✅ hashedPassword + salt | ❌ KHÔNG BAO GIỜ |
| Chứa metadata | ✅ createdAt, updatedAt | ❌ Không cần |
| Có setter | ✅ (cần để Gson gán giá trị) | ❌ Immutable (an toàn multi-thread) |
| Tồn tại ở đâu | File JSON | RAM server |
| Khi nào dùng | Đọc/ghi file | Xử lý logic nghiệp vụ |
| Thể hiện OOP | Encapsulation (getter/setter) | Inheritance + Polymorphism |

**Tại sao Domain Model phải immutable (không có setter)?**

Server xử lý nhiều client đồng thời (multi-thread). Nếu User có setter:
```java
// Thread A: user.setUsername("hacker");
// Thread B: String name = user.getUsername();  // → "hacker" ???
```
→ Data race! Immutable = mọi thread đọc cùng dữ liệu = an toàn.

### A.3 Cây kế thừa User — OOP Polymorphism chính

```
User (abstract)
  ├── Bidder     — người đặt giá
  ├── Seller     — người bán
  └── Admin      — quản trị viên
```

**Tại sao cần cây kế thừa?**

1. **Đề bài yêu cầu OOP Inheritance + Polymorphism** — đây là nơi thể hiện chính.
2. **Phân quyền:** Mỗi vai trò được phép làm những hành động khác nhau:
   - Bidder: được đặt giá, xem auction, xem lịch sử bid
   - Seller: được tạo auction, xem auction của mình
   - Admin: được quản lý user, xem tất cả
3. **`hasPermission()`** là method abstract — mỗi lớp con override với bảng quyền riêng. Khi Controller cần kiểm tra quyền:
   ```java
   if (user.hasPermission("PLACE_BID")) {
       // cho phép đặt giá
   } else {
       return Response.error("Bạn không có quyền đặt giá");
   }
   ```
   Controller KHÔNG cần biết user là Bidder, Seller hay Admin. Nó chỉ gọi `hasPermission()` → đa hình tự xử lý. Đây là **Polymorphism**.

### A.4 Mapper — Cầu nối duy nhất giữa các tầng

**Mapper** là class chứa các **static method** chuyển đổi giữa Schema ↔ Domain ↔ DTO.

**Tại sao cần Mapper?**
- Schema và Domain là 2 class hoàn toàn khác nhau. Cần "ai đó" biết cách chuyển giữa chúng.
- Mapper đặt trong package riêng (`mapper/`), KHÔNG nằm trong persistence hay domain → trung lập.

**`UserMapper.toDomain(UserSchema)` — Factory Method:**
Method này nhận UserSchema, nhìn vào trường `role`, rồi tạo đúng subclass:
- `role == BIDDER` → `new Bidder(id, username)`
- `role == SELLER` → `new Seller(id, username)`
- `role == ADMIN` → `new Admin(id, username)`

Đây chính là **Factory Method Pattern** — quyết định tạo class nào dựa trên input, ẩn logic tạo object khỏi caller.

### A.5 DTO — Dữ liệu gửi qua mạng

**DTO (Data Transfer Object)** là class "siêu nhẹ" chỉ chứa dữ liệu cần gửi cho Client. UserDTO chỉ có `id`, `username`, `role` — **TUYỆT ĐỐI không có password**.

DTO tồn tại trong package `network/dto/` vì nó thuộc "thế giới mạng". Domain Model không biết DTO tồn tại. DTO cũng immutable (chỉ getter).

### A.6 Utilities — Công cụ dùng chung

| Class | Vai trò | Ai dùng? |
|-------|---------|----------|
| `IdGenerator` | Sinh UUID cho entity mới | Mapper (khi tạo schema mới) |
| `PasswordUtils` | Hash password + salt, verify | AuthService (của Cường, tuần 3) |
| `ValidationUtils` | Validate email, password, username | AuthService (của Cường, tuần 3) |

### A.7 Exception classes — Lỗi nghiệp vụ

| Exception | Khi nào throw | Ai throw? |
|-----------|---------------|-----------|
| `AuthenticationException` | Password sai, token hết hạn | AuthService, SessionManager |
| `UserNotFoundException` | Username không tồn tại | AuthService |
| `DuplicateUserException` | Register username đã tồn tại | AuthService |

---

## PHẦN B — Tuần 2: Chi tiết từng file phải viết

### Package đích:
```
src/main/java/com/auctionuet/server/
├── domain/
│   ├── model/
│   │   ├── User.java          (abstract)
│   │   ├── Bidder.java
│   │   ├── Seller.java
│   │   └── Admin.java
│   └── enums/
│       └── UserRole.java
├── network/
│   └── dto/
│       └── UserDTO.java
├── mapper/
│   └── UserMapper.java
├── util/
│   ├── IdGenerator.java
│   ├── PasswordUtils.java
│   └── ValidationUtils.java
└── exception/
    ├── AuthenticationException.java
    ├── UserNotFoundException.java
    └── DuplicateUserException.java
```

---

### B.1 File: `UserRole.java`

**Đường dẫn:** `domain/enums/UserRole.java`

**Vai trò:** Enum định nghĩa 3 vai trò trong hệ thống. Được dùng bởi CẢ Schema, Domain lẫn DTO.

```
public enum UserRole {
    BIDDER,
    SELLER,
    ADMIN
}
```

> **Lưu ý:** Tên enum PHẢI viết hoa (convention Java) và PHẢI khớp với giá trị trong JSON. Khi Gson deserialize `"role":"BIDDER"` → tự động map thành `UserRole.BIDDER`.

---

### B.2 File: `User.java` (abstract)

**Đường dẫn:** `domain/model/User.java`

**Vai trò:** Lớp cha trừu tượng cho tất cả loại người dùng. Chứa thông tin TỐI THIỂU cần thiết cho logic nghiệp vụ. **KHÔNG có password.**

**Khai báo class:**
```
public abstract class User
```

**Trường dữ liệu (tất cả `private final` — immutable):**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `id` | `String` | ID liên kết với UserSchema.id |
| `username` | `String` | Tên hiển thị |
| `role` | `UserRole` | Vai trò |

**Tại sao `private final`?**
- `private`: Encapsulation — bên ngoài không truy cập trực tiếp
- `final`: Một khi gán giá trị trong constructor, không ai đổi được → immutable → thread-safe

**Constructor:**
```
protected User(String id, String username, UserRole role)
```
- `protected`: Chỉ lớp con gọi được (qua `super(...)`). Bên ngoài không `new User(...)` được (vì abstract).
- Gán trực tiếp cả 3 trường.

**Abstract methods (lớp con BẮT BUỘC override):**

| Method | Signature | Mô tả |
|--------|-----------|-------|
| `hasPermission` | `public abstract boolean hasPermission(String action)` | Kiểm tra quyền. Trả `true` nếu role này được phép thực hiện action |
| `getDisplayInfo` | `public abstract String getDisplayInfo()` | Trả chuỗi mô tả ngắn, dùng để log/debug |

**Concrete methods (getter — KHÔNG có setter):**

| Method | Return |
|--------|--------|
| `getId()` | `String` |
| `getUsername()` | `String` |
| `getRole()` | `UserRole` |

---

### B.3 File: `Bidder.java`

**Đường dẫn:** `domain/model/Bidder.java`

**Vai trò:** Người đặt giá. Được phép xem auction, đặt giá, xem lịch sử bid. KHÔNG được tạo auction, quản lý user.

**Khai báo class:**
```
public class Bidder extends User
```

**Constructor:**
```
public Bidder(String id, String username)
```
- Gọi `super(id, username, UserRole.BIDDER)` — role luôn là BIDDER, cố định.
- Caller KHÔNG cần truyền role (tránh nhầm lẫn).

**Override `hasPermission(String action)`:**

```java
@Override
public boolean hasPermission(String action) {
    return switch (action) {
        case "PLACE_BID"        -> true;
        case "VIEW_AUCTION"     -> true;
        case "VIEW_BID_HISTORY" -> true;
        case "GET_PROFILE"      -> true;
        case "UPDATE_PROFILE"   -> true;
        case "CREATE_AUCTION"   -> false;  // chỉ Seller mới được
        case "MANAGE_USERS"     -> false;  // chỉ Admin mới được
        default                 -> false;  // mặc định: không cho phép
    };
}
```

**Override `getDisplayInfo()`:**
```java
@Override
public String getDisplayInfo() {
    return "Bidder: " + getUsername();
}
```

---

### B.4 File: `Seller.java`

**Đường dẫn:** `domain/model/Seller.java`

**Khai báo class:**
```
public class Seller extends User
```

**Constructor:**
```
public Seller(String id, String username)
```
Gọi `super(id, username, UserRole.SELLER)`.

**Override `hasPermission(String action)`:**

| Action | Cho phép? | Lý do |
|--------|----------|-------|
| `VIEW_AUCTION` | ✅ | Xem auction của mình |
| `CREATE_AUCTION` | ✅ | Tạo phiên đấu giá |
| `GET_PROFILE` | ✅ | Xem profile |
| `UPDATE_PROFILE` | ✅ | Sửa profile |
| `PLACE_BID` | ❌ | Người bán KHÔNG được tự đặt giá |
| `MANAGE_USERS` | ❌ | Chỉ Admin |
| default | ❌ | — |

**Override `getDisplayInfo()`:**
```
"Seller: " + getUsername()
```

---

### B.5 File: `Admin.java`

**Đường dẫn:** `domain/model/Admin.java`

**Khai báo class:**
```
public class Admin extends User
```

**Constructor:**
```
public Admin(String id, String username)
```
Gọi `super(id, username, UserRole.ADMIN)`.

**Override `hasPermission(String action)`:**

| Action | Cho phép? | Lý do |
|--------|----------|-------|
| `MANAGE_USERS` | ✅ | Quản lý user (xem, xóa, đổi role) |
| `VIEW_AUCTION` | ✅ | Xem tất cả auction |
| `GET_PROFILE` | ✅ | Xem profile |
| `GET_ALL_USERS` | ✅ | Xem danh sách user |
| `DELETE_USER` | ✅ | Xóa user |
| `UPDATE_ROLE` | ✅ | Đổi role user |
| `PLACE_BID` | ❌ | Admin không tham gia đấu giá |
| `CREATE_AUCTION` | ❌ | Admin không bán hàng |
| default | ❌ | — |

---

### B.6 File: `UserDTO.java`

**Đường dẫn:** `network/dto/UserDTO.java`

**Vai trò:** Dữ liệu user gửi qua mạng cho Client. **TUYỆT ĐỐI KHÔNG có password.**

**Khai báo class:**
```
public class UserDTO
```

**Trường dữ liệu (tất cả `private final`):**

| Trường | Kiểu |
|--------|------|
| `id` | `String` |
| `username` | `String` |
| `role` | `UserRole` |

**Constructor:**
```
public UserDTO(String id, String username, UserRole role)
```

**Methods:** Chỉ getter. Không setter. Immutable.

---

### B.7 File: `UserMapper.java`

**Đường dẫn:** `mapper/UserMapper.java`

**Vai trò:** Nơi DUY NHẤT biết cách chuyển đổi giữa UserSchema ↔ User ↔ UserDTO. Tất cả method đều **static**.

**Khai báo class:**
```
public class UserMapper
```

**Private constructor:** `private UserMapper() {}` — ngăn khởi tạo instance (utility class).

**3 static methods:**

---

#### Method 1: `toDomain` — Schema → Domain (Factory Method)

```
public static User toDomain(UserSchema schema)
```

**Đầu vào:** `UserSchema` (từ DB, chứa password)

**Đầu ra:** `User` (Bidder/Seller/Admin, KHÔNG có password)

**Logic:**
```java
public static User toDomain(UserSchema schema) {
    String id = schema.getId();
    String username = schema.getUsername();
    
    return switch (schema.getRole()) {
        case BIDDER -> new Bidder(id, username);
        case SELLER -> new Seller(id, username);
        case ADMIN  -> new Admin(id, username);
    };
}
```

> **Đây là Factory Method:** Caller (AuthService) gọi `UserMapper.toDomain(schema)` và nhận lại đúng subclass mà KHÔNG cần biết bên trong switch gì. Nếu sau này thêm role mới (ví dụ `MODERATOR`), chỉ cần sửa ở đây.

**Password xảy ra điều gì?** Nó bị **BỎ QUA** hoàn toàn. `toDomain` chỉ lấy `id`, `username`, `role` — 3 trường cần thiết cho Domain. Password hash, salt, createdAt, updatedAt — tất cả bị bỏ. Sau khi method return, nếu không có biến nào khác tham chiếu `schema`, Java GC sẽ thu hồi nó = password hash biến mất khỏi RAM.

---

#### Method 2: `toDTO` — Domain → DTO

```
public static UserDTO toDTO(User user)
```

**Logic:**
```java
public static UserDTO toDTO(User user) {
    return new UserDTO(user.getId(), user.getUsername(), user.getRole());
}
```

Đơn giản vì User và UserDTO có cùng 3 trường. Chỉ copy giá trị sang object mới.

---

#### Method 3: `toNewSchema` — Tạo Schema mới khi register

```
public static UserSchema toNewSchema(String username, String hashedPassword, 
                                      String salt, String email, UserRole role)
```

**Logic:**
```java
public static UserSchema toNewSchema(String username, String hashedPassword,
                                      String salt, String email, UserRole role) {
    String id = IdGenerator.generate();          // sinh UUID mới
    LocalDateTime now = LocalDateTime.now();      // thời gian hiện tại
    return new UserSchema(id, now, now, username, hashedPassword, salt, email, role);
    //                    ^^  ^^  ^^  → createdAt = updatedAt = now
}
```

> **`IdGenerator.generate()` thay vì `UUID.randomUUID()` trực tiếp:** Để sau này nếu cần thay đổi cách sinh ID (ví dụ: ID tuần tự, ID có prefix), chỉ sửa 1 chỗ.

---

### B.8 File: `IdGenerator.java`

**Đường dẫn:** `util/IdGenerator.java`

**Vai trò:** Sinh UUID tập trung. Mọi nơi cần ID mới đều gọi class này.

```java
public class IdGenerator {
    private IdGenerator() {}  // utility class
    
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
```

---

### B.9 File: `PasswordUtils.java`

**Đường dẫn:** `util/PasswordUtils.java`

**Vai trò:** Hash password bằng SHA-256 + salt. Class quan trọng nhất cho bảo mật.

**Methods bắt buộc:**

#### Method 1: `generateSalt`
```
public static String generateSalt()
```
- Sinh chuỗi salt ngẫu nhiên (16 bytes → hex string 32 ký tự)
- Dùng `SecureRandom` (KHÔNG dùng `Math.random()` — không đủ entropy cho bảo mật)
- Chuyển bytes → hex string bằng vòng lặp hoặc `HexFormat` (Java 17+)

#### Method 2: `hash`
```
public static String hash(String password, String salt)
```
- Nối `password + salt` → hash bằng SHA-256
- Dùng `MessageDigest.getInstance("SHA-256")`
- Chuyển hash bytes → hex string (64 ký tự)
- **Return** hex string

**Tại sao cần salt?** Nếu 2 user cùng password `"abc123"`, không có salt thì hash giống nhau → hacker biết ngay. Có salt (khác nhau cho mỗi user) → hash khác nhau → an toàn.

#### Method 3: `verify`
```
public static boolean verify(String password, String salt, String hashedPassword)
```
1. Gọi `hash(password, salt)` → tạm gọi là `computedHash`
2. So sánh `computedHash.equals(hashedPassword)` → trả `true/false`

> **Lưu ý bảo mật nâng cao (tuỳ chọn):** Dùng `MessageDigest.isEqual()` thay vì `String.equals()` để tránh timing attack. Nhưng cho scope dự án này, `equals()` là đủ.

---

### B.10 File: `ValidationUtils.java`

**Đường dẫn:** `util/ValidationUtils.java`

**Vai trò:** Validate input từ user. Throw `IllegalArgumentException` nếu input không hợp lệ.

**Methods bắt buộc:**

| Method | Signature | Rule | Message khi lỗi |
|--------|-----------|------|-----------------|
| `validateUsername` | `static void validateUsername(String username)` | Không null/empty, ≥ 3 ký tự, chỉ chữ + số + underscore | `"Username phải có ít nhất 3 ký tự"` |
| `validatePassword` | `static void validatePassword(String password)` | Không null/empty, ≥ 8 ký tự | `"Password phải có ít nhất 8 ký tự"` |
| `validateEmail` | `static void validateEmail(String email)` | Không null/empty, chứa `@` và `.` | `"Email không hợp lệ"` |

**Cách validate email (đơn giản, đủ cho MVP):**
```java
public static void validateEmail(String email) {
    if (email == null || email.isBlank()) {
        throw new IllegalArgumentException("Email không được để trống");
    }
    if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
        throw new IllegalArgumentException("Email không hợp lệ");
    }
}
```

---

### B.11 Exception Classes

**Đường dẫn:** `exception/`

Tất cả 3 exception đều extend `RuntimeException` (unchecked exception — không bắt buộc khai báo `throws`).

#### `AuthenticationException.java`
```java
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
```

#### `UserNotFoundException.java`
```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super("User không tồn tại: " + username);
    }
}
```

#### `DuplicateUserException.java`
```java
public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String username) {
        super("Username đã tồn tại: " + username);
    }
}
```

---

## PHẦN C — Tuần 3: Integration Testing

### C.1 Vai trò Integration Test

Tuần 2, mỗi người test **đơn vị** (unit test): Cường test DAO, Khánh test Serializer, Anh test Mapper. Nhưng khi ghép lại → có thể lỗi ở "mối nối".

**Integration Test** kiểm tra toàn bộ luồng xuyên suốt:
```
Client gửi JSON → Socket → ClientHandler → Router → Controller → Service → DAO → JSON File
```
Nếu 1 mắt xích nào sai → test fail.

### C.2 File: `TestHelper.java`

**Đường dẫn:** `src/test/java/.../TestHelper.java`

**Vai trò:** Utility giúp khởi chạy server và gửi request trong test.

**Methods bắt buộc:**

| Method | Mô tả |
|--------|-------|
| `startTestServer(int port)` | Khởi chạy AuctionServer trên thread riêng, dùng port tuỳ chọn (tránh conflict) |
| `stopTestServer()` | Dừng server |
| `Response sendRawRequest(int port, String json)` | Mở socket → gửi json → đọc response → parse → trả Response |
| `void cleanTestData()` | Xóa file JSON test (`data/test_users.json`) |

**Chi tiết `sendRawRequest`:**
```java
public static Response sendRawRequest(int port, String json) throws Exception {
    try (Socket socket = new Socket("localhost", port)) {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        
        out.println(json);           // gửi request
        String response = in.readLine();  // đọc 1 dòng response
        return new Gson().fromJson(response, Response.class);
    }
}
```

> **Lưu ý:** Mỗi lần gọi `sendRawRequest` tạo 1 connection mới rồi đóng. Đủ cho test. Production thì client giữ connection suốt session.

---

### C.3 File: `AuthIntegrationTest.java`

**Đường dẫn:** `src/test/java/.../AuthIntegrationTest.java`

**Test scenarios bắt buộc (ít nhất 7 test cases):**

#### Test 1: `testFullAuthFlow`
**Mô tả:** Luồng hoàn chỉnh: Register → Login → Validate Token → Logout → Token hết hạn

**Bước-by-bước:**
1. Gửi REGISTER `{username: "testuser", password: "test12345", email: "test@uet.vn", role: "BIDDER"}`
   - Assert: `response.status == "OK"`
2. Gửi LOGIN `{username: "testuser", password: "test12345"}`
   - Assert: `response.status == "OK"`
   - Assert: `response.data` có `token` (not null/empty)
   - Assert: `response.data` có `user.username == "testuser"`
   - Assert: `response.data` có `user.role == "BIDDER"`
   - Assert: `response.data` **KHÔNG** có `password` hay `hashedPassword`
   - Lưu token lại
3. Gửi LOGOUT `{token: saved_token}`
   - Assert: `response.status == "OK"`

#### Test 2: `testRegisterDuplicateUsername`
1. Gửi REGISTER username "cuong" → OK
2. Gửi REGISTER username "cuong" lần 2 → ERROR, message chứa "đã tồn tại"

#### Test 3: `testLoginWrongPassword`
1. Gửi REGISTER username "cuong", password "correct123"
2. Gửi LOGIN username "cuong", password "wrongpass" → ERROR, message chứa "Sai mật khẩu"

#### Test 4: `testLoginNonExistentUser`
1. Gửi LOGIN username "ghost" (chưa register) → ERROR, message chứa "không tồn tại"

#### Test 5: `testInvalidRequest`
1. Gửi JSON rác: `"this is not json"` → Server trả ERROR, **KHÔNG CRASH**
2. Gửi JSON hợp lệ nhưng thiếu action: `{"data":{}}` → ERROR

#### Test 6: `testMultipleClientsLogin`
1. Client A: Register "userA" + Login → Token A
2. Client B: Register "userB" + Login → Token B
3. Assert: Token A ≠ Token B (2 token khác nhau)
4. Cả 2 token đều valid

#### Test 7: `testPingNoAuth`
1. Gửi PING `{action: "PING"}` (không cần token) → Response OK "PONG"

### C.4 Setup/Teardown

```java
@BeforeAll
static void setup() {
    TestHelper.startTestServer(9999);  // dùng port 9999 cho test
    Thread.sleep(500);  // chờ server start
}

@AfterAll
static void teardown() {
    TestHelper.stopTestServer();
    TestHelper.cleanTestData();
}

@BeforeEach
void cleanBefore() {
    TestHelper.cleanTestData();  // mỗi test bắt đầu với DB sạch
}
```

### C.5 Bug Report & Tổng hợp lỗi

Sau khi chạy toàn bộ test:
1. Ghi nhận test nào FAIL → xác định nguyên nhân:
   - Lỗi ở tầng nào? (Persistence / Domain / Network)
   - Ai phụ trách tầng đó?
2. Tạo GitHub Issue cho mỗi bug:
   - Title: `[BUG] Mô tả ngắn`
   - Body: Steps to reproduce, expected vs actual, stack trace
   - Label: `bug`, assign cho người phụ trách
3. Nếu bug đơn giản → Anh tự fix luôn (vì có code cả 3 tầng). Tạo fix PR.

---

## PHẦN D — Test bắt buộc

### D.1 Test tuần 2 (Unit Test)

**File: `UserModelTest.java`**

| # | Test name | Mô tả | Assert |
|---|-----------|-------|--------|
| 1 | `testBidderPermissions` | Bidder PLACE_BID=true, MANAGE_USERS=false | `assertTrue/assertFalse` |
| 2 | `testSellerPermissions` | Seller CREATE_AUCTION=true, PLACE_BID=false | `assertTrue/assertFalse` |
| 3 | `testAdminPermissions` | Admin MANAGE_USERS=true, PLACE_BID=false | `assertTrue/assertFalse` |
| 4 | `testUserImmutable` | User không có setter, fields là final | Compile-time: không có setter method |
| 5 | `testGetDisplayInfo` | Bidder → "Bidder: username" | `assertEquals` |

**File: `UserMapperTest.java`**

| # | Test name | Mô tả | Assert |
|---|-----------|-------|--------|
| 1 | `testToDomainBidder` | Schema(role=BIDDER) → Bidder instance | `assertInstanceOf(Bidder.class)` |
| 2 | `testToDomainSeller` | Schema(role=SELLER) → Seller instance | `assertInstanceOf(Seller.class)` |
| 3 | `testToDomainAdmin` | Schema(role=ADMIN) → Admin instance | `assertInstanceOf(Admin.class)` |
| 4 | `testToDTO` | User → UserDTO, giữ nguyên id/username/role | `assertEquals` |
| 5 | `testToDTONoPassword` | UserDTO không có method getPassword() | Compile-time check |
| 6 | `testToNewSchema` | Tạo schema mới có id + timestamps | `assertNotNull(schema.getId())` |

**File: `PasswordUtilsTest.java`**

| # | Test name | Mô tả | Assert |
|---|-----------|-------|--------|
| 1 | `testHashAndVerify` | hash rồi verify → true | `assertTrue` |
| 2 | `testWrongPassword` | hash "abc", verify "xyz" → false | `assertFalse` |
| 3 | `testDifferentSalt` | cùng password, khác salt → khác hash | `assertNotEquals` |
| 4 | `testSaltGeneration` | 2 lần generateSalt() → khác nhau | `assertNotEquals` |

**File: `ValidationUtilsTest.java`**

| # | Test name | Mô tả | Assert |
|---|-----------|-------|--------|
| 1 | `testValidEmail` | "user@uet.vn" → không throw | Không exception |
| 2 | `testInvalidEmail` | "not-email" → throw | `assertThrows` |
| 3 | `testWeakPassword` | "123" → throw | `assertThrows` |
| 4 | `testValidPassword` | "abc12345" → không throw | Không exception |
| 5 | `testShortUsername` | "ab" → throw | `assertThrows` |

### D.2 Test tuần 3 — Integration Test (mô tả ở phần C)

- Tối thiểu **7 test cases** pass
- Không có test flaky (chạy lại 5 lần đều pass)
- Test data cleanup tự động

---

## PHẦN E — Dependency & Nhắc nhở phối hợp

### E.1 Tuần 2: Anh HOÀN TOÀN KHÔNG phụ thuộc ai

Code tuần 2 của Anh không import gì từ code của Cường hay Khánh.

**Ngoại trừ:** `UserMapper.toDomain(UserSchema schema)` cần `UserSchema` (của Cường). Nhưng Anh có thể:
1. Import class `UserSchema` — nếu Cường đã push code
2. Hoặc tự tạo `UserSchema` mock class tạm để test Mapper

### E.2 Tuần 3: Anh phụ thuộc TẤT CẢ (Integration Test)

| Anh cần (tuần 3) | Ai cung cấp | Deadline |
|-------------------|------------|----------|
| `UserDAO` hoạt động | **Cường** (tuần 2) | Merge cuối tuần 2 |
| `AuthService`, `SessionManager` | **Cường** (tuần 3) | Thứ 4 tuần 3 |
| `AuctionServer`, `ClientHandler` | **Khánh** (tuần 2) | Merge cuối tuần 2 |
| `AuthController`, `RequestRouter` | **Khánh** (tuần 3) | Thứ 5 tuần 3 |
| GUI không cần — test bằng raw socket | — | — |

> [!WARNING]
> **NHẮC NHỞ CHO ANH:**
> - Anh sẽ bắt đầu integration test **MUỘN NHẤT** trong nhóm. Điều này là BY DESIGN — vì test E2E cần cả server chạy.
> - **Trong khi chờ**, Anh PHẢI:
>   1. Viết sẵn `TestHelper.java` (tuần 2, cuối tuần)
>   2. Viết sẵn test skeleton — tạo method, thêm comment mô tả step, để body trống
>   3. Test đầy đủ code tuần 2 của mình (unit test — không cần server)
> - **Thứ 5-6 tuần 3:** Nhắc TẤT CẢ mọi người:
>   - *"Cường ơi, AuthService merge chưa?"*
>   - *"Khánh ơi, server chạy được chưa?"*
>   - *"Mọi người merge develop đi, tôi cần test E2E!"*
> - Nếu server chưa chạy hết → Anh vẫn test được 1 phần (ví dụ: test Mapper, test PasswordUtils với AuthService — không cần Network).

### E.3 Vấn đề test port conflict

Khi chạy test integration, **KHÔNG dùng port 8888** (port production). Dùng port khác (9999, 12345...) để tránh conflict nếu ai đó đang chạy server thật trên máy.

### E.4 Tóm tắt timeline

```
Tuần 2:
  Anh: Hoàn thành User, Bidder, Seller, Admin, UserRole
       Hoàn thành UserDTO, UserMapper
       Hoàn thành IdGenerator, PasswordUtils, ValidationUtils
       Hoàn thành 3 Exception classes
       Unit test TẤT CẢ → pass
       Viết sẵn TestHelper + test skeleton

Tuần 3:
  Đầu tuần: Merge develop → lấy code Cường + Khánh tuần 2
  Thứ 2-3:  Test Mapper với UserSchema thật (của Cường)
            Test PasswordUtils với UserDAO thật
  Thứ 4-5:  Chờ Cường push AuthService → Chờ Khánh push Controller
            Khi có → bắt đầu integration test ngay
  Thứ 6-7:  Fix bugs, tổng hợp report, tạo GitHub Issues
            7 integration tests ALL PASS
```

---

## PHẦN F — Checklist hoàn thành

### Tuần 2 — Deadline: Tối thứ 7

- [ ] `UserRole.java` — Compile, 3 giá trị enum
- [ ] `User.java` — Compile, abstract, 3 trường final, 2 abstract methods
- [ ] `Bidder.java` — Compile, override hasPermission + getDisplayInfo
- [ ] `Seller.java` — Compile, override hasPermission + getDisplayInfo
- [ ] `Admin.java` — Compile, override hasPermission + getDisplayInfo
- [ ] `UserDTO.java` — Compile, immutable, 3 trường + getter
- [ ] `UserMapper.java` — Compile, 3 static methods hoạt động đúng
- [ ] `IdGenerator.java` — Compile, trả UUID string
- [ ] `PasswordUtils.java` — Compile, hash + verify hoạt động đúng
- [ ] `ValidationUtils.java` — Compile, validate 3 loại input
- [ ] `AuthenticationException.java` — Compile
- [ ] `UserNotFoundException.java` — Compile
- [ ] `DuplicateUserException.java` — Compile
- [ ] `UserModelTest.java` — **5 tests ALL PASS** ✅
- [ ] `UserMapperTest.java` — **6 tests ALL PASS** ✅
- [ ] `PasswordUtilsTest.java` — **4 tests ALL PASS** ✅
- [ ] `ValidationUtilsTest.java` — **5 tests ALL PASS** ✅
- [ ] Code push lên branch `feature/tuan-2-anh-domain`
- [ ] Tham gia meeting cuối tuần 2

### Tuần 3 — Deadline: Tối thứ 7

- [ ] `TestHelper.java` — Compile, 4 methods hoạt động
- [ ] `AuthIntegrationTest.java` — **7 tests ALL PASS** ✅
- [ ] Test chạy stable (5 lần liên tiếp không flaky)
- [ ] Test data cleanup tự động
- [ ] Bug report tổng hợp → GitHub Issues (nếu có)
- [ ] Code push lên branch `feature/tuan-3-anh-integration`
- [ ] Tham gia meeting cuối tuần 3, chạy test trước mặt nhóm

---

> **Ghi chú cuối:** Anh là "người kiểm soát chất lượng" cho cả nhóm ở tuần 3. Integration test pass = nhóm hoàn thành Auth MVP. Hãy test kỹ, test nhiều edge case, và báo bug sớm — đừng chờ đến tối thứ 7 mới chạy test lần đầu.
