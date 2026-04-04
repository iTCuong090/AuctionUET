# 🅰️ NHIỆM VỤ CHI TIẾT — CƯỜNG (Persistence Layer + Auth Service)

> **Tuần:** 2–3 · **Branch tuần 2:** `feature/tuan-2-cuong-persistence` · **Branch tuần 3:** `feature/tuan-3-cuong-auth-service`
>
> **Tài liệu tham chiếu bắt buộc đọc trước khi code:**
> - `docs/server_architecture_overhaul.md` — Mục 3 (Package), Mục 4.1 (Persistence)
> - `docs/diagrams_class_hierarchy.md` — Mục 1 (Schema), Mục 2 (DAO)
> - `docs/diagrams_flow_auth.md` — Luồng Login + Register
> - `planning/WEEK2-3/WEEK2_3_REVISED.md` — Contract 1

---

## MỤC LỤC

1. [PHẦN A — Hiểu vai trò Persistence Layer](#phần-a--hiểu-vai-trò-persistence-layer)
2. [PHẦN B — Tuần 2: Chi tiết từng file phải viết](#phần-b--tuần-2-chi-tiết-từng-file-phải-viết)
3. [PHẦN C — Tuần 3: AuthService + SessionManager](#phần-c--tuần-3-authservice--sessionmanager)
4. [PHẦN D — Test bắt buộc](#phần-d--test-bắt-buộc)
5. [PHẦN E — Dependency & Nhắc nhở phối hợp](#phần-e--dependency--nhắc-nhở-phối-hợp)
6. [PHẦN F — Checklist hoàn thành](#phần-f--checklist-hoàn-thành)

---

## PHẦN A — Hiểu vai trò Persistence Layer

### A.1 Persistence Layer là gì?

Persistence Layer (tầng lưu trữ) là tầng thấp nhất trong kiến trúc 3 tầng của server. Nó có **một và chỉ một nhiệm vụ**: đọc/ghi dữ liệu từ/vào nơi lưu trữ (trong dự án này là file JSON).

Hãy tưởng tượng server là một tòa nhà:
- **Tầng hầm (Persistence)** = Kho lưu trữ. Nó không biết tòa nhà dùng để làm gì, nó chỉ biết: "Cho tôi cái hộp đỏ → đây, hộp đỏ" hoặc "Cất cái hộp xanh này → done".
- **Tầng trệt (Domain)** = Phòng xử lý nghiệp vụ. Nó lấy hộp từ kho, mở ra, xử lý logic, rồi cất lại.
- **Tầng trên (Network)** = Quầy tiếp khách. Nó nhận yêu cầu từ khách hàng, chuyển xuống tầng trệt, rồi trả kết quả.

**Quy tắc vàng:** Persistence Layer **không bao giờ** chứa logic nghiệp vụ. Nó không biết "login" là gì, không biết "đấu giá" là gì. Nó chỉ biết: save, find, update, delete.

### A.2 Tại sao cần Schema riêng biệt với Domain Model?

Trong kiến trúc cũ, cùng một class `User` được dùng cho cả 3 mục đích:
1. Lưu vào file (chứa password hash)
2. Giữ trong RAM khi user đang online (chứa password hash — **nguy hiểm!**)
3. Gửi qua mạng cho client (chứa password hash — **CỰC KỲ NGUY HIỂM!**)

Kiến trúc mới tách thành 3 loại object:

| Loại | Class | Chứa password? | Tồn tại ở đâu? | Ai sở hữu? |
|------|-------|----------------|-----------------|-------------|
| **Schema** | `UserSchema` | ✅ Có (hash + salt) | File JSON | **Cường** |
| **Domain** | `User/Bidder/Seller/Admin` | ❌ Không | RAM server | Anh |
| **DTO** | `UserDTO` | ❌ Không | Gửi qua mạng | Anh |

**Schema** là "bản sao 1:1" của dữ liệu trong file JSON. Mỗi trường trong Schema tương ứng với 1 field trong JSON. Schema có cả những thứ nhạy cảm (password hash) và metadata (createdAt, updatedAt) mà Domain Model không cần.

### A.3 Sau khi làm Auth xong, Persistence Layer mở rộng như thế nào?

Đây là bức tranh toàn cảnh. Tuần 2-3 Cường chỉ cần làm `UserSchema` + `UserDAO`. Nhưng sau đó, **cùng một pattern sẽ được lặp lại** cho các loại dữ liệu khác:

```
Tuần 2-3 (Auth):
  persistence/
  ├── schema/
  │   ├── BaseSchema.java       ← Cường viết tuần 2
  │   └── UserSchema.java       ← Cường viết tuần 2
  ├── dao/
  │   ├── GenericDAO.java        ← Cường viết tuần 2
  │   └── UserDAO.java           ← Cường viết tuần 2
  └── json/
      ├── JsonFileHelper.java    ← Cường viết tuần 2
      └── GsonFactory.java       ← Cường viết tuần 2

Tuần 4-5 (Auction + Bidding — MỞ RỘNG):
  persistence/
  ├── schema/
  │   ├── BaseSchema.java        ← Đã có, không cần sửa
  │   ├── UserSchema.java        ← Đã có, không cần sửa
  │   ├── ItemSchema.java        ← MỚI: Thông tin sản phẩm
  │   ├── AuctionSchema.java     ← MỚI: Phiên đấu giá
  │   └── BidSchema.java         ← MỚI: Lệnh đặt giá
  ├── dao/
  │   ├── GenericDAO.java        ← Đã có, KHÔNG CẦN SỬA (vì dùng Generics)
  │   ├── UserDAO.java           ← Đã có
  │   ├── ItemDAO.java           ← MỚI: implements GenericDAO<ItemSchema>
  │   ├── AuctionDAO.java        ← MỚI: implements GenericDAO<AuctionSchema>
  │   └── BidDAO.java            ← MỚI: implements GenericDAO<BidSchema>
  └── json/
      ├── JsonFileHelper.java    ← Đã có, KHÔNG CẦN SỬA
      └── GsonFactory.java       ← Đã có, KHÔNG CẦN SỬA
```

**Điểm quan trọng:** Nếu Cường thiết kế `GenericDAO` và `JsonFileHelper` tốt ở tuần 2, thì tuần 4-5 chỉ cần **copy-paste pattern** để tạo thêm DAO mới. Đó là sức mạnh của Generics và abstraction.

### A.4 AuthService và SessionManager làm gì?

**`AuthService`** (tuần 3) là "bộ não" xử lý xác thực. Nó **sử dụng** Persistence Layer (UserDAO) để:
- **Login:** Tìm user trong DB → kiểm tra password → tạo session
- **Register:** Validate → kiểm tra trùng → hash password → lưu vào DB

Nó KHÔNG trực tiếp đọc/ghi file JSON. Nó gọi UserDAO để làm việc đó. Đây là Separation of Concerns.

**`SessionManager`** (tuần 3) là "bảo vệ cổng". Nó quản lý ai đang online:
- Khi user login thành công → tạo "token" (một chuỗi UUID ngẫu nhiên) → lưu cặp `{token → User}` vào RAM
- Khi user gửi request → kiểm tra token có hợp lệ không → trả về User tương ứng
- Khi user logout → xóa token khỏi RAM
- Token chỉ tồn tại trong RAM, **KHÔNG lưu xuống file**. Server restart = tất cả user phải login lại (đây là thiết kế có chủ đích cho MVP)

---

## PHẦN B — Tuần 2: Chi tiết từng file phải viết

### Package đích:
```
src/main/java/com/auctionuet/server/persistence/
├── schema/
│   ├── BaseSchema.java
│   └── UserSchema.java
├── dao/
│   ├── GenericDAO.java    (interface)
│   └── UserDAO.java       (implementation)
└── json/
    ├── JsonFileHelper.java
    └── GsonFactory.java
```

---

### B.1 File: `BaseSchema.java`

**Đường dẫn:** `persistence/schema/BaseSchema.java`

**Vai trò:** Lớp cha trừu tượng cho TẤT CẢ schema trong hệ thống. Chứa 3 trường chung mà mọi entity đều có: id, thời gian tạo, thời gian cập nhật. Tất cả `XxxSchema` khác sẽ kế thừa class này.

**Khai báo class:**
```
public abstract class BaseSchema
```

**Trường dữ liệu (tất cả `protected`):**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `id` | `String` | Định danh duy nhất (UUID). Mỗi entity có 1 id không trùng lặp |
| `createdAt` | `LocalDateTime` | Thời điểm entity được tạo lần đầu |
| `updatedAt` | `LocalDateTime` | Thời điểm cập nhật gần nhất |

> **Tại sao `protected` chứ không phải `private`?** Vì các lớp con (`UserSchema`, `ItemSchema`...) cần truy cập trực tiếp các trường này trong constructor khi gọi `super(...)`. Dùng `protected` cho phép lớp con truy cập nhưng ngăn code bên ngoài package.

**Constructor:**

1. **Constructor rỗng — `protected BaseSchema()`**
   - **KHÔNG LÀM GÌ** bên trong body. Không sinh UUID, không gán thời gian.
   - Lý do: Gson dùng Reflection để gọi constructor rỗng trước, rồi mới gán giá trị từ JSON. Nếu constructor tự sinh UUID → UUID bị ghi đè → lãng phí CPU và gây nhầm lẫn.
   - Đánh dấu `protected` để chỉ Gson (qua Reflection) và lớp con gọi được, code bên ngoài không gọi được.

2. **Constructor tường minh — `protected BaseSchema(String id, LocalDateTime createdAt, LocalDateTime updatedAt)`**
   - Gán trực tiếp 3 tham số vào 3 trường.
   - Dùng khi tạo entity mới từ code (ví dụ: khi register user mới).

**Methods bắt buộc:**

| Method | Mô tả |
|--------|-------|
| `getId()` | Getter cho id |
| `getCreatedAt()` | Getter cho createdAt |
| `getUpdatedAt()` | Getter cho updatedAt |
| `setId(String id)` | Setter cho id |
| `setCreatedAt(LocalDateTime)` | Setter cho createdAt |
| `setUpdatedAt(LocalDateTime)` | Setter cho updatedAt |
| `equals(Object o)` | So sánh theo `id`. Hai schema có cùng id = cùng entity |
| `hashCode()` | Tính từ `id` (dùng `Objects.hash(id)`) |

> **Tại sao có setter?** Schema là "data container" — nó cần setter để Gson gán giá trị và để code update được `updatedAt` trước khi ghi lại file.

---

### B.2 File: `UserSchema.java`

**Đường dẫn:** `persistence/schema/UserSchema.java`

**Vai trò:** Ánh xạ 1:1 với file `data/users.json`. Mỗi object trong JSON array tương ứng với 1 instance `UserSchema`.

**Khai báo class:**
```
public class UserSchema extends BaseSchema
```

**Trường dữ liệu (tất cả `private`):**

| Trường | Kiểu | Ý nghĩa | Ví dụ giá trị |
|--------|------|---------|---------------|
| `username` | `String` | Tên đăng nhập, unique | `"cuong123"` |
| `hashedPassword` | `String` | Mật khẩu đã hash bằng SHA-256 | `"a1b2c3d4..."` (64 ký tự hex) |
| `passwordSalt` | `String` | Chuỗi salt ngẫu nhiên dùng để hash | `"x9y8z7..."` |
| `email` | `String` | Email người dùng | `"cuong@uet.vn"` |
| `role` | `UserRole` | Vai trò (dùng enum của Anh) | `UserRole.BIDDER` |

> **Lưu ý tên trường `hashedPassword`:** Đặt tên nhấn mạnh đây là hash, KHÔNG phải plain text. Ai đọc code cũng hiểu ngay rằng đây không phải mật khẩu gốc.

**Constructor:**

1. **Constructor rỗng — `protected UserSchema()`**
   - Gọi `super()` (constructor rỗng của BaseSchema).
   - Cho Gson dùng.

2. **Constructor tường minh — nhận 8 tham số:**
   ```
   public UserSchema(String id, LocalDateTime createdAt, LocalDateTime updatedAt,
                     String username, String hashedPassword, String passwordSalt,
                     String email, UserRole role)
   ```
   - Gọi `super(id, createdAt, updatedAt)` → gán 3 trường kế thừa.
   - Gán 5 trường riêng.
   - Dùng khi tạo user mới (trong UserMapper.toNewSchema).

**Methods:** Getter + Setter cho tất cả 5 trường private.

**File JSON tương ứng (`data/users.json`) sẽ trông như thế này:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "createdAt": "2026-04-05T00:00:00",
    "updatedAt": "2026-04-05T00:00:00",
    "username": "cuong123",
    "hashedPassword": "a1b2c3d4e5f6...",
    "passwordSalt": "x9y8z7...",
    "email": "cuong@uet.vn",
    "role": "BIDDER"
  }
]
```

---

### B.3 File: `GsonFactory.java`

**Đường dẫn:** `persistence/json/GsonFactory.java`

**Vai trò:** Nơi DUY NHẤT tạo `Gson` instance cho toàn bộ server. Mọi chỗ cần serialize/deserialize JSON đều phải lấy Gson từ đây, không được `new Gson()` ở chỗ khác.

**Khai báo class:**
```
public class GsonFactory
```

**Tại sao cần factory riêng?** Vì `Gson` mặc định KHÔNG biết serialize/deserialize `LocalDateTime`. Cần đăng ký TypeAdapter tùy chỉnh. Nếu mỗi chỗ tự `new Gson()`, sẽ quên đăng ký TypeAdapter → crash.

**Methods bắt buộc:**

| Method | Signature | Mô tả |
|--------|-----------|-------|
| `create()` | `public static Gson create()` | Trả về Gson instance đã config đầy đủ |

**Bên trong `create()` phải làm gì:**

1. Tạo `GsonBuilder`
2. Đăng ký `TypeAdapter<LocalDateTime>`:
   - **Serialize:** `LocalDateTime` → `String` (dùng `DateTimeFormatter.ISO_LOCAL_DATE_TIME`)
   - **Deserialize:** `String` → `LocalDateTime` (dùng `LocalDateTime.parse()`)
3. Gọi `setPrettyPrinting()` — để file JSON dễ đọc khi debug
4. Gọi `create()` → trả về `Gson`

**Ví dụ kết quả serialize:**
```json
{
  "createdAt": "2026-04-05T00:00:00"
}
```
Thay vì (nếu không có TypeAdapter):
```json
{
  "createdAt": { "year": 2026, "month": 4, "day": 5 ... }  ← SẬP!
```

> **Mở rộng sau này:** Khi có `ItemSchema` với cây kế thừa (Electronics, Art, Vehicle), cần thêm `RuntimeTypeAdapterFactory` vào đây để Gson biết deserialize đúng subclass. Nhưng tuần 2 chưa cần.

---

### B.4 File: `JsonFileHelper.java`

**Đường dẫn:** `persistence/json/JsonFileHelper.java`

**Vai trò:** class tiện ích đọc/ghi file JSON. Đây là lớp "thấp nhất" — nó chỉ biết: đọc file → trả List, hoặc nhận List → ghi file. Không biết nội dung là gì.

**Khai báo class:**
```
public class JsonFileHelper
```

**Methods bắt buộc:**

#### Method 1: `readList`
```
public static <T> List<T> readList(String filePath, Class<T> clazz)
```

**Đầu vào:**
- `filePath`: Đường dẫn tới file JSON (ví dụ: `"data/users.json"`)
- `clazz`: Class cần deserialize thành (ví dụ: `UserSchema.class`)

**Đầu ra:**
- `List<T>`: Danh sách đối tượng đọc từ file

**Logic bên trong:**
1. Kiểm tra file có tồn tại không. Nếu KHÔNG → trả `new ArrayList<>()` (list rỗng). **KHÔNG throw exception.**
2. Đọc toàn bộ nội dung file bằng `Files.readString(Path)` hoặc `BufferedReader`.
3. Dùng `GsonFactory.create()` lấy Gson instance.
4. Dùng `TypeToken` để tạo `Type` cho `List<T>`:
   ```
   Type listType = TypeToken.getParameterized(List.class, clazz).getType();
   ```
5. Gọi `gson.fromJson(content, listType)` → trả kết quả.
6. Nếu kết quả là `null` (file rỗng hoặc chứa `null`) → trả `new ArrayList<>()`.

> **Tại sao không throw exception khi file chưa tồn tại?** Vì lần đầu chạy server, chưa có file `users.json`. Nếu throw → server crash ngay. Trả list rỗng = "chưa có user nào" — hợp lý hơn.

#### Method 2: `writeList`
```
public static <T> void writeList(String filePath, List<T> list)
```

**Đầu vào:**
- `filePath`: Đường dẫn file JSON
- `list`: Danh sách đối tượng cần ghi

**Logic bên trong:**
1. Tạo thư mục cha nếu chưa tồn tại: `Files.createDirectories(Path.of(filePath).getParent())`
2. Dùng `GsonFactory.create()` lấy Gson instance.
3. Gọi `gson.toJson(list)` → JSON string.
4. Ghi ra file bằng `Files.writeString(Path, jsonString)`.

**Xử lý lỗi:**
- Nếu không thể ghi file (permission, disk full...) → throw `RuntimeException` wrapping `IOException`. Đây là lỗi nghiêm trọng, không nên nuốt im lặng.

---

### B.5 File: `GenericDAO.java` (Interface)

**Đường dẫn:** `persistence/dao/GenericDAO.java`

**Vai trò:** Interface định nghĩa 5 thao tác CRUD chuẩn mà MỌI DAO phải implement. Dùng Generics để 1 interface phục vụ cho tất cả loại Schema.

**Khai báo:**
```
public interface GenericDAO<T extends BaseSchema>
```

**Ràng buộc `T extends BaseSchema`:** Đảm bảo DAO chỉ làm việc với Schema (dữ liệu lưu trữ). Không ai có thể tạo `GenericDAO<String>` hay `GenericDAO<User>` — compiler sẽ báo lỗi.

**Methods:**

| # | Signature | Mô tả | Trả về khi không tìm thấy |
|---|-----------|-------|---------------------------|
| 1 | `void save(T entity)` | Thêm entity mới vào danh sách và ghi file | — |
| 2 | `T findById(String id)` | Tìm entity theo ID | `null` |
| 3 | `List<T> findAll()` | Trả toàn bộ danh sách | `List` rỗng (không bao giờ `null`) |
| 4 | `void update(T entity)` | Tìm entity cũ theo ID, thay bằng entity mới, ghi file | — |
| 5 | `void delete(String id)` | Xóa entity theo ID, ghi file | — |

---

### B.6 File: `UserDAO.java` (Implementation)

**Đường dẫn:** `persistence/dao/UserDAO.java`

**Vai trò:** Implementation cụ thể của `GenericDAO` cho `UserSchema`. Đọc/ghi từ file `data/users.json`.

**Khai báo class:**
```
public class UserDAO implements GenericDAO<UserSchema>
```

**Trường dữ liệu:**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `filePath` | `String` | Đường dẫn tới file JSON, mặc định `"data/users.json"` |

**Constructor:**
```
public UserDAO(String filePath)  // cho phép test dùng file khác
public UserDAO()                 // mặc định "data/users.json"
```

**Implement từng method:**

#### `save(UserSchema entity)`:
1. Gọi `JsonFileHelper.readList(filePath, UserSchema.class)` → lấy list hiện tại
2. Thêm entity vào list: `list.add(entity)`
3. Gọi `JsonFileHelper.writeList(filePath, list)` → ghi lại toàn bộ

> **Tại sao đọc toàn bộ → thêm → ghi toàn bộ?** Vì dùng file JSON, không có cách "append" 1 dòng như database thật. Cách này đơn giản, đủ cho MVP. Sau này nếu dùng SQLite/PostgreSQL, chỉ cần thay implementation mà không sửa interface.

#### `findById(String id)`:
1. Đọc list từ file
2. Duyệt list, tìm entity có `getId().equals(id)`
3. Trả entity nếu tìm thấy, `null` nếu không

#### `findAll()`:
1. Gọi `JsonFileHelper.readList(...)` → trả list

#### `update(UserSchema entity)`:
1. Đọc list từ file
2. Duyệt list, tìm entity cũ có cùng ID
3. Thay entity cũ bằng entity mới (dùng `list.set(index, entity)`)
4. Cập nhật `entity.setUpdatedAt(LocalDateTime.now())` trước khi ghi
5. Ghi lại file

#### `delete(String id)`:
1. Đọc list từ file
2. Dùng `list.removeIf(e -> e.getId().equals(id))`
3. Ghi lại file

#### **Method RIÊNG của UserDAO (không có trong GenericDAO):**

```
public UserSchema findByUsername(String username)
```
1. Đọc list từ file
2. Duyệt list, tìm entity có `getUsername().equals(username)`
3. Trả entity nếu tìm thấy, `null` nếu không

> **Tại sao method này không nằm trong GenericDAO?** Vì chỉ UserSchema có trường `username`. ItemSchema, BidSchema không có. Mỗi DAO cụ thể có thể thêm method riêng phù hợp với dữ liệu của mình.

**Mở rộng sau (tuần 4-5):** Khi tạo `ItemDAO`, `AuctionDAO`, `BidDAO`, pattern hoàn toàn giống UserDAO. Chỉ đổi:
- `UserSchema` → `ItemSchema`
- `filePath` → `"data/items.json"`
- Thêm method riêng (ví dụ `ItemDAO.findBySellerId()`)

---

## PHẦN C — Tuần 3: AuthService + SessionManager

### Package đích:
```
src/main/java/com/auctionuet/server/domain/service/
├── AuthService.java
├── SessionManager.java
└── LoginResult.java
```

> [!IMPORTANT]
> Tuần 3 Cường bắt đầu **SAU KHI** merge code tuần 2 của cả nhóm vào `develop`. AuthService sẽ sử dụng:
> - `UserDAO` (của Cường tuần 2) — để tìm/lưu user
> - `PasswordUtils` (của **Anh** tuần 2) — để hash/verify password
> - `UserMapper` (của **Anh** tuần 2) — để chuyển Schema → Domain
> - `ValidationUtils` (của **Anh** tuần 2) — để validate input
> - Các Exception classes (của **Anh** tuần 2)

---

### C.1 File: `AuthService.java`

**Đường dẫn:** `domain/service/AuthService.java`

**Vai trò:** Trái tim 🫀 của hệ thống xác thực. Chứa TOÀN BỘ logic login/register. Controller (của Khánh) chỉ cần gọi 1 method duy nhất, không cần biết bên trong làm gì.

**Khai báo class:**
```
public class AuthService
```

**Trường dữ liệu:**

| Trường | Kiểu | Nguồn |
|--------|------|-------|
| `userDAO` | `UserDAO` | Inject qua constructor |
| `sessionManager` | `SessionManager` | `SessionManager.getInstance()` |

**Constructor:**
```
public AuthService(UserDAO userDAO)
```
Nhận `UserDAO` qua constructor (Dependency Injection đơn giản). Lấy `SessionManager` qua Singleton.

---

#### Method 1: `login`

```
public LoginResult login(String username, String password)
    throws UserNotFoundException, AuthenticationException
```

**Luồng xử lý chi tiết (theo thứ tự):**

| Bước | Hành động | Gọi đến | Kết quả |
|------|-----------|---------|---------|
| 1 | Tìm user trong DB | `userDAO.findByUsername(username)` | `UserSchema` hoặc `null` |
| 2 | Nếu null → ném lỗi | `throw new UserNotFoundException(username)` | — |
| 3 | Xác minh password | `PasswordUtils.verify(password, schema.getPasswordSalt(), schema.getHashedPassword())` | `boolean` |
| 4 | Nếu false → ném lỗi | `throw new AuthenticationException("Sai mật khẩu")` | — |
| 5 | Chuyển Schema → Domain | `UserMapper.toDomain(schema)` | `User` (Bidder/Seller/Admin) |
| 6 | Tạo session | `sessionManager.createSession(user)` | `String token` (UUID) |
| 7 | Trả kết quả | `return new LoginResult(token, user)` | — |

> **Lưu ý quan trọng:** Sau bước 5, biến `schema` (chứa password hash) không còn được tham chiếu nữa. Java GC sẽ thu hồi nó. Từ đây, password hash KHÔNG CÒN tồn tại trong RAM.

---

#### Method 2: `register`

```
public void register(String username, String password, String email, UserRole role)
    throws DuplicateUserException, IllegalArgumentException
```

**Luồng xử lý chi tiết:**

| Bước | Hành động | Gọi đến | Kết quả |
|------|-----------|---------|---------|
| 1 | Validate username | `ValidationUtils.validateUsername(username)` | throw nếu < 3 ký tự |
| 2 | Validate password | `ValidationUtils.validatePassword(password)` | throw nếu < 8 ký tự |
| 3 | Validate email | `ValidationUtils.validateEmail(email)` | throw nếu sai format |
| 4 | Kiểm tra trùng | `userDAO.findByUsername(username)` | `UserSchema` hoặc `null` |
| 5 | Nếu != null → ném lỗi | `throw new DuplicateUserException(username)` | — |
| 6 | Tạo salt | `PasswordUtils.generateSalt()` | `String salt` |
| 7 | Hash password | `PasswordUtils.hash(password, salt)` | `String hashedPassword` |
| 8 | Tạo schema mới | `UserMapper.toNewSchema(username, hashedPassword, salt, email, role)` | `UserSchema` |
| 9 | Lưu DB (WRITE-THROUGH) | `userDAO.save(schema)` | Ghi file ngay lập tức |

> **WRITE-THROUGH** nghĩa là ghi xuống file **NGAY LẬP TỨC**, không buffer. Vì nếu server crash sau bước 8 mà chưa ghi file → user mất tài khoản vừa đăng ký.

---

### C.2 File: `SessionManager.java`

**Đường dẫn:** `domain/service/SessionManager.java`

**Vai trò:** Quản lý session (phiên đăng nhập) của tất cả user đang online. Đây là **Singleton** — chỉ có 1 instance duy nhất trong toàn bộ server.

**Khai báo class:**
```
public class SessionManager
```

**Tại sao Singleton?** Vì tất cả ClientHandler (mỗi client 1 thread) đều cần truy cập cùng 1 danh sách session. Nếu có 2 instance → 2 danh sách khác nhau → token tạo ở instance A không tìm thấy ở instance B → bug.

**Cách implement Singleton (thread-safe):**
Dùng **enum Singleton** (cách đơn giản nhất, thread-safe by default):
```java
private static final SessionManager INSTANCE = new SessionManager();
private SessionManager() {}
public static SessionManager getInstance() { return INSTANCE; }
```

**Trường dữ liệu:**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `tokenMap` | `ConcurrentHashMap<String, User>` | Map từ token → User đang online |

> **Tại sao `ConcurrentHashMap` chứ không phải `HashMap`?** Vì nhiều thread (ClientHandler) đồng thời đọc/ghi map này. `HashMap` không thread-safe → race condition. `ConcurrentHashMap` thread-safe mà không cần `synchronized`.

**Methods bắt buộc:**

| # | Signature | Mô tả |
|---|-----------|-------|
| 1 | `String createSession(User user)` | Sinh UUID token, lưu `{token → user}` vào map, trả token |
| 2 | `User validateToken(String token)` | Tìm user theo token. Nếu không có → throw `AuthenticationException("Token không hợp lệ")` |
| 3 | `void removeSession(String token)` | Xóa token khỏi map (logout) |
| 4 | `void invalidateByUserId(String userId)` | Duyệt map, tìm token nào có user.getId() = userId → xóa. Dùng cho admin kick user |

#### Detail cho `createSession`:
```
public String createSession(User user) {
    String token = UUID.randomUUID().toString();
    tokenMap.put(token, user);
    return token;
}
```

#### Detail cho `validateToken`:
```
public User validateToken(String token) throws AuthenticationException {
    User user = tokenMap.get(token);
    if (user == null) {
        throw new AuthenticationException("Token không hợp lệ hoặc đã hết hạn");
    }
    return user;
}
```

---

### C.3 File: `LoginResult.java`

**Đường dẫn:** `domain/service/LoginResult.java`

**Vai trò:** POJO đơn giản chứa kết quả login: token + user. Dùng để trả từ AuthService cho AuthController.

**Trường:**
- `token` (String, final)
- `user` (User, final)

**Constructor:** nhận 2 tham số, gán trực tiếp.

**Methods:** Chỉ getter, không setter (immutable).

---

## PHẦN D — Test bắt buộc

### D.1 Test tuần 2 (JUnit — file `UserDAOTest.java`)

Tất cả test phải dùng file JSON riêng (ví dụ `"data/test_users.json"`) và dọn dẹp sau mỗi test (`@AfterEach`).

| # | Test name | Mô tả | Kết quả mong đợi |
|---|-----------|-------|-------------------|
| 1 | `testSaveAndFindById` | Save 1 UserSchema, findById → trả đúng | `assertNotNull`, `assertEquals(username)` |
| 2 | `testFindByUsername` | Save 1 UserSchema, findByUsername → trả đúng | `assertEquals(username)` |
| 3 | `testFindAll` | Save 3 UserSchema, findAll → trả 3 | `assertEquals(3, list.size())` |
| 4 | `testUpdate` | Save user, update email, findById → email mới | `assertEquals(newEmail)` |
| 5 | `testDelete` | Save user, delete, findById → null | `assertNull` |
| 6 | `testFileNotExist` | findAll trên file chưa tạo → List rỗng | `assertTrue(list.isEmpty())` |
| 7 | `testGsonLocalDateTime` | Save entity có LocalDateTime, đọc lại → đúng giờ | `assertEquals(dateTime)` |

### D.2 Test tuần 3 (JUnit — file `AuthServiceTest.java`)

| # | Test name | Mô tả | Kết quả mong đợi |
|---|-----------|-------|-------------------|
| 1 | `testRegisterAndLogin` | Register user, login → có token, có user | `assertNotNull(token)` |
| 2 | `testLoginWrongPassword` | Login sai password → throw | `assertThrows(AuthenticationException.class)` |
| 3 | `testLoginUserNotExist` | Login user chưa register → throw | `assertThrows(UserNotFoundException.class)` |
| 4 | `testRegisterDuplicate` | Register 2 user cùng username → throw | `assertThrows(DuplicateUserException.class)` |
| 5 | `testValidateToken` | Login → lấy token → validateToken → đúng user | `assertEquals(username)` |
| 6 | `testLogout` | Login → token → removeSession → validateToken → throw | `assertThrows` |
| 7 | `testSingletonSessionManager` | 2 lần getInstance → cùng object | `assertSame(sm1, sm2)` |

---

## PHẦN E — Dependency & Nhắc nhở phối hợp

### E.1 Tuần 2: Cường KHÔNG phụ thuộc ai

Tuần 2 Cường hoàn toàn độc lập. Không cần code của ai khác.

**Tuy nhiên**, Cường cần biết `UserRole` enum sẽ trông như thế nào (Anh định nghĩa). Đã thống nhất trong Contract:
- `UserRole` có 3 giá trị: `BIDDER`, `SELLER`, `ADMIN`
- Nếu Anh chưa merge, Cường có thể **tự tạo enum `UserRole` tạm** trong package của mình để test, rồi xóa đi khi merge.

### E.2 Tuần 3: Cường phụ thuộc Anh (tuần 2)

| Cường cần | Anh cung cấp (tuần 2) | Nếu Anh chưa xong? |
|-----------|----------------------|---------------------|
| `PasswordUtils.verify()` | `util/PasswordUtils.java` | Cường tự viết mock: return `password.equals(...)` |
| `PasswordUtils.hash()` | `util/PasswordUtils.java` | Tương tự |
| `PasswordUtils.generateSalt()` | `util/PasswordUtils.java` | Tương tự |
| `UserMapper.toDomain()` | `mapper/UserMapper.java` | Cường tự tạo Bidder trực tiếp |
| `UserMapper.toNewSchema()` | `mapper/UserMapper.java` | Cường tự tạo UserSchema trực tiếp |
| `ValidationUtils.*` | `util/ValidationUtils.java` | Bỏ qua validation, test không validate |
| `AuthenticationException` | `exception/` | Cường tự tạo tạm |
| `UserNotFoundException` | `exception/` | Cường tự tạo tạm |
| `DuplicateUserException` | `exception/` | Cường tự tạo tạm |

> [!WARNING]
> **NHẮC NHỞ CHO CƯỜNG:**
> - **Đầu tuần 3**, kiểm tra xem Anh đã push code tuần 2 chưa. Nếu chưa → **nhắc Anh** ngay. AuthService không thể hoàn thiện nếu thiếu PasswordUtils và UserMapper.
> - Nếu Anh chậm hơn 2 ngày → dùng mock tạm (xem bảng trên) để code tiếp, merge mock code sau.

### E.3 Tuần 3: Khánh phụ thuộc Cường

| Khánh cần (tuần 3) | Cường cung cấp (tuần 3) | Deadline cho Cường |
|---------------------|-------------------------|-------------------|
| `AuthService.login()` | `domain/service/AuthService.java` | **Thứ 4 tuần 3** |
| `AuthService.register()` | `domain/service/AuthService.java` | **Thứ 4 tuần 3** |
| `SessionManager.validateToken()` | `domain/service/SessionManager.java` | **Thứ 4 tuần 3** |
| `SessionManager.removeSession()` | `domain/service/SessionManager.java` | **Thứ 4 tuần 3** |

> [!WARNING]
> **NHẮC NHỞ CHO CƯỜNG:**
> - Khánh sẽ viết AuthController + RequestRouter tuần 3. Controller gọi trực tiếp AuthService.
> - Cường phải **push AuthService lên branch trước thứ 4 tuần 3** để Khánh có thể pull về và kết nối.
> - Nếu Cường chậm → Khánh bị block → toàn bộ nhóm bị block (vì Anh cần server chạy để integration test, Công cần server chạy để test GUI).
> - **AuthService là CRITICAL PATH. Ưu tiên cao nhất tuần 3.**

### E.4 Ai cần nhắc ai — Tóm tắt

```
Tuần 2: Không ai chờ ai ✅

Tuần 3:
  Cường nhắc Anh:  "Anh ơi, push PasswordUtils + UserMapper chưa?" (Đầu tuần 3)
  Khánh nhắc Cường: "Cường ơi, push AuthService chưa?" (Thứ 4 tuần 3)
  Anh nhắc TẤT CẢ: "Mọi người merge develop chưa? Tôi cần test" (Thứ 5-6 tuần 3)
  Công nhắc Khánh:  "Khánh ơi, server chạy được chưa? Tôi cần test GUI" (Thứ 5-6 tuần 3)
```

---

## PHẦN F — Checklist hoàn thành

### Tuần 2 — Deadline: Tối thứ 7

- [ ] `BaseSchema.java` — Compile, có constructor rỗng + tường minh, có equals/hashCode
- [ ] `UserSchema.java` — Compile, extends BaseSchema, đủ 5 trường + getter/setter
- [ ] `GsonFactory.java` — Compile, serialize/deserialize LocalDateTime thành công
- [ ] `JsonFileHelper.java` — Compile, readList file không tồn tại → list rỗng
- [ ] `GenericDAO.java` — Compile, 5 method signatures
- [ ] `UserDAO.java` — Compile, implements GenericDAO + findByUsername
- [ ] `UserDAOTest.java` — **7 tests ALL PASS** ✅
- [ ] Code push lên branch `feature/tuan-2-cuong-persistence`
- [ ] Tham gia meeting cuối tuần 2, demo tests

### Tuần 3 — Deadline: Tối thứ 7

- [ ] `AuthService.java` — Compile, login + register logic hoàn chỉnh
- [ ] `SessionManager.java` — Compile, Singleton, thread-safe (ConcurrentHashMap)
- [ ] `LoginResult.java` — Compile, POJO đơn giản
- [ ] `AuthServiceTest.java` — **7 tests ALL PASS** ✅
- [ ] AuthService push lên branch trước **thứ 4** để Khánh dùng
- [ ] Code push lên branch `feature/tuan-3-cuong-auth-service`
- [ ] Tham gia meeting cuối tuần 3, demo live cùng nhóm

---

> **Ghi chú cuối:** Nếu gặp khó khăn với Generics, TypeToken, hoặc Gson TypeAdapter, hãy đọc lại phần TỰ HỌC trong `WEEK2_3_REVISED.md`. Nếu vẫn bí, hỏi nhóm trên chat — đừng mắc kẹt quá 2 tiếng mà không hỏi.
