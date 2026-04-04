# 🔐 Sơ đồ Luồng Xác thực (Authentication Flows)

> Tài liệu bổ trợ cho [server_architecture_overhaul.md](./server_architecture_overhaul.md)

---

## 1. Luồng Đăng nhập (Login)

Từ lúc người dùng nhập tài khoản/mật khẩu đến khi nhận được token và thông tin user.

```mermaid
sequenceDiagram
    actor Client
    participant CH as ClientHandler
    participant RR as RequestRouter
    participant ATC as AuthController
    participant Auth as AuthService
    participant PU as PasswordUtils
    participant DAO as UserDAO
    participant JF as users.json
    participant Map as UserMapper
    participant SM as SessionManager

    Client->>CH: Gửi JSON: action=LOGIN, username, password
    CH->>RR: deserialize → Request
    RR->>ATC: route(LOGIN)
    ATC->>Auth: login(username, password)

    Note over Auth,JF: Bước 1 — Tìm user trong DB
    Auth->>DAO: findByUsername(username)
    DAO->>JF: Đọc file JSON
    JF-->>DAO: Raw JSON data
    DAO-->>Auth: UserSchema (chứa hashedPassword, salt)

    alt UserSchema == null
        Auth-->>ATC: throw UserNotFoundException
        ATC-->>CH: Response.error("User không tồn tại")
        CH-->>Client: JSON: type=RESPONSE, status=ERROR
    end

    Note over Auth,PU: Bước 2 — Xác minh mật khẩu
    Auth->>PU: verify(password, schema.salt, schema.hashedPassword)
    PU-->>Auth: boolean match

    alt match == false
        Auth-->>ATC: throw AuthenticationException
        ATC-->>CH: Response.error("Sai mật khẩu")
        CH-->>Client: JSON: type=RESPONSE, status=ERROR
    end

    Note over Auth,SM: Bước 3 — Tạo session
    Auth->>Map: toDomain(schema)
    Map-->>Auth: User (Bidder/Seller/Admin, KHÔNG có password)
    Note over Auth: UserSchema bị GC thu hồi từ đây
    Auth->>SM: createSession(user)
    SM-->>Auth: token (UUID string)

    Note over ATC,Client: Bước 4 — Trả response
    Auth-->>ATC: LoginResult(token, user)
    ATC->>Map: toDTO(user)
    Map-->>ATC: UserDTO (id, username, role)
    ATC-->>CH: Response.ok(token + UserDTO)
    CH-->>Client: JSON: type=RESPONSE, status=OK, data={token, userDTO}
```

---

## 2. Luồng Đăng ký (Register)

Từ lúc người dùng điền form đăng ký đến khi tài khoản được lưu.

```mermaid
sequenceDiagram
    actor Client
    participant CH as ClientHandler
    participant RR as RequestRouter
    participant ATC as AuthController
    participant Auth as AuthService
    participant VU as ValidationUtils
    participant PU as PasswordUtils
    participant DAO as UserDAO
    participant JF as users.json
    participant Map as UserMapper

    Client->>CH: JSON: action=REGISTER, username, password, email, role
    CH->>RR: deserialize → Request
    RR->>ATC: route(REGISTER)
    ATC->>Auth: register(username, password, email, role)

    Note over Auth,VU: Bước 1 — Validate đầu vào
    Auth->>VU: validateUsername(username)
    Auth->>VU: validatePassword(password)
    Auth->>VU: validateEmail(email)

    alt Validation thất bại
        Auth-->>ATC: throw IllegalArgumentException
        ATC-->>CH: Response.error("Dữ liệu không hợp lệ: ...")
        CH-->>Client: JSON: type=RESPONSE, status=ERROR
    end

    Note over Auth,DAO: Bước 2 — Kiểm tra trùng lặp
    Auth->>DAO: findByUsername(username)
    DAO->>JF: Đọc file
    JF-->>DAO: Kết quả

    alt Username đã tồn tại
        Auth-->>ATC: throw DuplicateUserException
        ATC-->>CH: Response.error("Username đã tồn tại")
        CH-->>Client: JSON: type=RESPONSE, status=ERROR
    end

    Note over Auth,JF: Bước 3 — Hash password & Lưu DB
    Auth->>PU: generateSalt()
    PU-->>Auth: salt
    Auth->>PU: hash(password, salt)
    PU-->>Auth: hashedPassword
    Auth->>Map: toNewSchema(username, hashedPassword, salt, email, role)
    Note over Map: IdGenerator.generate() → UUID mới
    Note over Map: createdAt = updatedAt = now()
    Map-->>Auth: UserSchema mới
    Auth->>DAO: save(userSchema)
    DAO->>JF: Ghi file JSON (WRITE-THROUGH)

    Note over ATC,Client: Bước 4 — Trả response
    Auth-->>ATC: Thành công
    ATC-->>CH: Response.ok("Đăng ký thành công")
    CH-->>Client: JSON: type=RESPONSE, status=OK
```

---

## 3. Luồng Đăng xuất (Logout)

```mermaid
sequenceDiagram
    actor Client
    participant CH as ClientHandler
    participant RR as RequestRouter
    participant ATC as AuthController
    participant SM as SessionManager
    participant AM as AuctionManager
    participant LA as LiveAuction

    Client->>CH: JSON: action=LOGOUT, token
    CH->>RR: deserialize → Request
    RR->>ATC: route(LOGOUT)

    ATC->>SM: validateToken(token)
    SM-->>ATC: User

    Note over ATC,LA: Bước 1 — Dọn dẹp subscription
    ATC->>AM: getAuction(subscribedAuctionId)
    AM-->>ATC: LiveAuction
    ATC->>LA: removeObserver(clientHandler)

    Note over ATC,SM: Bước 2 — Xóa session
    ATC->>SM: removeSession(token)

    Note over CH: Bước 3 — Reset trạng thái ClientHandler
    ATC-->>CH: Response.ok("Đã đăng xuất")
    CH->>CH: currentUser = null
    CH->>CH: subscribedAuctionId = null
    CH-->>Client: JSON: type=RESPONSE, status=OK
```

---

## 4. Luồng Xác thực Token (dùng cho mọi request sau login)

Mọi request (trừ LOGIN, REGISTER, PING) đều phải qua bước xác thực token trước.

```mermaid
flowchart TD
    A[Client gửi Request kèm token] --> B{Token có trong request?}
    B -->|Không| C[Response.error — Missing token]
    B -->|Có| D[SessionManager.validateToken]
    D --> E{Token hợp lệ?}
    E -->|Không| F[Response.error — Invalid/expired token]
    E -->|Có| G[Lấy User từ SessionManager]
    G --> H{User có quyền thực hiện action?}
    H -->|Không| I[Response.error — Permission denied]
    H -->|Có| J[Chuyển tiếp tới Controller xử lý]
    J --> K[Response.ok — Kết quả]

    style C fill:#f66,stroke:#333
    style F fill:#f66,stroke:#333
    style I fill:#f66,stroke:#333
    style K fill:#6f6,stroke:#333
```
