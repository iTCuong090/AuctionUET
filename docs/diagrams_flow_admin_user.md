# 👤 Sơ đồ Luồng Quản trị & Các thao tác User

> Tài liệu bổ trợ cho [server_architecture_overhaul.md](./server_architecture_overhaul.md)

---

## 1. Luồng Admin xem danh sách Users

```mermaid
sequenceDiagram
    actor Admin
    participant CH as ClientHandler
    participant ADC as AdminController
    participant SM as SessionManager
    participant DAO as UserDAO
    participant JF as users.json
    participant Map as UserMapper

    Admin->>CH: JSON: action=GET_ALL_USERS, token
    CH->>ADC: route(GET_ALL_USERS)
    ADC->>SM: validateToken(token) → Admin
    ADC->>ADC: user.hasPermission("MANAGE_USERS") → true

    ADC->>DAO: findAll()
    DAO->>JF: Đọc file
    JF-->>DAO: List〈UserSchema〉
    DAO-->>ADC: List〈UserSchema〉

    loop Mỗi UserSchema
        ADC->>Map: toDTO(toDomain(schema))
        Map-->>ADC: UserDTO (KHÔNG có password)
    end

    ADC-->>CH: Response.ok(List〈UserDTO〉)
    CH-->>Admin: RESPONSE OK + danh sách UserDTO
```

---

## 2. Luồng Admin thay đổi vai trò User

```mermaid
sequenceDiagram
    actor Admin
    participant CH as ClientHandler
    participant ADC as AdminController
    participant SM as SessionManager
    participant DAO as UserDAO
    participant JF as users.json

    Admin->>CH: JSON: action=UPDATE_ROLE, targetUserId, newRole, token
    CH->>ADC: route(UPDATE_ROLE)
    ADC->>SM: validateToken(token) → Admin
    ADC->>ADC: user.hasPermission("MANAGE_USERS") → true

    ADC->>DAO: findById(targetUserId)
    DAO->>JF: Đọc file
    JF-->>DAO: UserSchema
    DAO-->>ADC: UserSchema

    alt User không tồn tại
        ADC-->>CH: Response.error("User không tồn tại")
        CH-->>Admin: RESPONSE ERROR
    end

    ADC->>ADC: schema.setRole(newRole)
    ADC->>ADC: schema.setUpdatedAt(now)
    ADC->>DAO: update(schema)
    DAO->>JF: Ghi file (WRITE-THROUGH)

    Note over SM: Nếu user đang login → cập nhật session
    ADC->>SM: invalidateSessionByUserId(targetUserId)
    Note over SM: User sẽ phải login lại để có role mới

    ADC-->>CH: Response.ok("Đã cập nhật role")
    CH-->>Admin: RESPONSE OK
```

---

## 3. Luồng Admin xóa User

```mermaid
sequenceDiagram
    actor Admin
    participant CH as ClientHandler
    participant ADC as AdminController
    participant SM as SessionManager
    participant DAO as UserDAO
    participant JF as users.json

    Admin->>CH: JSON: action=DELETE_USER, targetUserId, token
    CH->>ADC: route(DELETE_USER)
    ADC->>SM: validateToken(token) → Admin
    ADC->>ADC: user.hasPermission("MANAGE_USERS") → true
    ADC->>ADC: Validate: targetUserId ≠ admin.id (không tự xóa mình)

    ADC->>SM: invalidateSessionByUserId(targetUserId)
    Note over SM: Kick user đang login (nếu có)

    ADC->>DAO: delete(targetUserId)
    DAO->>JF: Ghi file (WRITE-THROUGH)

    ADC-->>CH: Response.ok("Đã xóa user")
    CH-->>Admin: RESPONSE OK
```

---

## 4. Luồng User xem / cập nhật Profile

```mermaid
sequenceDiagram
    actor Client
    participant CH as ClientHandler
    participant UC as UserController
    participant SM as SessionManager
    participant DAO as UserDAO
    participant JF as users.json
    participant Map as UserMapper

    Note over Client,JF: GET_PROFILE
    Client->>CH: JSON: action=GET_PROFILE, token
    CH->>UC: route(GET_PROFILE)
    UC->>SM: validateToken(token) → User
    UC->>Map: toDTO(user)
    Map-->>UC: UserDTO
    UC-->>CH: Response.ok(UserDTO)
    CH-->>Client: RESPONSE OK + UserDTO

    Note over Client,JF: UPDATE_PROFILE (VD: đổi email)
    Client->>CH: JSON: action=UPDATE_PROFILE, token, newEmail
    CH->>UC: route(UPDATE_PROFILE)
    UC->>SM: validateToken(token) → User

    UC->>DAO: findById(user.id)
    DAO-->>UC: UserSchema
    UC->>UC: schema.setEmail(newEmail)
    UC->>UC: schema.setUpdatedAt(now)
    UC->>DAO: update(schema)
    DAO->>JF: Ghi file

    UC-->>CH: Response.ok("Cập nhật thành công")
    CH-->>Client: RESPONSE OK
```

---

## 5. Luồng Seller quản lý sản phẩm (Sửa / Xóa Item)

```mermaid
sequenceDiagram
    actor Seller
    participant CH as ClientHandler
    participant AC as AuctionController
    participant SM as SessionManager
    participant IDAO as ItemDAO
    participant ADAO as AuctionDAO
    participant JF as items.json

    Note over Seller,JF: Sửa Item (chỉ khi chưa có auction RUNNING)
    Seller->>CH: JSON: action=UPDATE_ITEM, token, itemId, updatedData
    CH->>AC: route(UPDATE_ITEM)
    AC->>SM: validateToken(token) → Seller

    AC->>IDAO: findById(itemId)
    IDAO-->>AC: ItemSchema
    AC->>AC: Validate: schema.sellerId == seller.id

    AC->>ADAO: findByItemId(itemId)
    ADAO-->>AC: List〈AuctionSchema〉

    alt Có auction đang RUNNING cho item này
        AC-->>CH: Response.error("Không thể sửa item đang đấu giá")
        CH-->>Seller: RESPONSE ERROR
    end

    AC->>AC: Cập nhật các trường: name, description, price...
    AC->>IDAO: update(schema)
    IDAO->>JF: Ghi file
    AC-->>CH: Response.ok(ItemDTO mới)
    CH-->>Seller: RESPONSE OK
```

---

## 6. Sơ đồ phân quyền (Permission Matrix)

Tổng hợp quyền hạn của từng vai trò, được implement trong `hasPermission()`.

```mermaid
graph LR
    subgraph Bidder["🙋 Bidder"]
        B1[PLACE_BID ✅]
        B2[VIEW_AUCTION ✅]
        B3[VIEW_BID_HISTORY ✅]
        B4[GET_PROFILE ✅]
        B5[UPDATE_PROFILE ✅]
        B6[SUBSCRIBE ✅]
    end

    subgraph Seller["💼 Seller"]
        S1[CREATE_ITEM ✅]
        S2[UPDATE_ITEM ✅]
        S3[DELETE_ITEM ✅]
        S4[CREATE_AUCTION ✅]
        S5[START_AUCTION ✅]
        S6[VIEW_AUCTION ✅]
        S7[GET_PROFILE ✅]
    end

    subgraph Admin["🛡️ Admin"]
        A1[GET_ALL_USERS ✅]
        A2[DELETE_USER ✅]
        A3[UPDATE_ROLE ✅]
        A4[VIEW_AUCTION ✅]
        A5[Mọi action quản trị ✅]
    end
```

---

## 7. Phân biệt RESPONSE vs PUSH — Flowchart phía Client

```mermaid
flowchart TD
    A[Client nhận JSON từ socket] --> B{Trường type?}

    B -->|"RESPONSE"| C[Main Thread xử lý]
    C --> C1[Cập nhật UI cho action vừa gửi]
    C --> C2[Hiển thị thông báo thành công/lỗi]

    B -->|"PUSH"| D[NotificationListener Thread]
    D --> E{Trường event?}
    E -->|"BID_UPDATE"| F["Platform.runLater → Cập nhật giá mới trên UI"]
    E -->|"AUCTION_ENDED"| G["Platform.runLater → Hiển thị kết quả + disable bid"]
    E -->|"USER_KICKED"| H["Platform.runLater → Quay về màn hình login"]
    E -->|Khác| I[Log warning — event không nhận diện]

    style C fill:#4a9,stroke:#333,color:#fff
    style D fill:#49a,stroke:#333,color:#fff
    style F fill:#fa4,stroke:#333
    style G fill:#fa4,stroke:#333
    style H fill:#f44,stroke:#333
```
