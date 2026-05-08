<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" />
  <img src="https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=java" />
  <img src="https://img.shields.io/badge/Maven-3.x-C71A36?style=for-the-badge&logo=apachemaven" />
  <img src="https://img.shields.io/badge/Architecture-Client--Server-green?style=for-the-badge" />
</p>

# AuctionUET — Hệ thống đấu giá trực tuyến

> Hệ thống đấu giá trực tuyến (Online Auction System) dựa trên kiến trúc **Client-Server** bằng Java. Ứng dụng mô hình **MVC** (JavaFX), **Design Patterns** (Singleton, Observer, Factory Method), xử lý **đa luồng** (Concurrent Bidding) và cập nhật **thời gian thực** (Realtime Update).

---

## Mục lục

- [Thành viên nhóm](#-thành-viên-nhóm)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Cách cài đặt & chạy](#-cách-cài-đặt--chạy)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Bảng phân công nhiệm vụ 4 tuần](#-bảng-phân-công-nhiệm-vụ-chi-tiết-4-tuần)
- [Sơ đồ thiết kế lớp UML](#-sơ-đồ-thiết-kế-lớp-uml)
- [Sơ đồ luồng dữ liệu](#-sơ-đồ-luồng-dữ-liệu-data-flow-diagrams)
- [Công nghệ & Design Patterns](#-công-nghệ--design-patterns-sử-dụng)

---

## 👥 Thành viên nhóm

| # | Họ và tên | Vai trò chính | Tầng phụ trách |
|:-:|-----------|---------------|----------------|
| 1 | **Tạ Hữu Cường** | Team Lead · Server Core & Database | Persistence Layer · Service Layer |
| 2 | **Đào Đình Khánh** | Networking & Protocol | Network Layer · Controller |
| 3 | **Nguyễn Cao Công** | Client GUI (JavaFX) | Client Views · Client Network |
| 4 | **Ngô Duy Anh** | Business Logic & Testing | Domain Model · Mapper · Integration Test |

---

## Yêu cầu hệ thống

| Thành phần | Phiên bản tối thiểu |
|------------|---------------------|
| JDK | 21 (Eclipse Temurin) |
| Maven | 3.x |
| Scene Builder | Gluon Scene Builder (cho phát triển UI) |
| IntelliJ IDEA | Community hoặc Ultimate |

---

## Cách cài đặt & chạy

```bash
# 1. Clone repository
git clone https://github.com/iTCuong090/AuctionUET.git
cd AuctionUET

# 2. Build toàn bộ project
mvn clean compile

# 3. Chạy tests
mvn test

# 4. Chạy Server
cd auction-server
mvn exec:java -Dexec.mainClass="com.auctionuet.server.ServerApp"

# 5. Chạy Client (cửa sổ mới)
cd auction-client
mvn javafx:run
```

---

## Cấu trúc dự án

```
AuctionUET/
├── auction-server/                         # Module Server
│   └── src/main/java/com/auctionuet/server/
│       ├── persistence/                    # Tầng lưu trữ (Schema + DAO + JSON)
│       │   ├── schema/                     #   BaseSchema, UserSchema, ItemSchema, ...
│       │   └── dao/                        #   GenericDAO, UserDAO, ItemDAO, ...
│       ├── domain/                         # Tầng nghiệp vụ
│       │   ├── model/                      #   User, Bidder, Seller, Item, LiveAuction, ...
│       │   ├── enums/                      #   UserRole, AuctionStatus, ItemType
│       │   ├── service/                    #   AuthService, AuctionService, ItemService, ...
│       │   └── manager/                    #   AuctionManager, DataManager
│       ├── network/                        # Tầng mạng
│       │   ├── protocol/                   #   ActionType, Request, Response, MessageSerializer
│       │   ├── controller/                 #   AuthController, AuctionController, ItemController
│       │   ├── dto/                        #   UserDTO, ItemDTO, AuctionDTO, BidDTO
│       │   └── server/                     #   AuctionServer, ClientHandler, RequestRouter
│       ├── mapper/                         # Chuyển đổi dữ liệu
│       │   ├── UserMapper, ItemMapper, AuctionMapper, BidMapper
│       ├── util/                           # Tiện ích
│       │   ├── PasswordUtils, IdGenerator, ValidationUtils
│       │   └── json/ (GsonFactory, JsonFileHelper, ...)
│       └── exception/                      # Ngoại lệ tùy chỉnh
│
├── auction-client/                         # Module Client (JavaFX)
│   └── src/main/java/com/auctionuet/client/
│       ├── view/                           # Controller cho các View
│       │   ├── LoginController, RegisterController, DashboardController
│       │   ├── CreateItemController, CreateAuctionController
│       │   ├── AuctionListController, AuctionDetailController
│       │   └── SceneManager
│       ├── network/                        # Giao tiếp với Server
│       │   ├── ServerConnection, AuthClient, ItemClient, AuctionClient
│       │   └── protocol/ (Request, Response, ActionType, ...)
│       └── model/                          # DTO phía Client
│           ├── ClientSession, UserDTO, ItemDTO, AuctionDTO, BidDTO
│
├── data/                                   # JSON Database (users.json, items.json, ...)
├── planning/                               # Kế hoạch & phân công từng tuần
├── docs/                                   # Tài liệu kỹ thuật
└── pom.xml                                 # Parent POM (Multi-module Maven)
```

---

## Bảng phân công nhiệm vụ chi tiết 4 tuần

### Tuần 1 (23/03 – 29/03/2026): Khởi tạo nền tảng

> **Mục tiêu:** Tạo nền móng dự án — Maven multi-module, CI/CD, JavaFX Hello World, Git workflow.

| Thành viên | Nhiệm vụ | Branch | Nội dung chi tiết | Trạng thái |
|:----------:|----------|--------|--------------------|:----------:|
| **Cường** | Khởi tạo Maven Multi-Module Project | `feature/tuan-1-cuong-maven-setup` | Tạo Parent POM, module `auction-server` + `auction-client`, cấu trúc Maven chuẩn, cấu hình JDK 21, JUnit 5, Gson, `.gitignore` | ✅ |
| **Khánh** | Thiết lập Git Workflow & CI/CD | `feature/tuan-1-khanh-git-cicd` | GitHub Actions CI (`ci.yml`), `CONTRIBUTING.md`, PR template, Issue template, Branch Protection rules, cập nhật `README.md` | ✅ |
| **Công** | JavaFX Hello World + Scene Builder | `feature/tuan-1-cong-javafx-hello` | `MainView.fxml`, `MainController.java` (button animation), `ClientApp.java` (load FXML), CSS dark theme cơ bản, tài liệu `javafx-guide.md` | ✅ |
| **Anh** | Thiết kế Domain Model + Exception | `feature/tuan-1-anh-domain` | Thiết kế sơ bộ cấu trúc domain, tạo custom exception classes, chuẩn bị base cho tuần 2 | ✅ |

> **Phần chung (tất cả):** Cài đặt môi trường (JDK 21, Maven, IntelliJ, Scene Builder, Git), học Git cơ bản, học Maven cơ bản, học JavaFX cơ bản.

---

### Tuần 2 (30/03 – 05/04/2026): Xây nền tảng 4 tầng song song — Feature: Auth (Phần 1)

> **Mục tiêu:** Mỗi người hoàn thành một khối code **độc lập, tự test được** trên branch riêng. Chưa kết nối với nhau.

| Thành viên | Tầng phụ trách | Branch | Nội dung chi tiết | Trạng thái |
|:----------:|----------------|--------|--------------------|:----------:|
| **Cường** | Persistence Layer | `feature/tuan-2-cuong-persistence` | `BaseSchema` (abstract, id/timestamps), `UserSchema` (extends BaseSchema), `GsonFactory` (LocalDateTime TypeAdapter, PrettyPrinting), `JsonFileHelper` (readList/writeList generic), `GenericDAO<T>` (interface CRUD), `UserDAO` (implementation + `findByUsername`) | ✅ |
| **Khánh** | Network Protocol + Socket | `feature/tuan-2-khanh-network` | `ActionType` enum (LOGIN, REGISTER, LOGOUT, PING), `Request` / `Response` class, `MessageSerializer` (JSON serialize/deserialize), `AuctionServer` (ServerSocket port 8888), `ClientHandler` (implements Runnable, vòng lặp đọc JSON) | ✅ |
| **Công** | Client GUI | `feature/tuan-2-cong-client-gui` | `LoginView.fxml` + `LoginController` (validate trên client, print console), `RegisterView.fxml` + `RegisterController` (validate email, password ≥ 8 ký tự), `SceneManager` (Singleton, chuyển scene), `styles.css` (dark theme hiện đại) | ✅ |
| **Anh** | Domain + Mapper + Utils | `feature/tuan-2-anh-domain` | `UserRole` enum, `User` (abstract, immutable, `hasPermission()`), `Bidder`/`Seller`/`Admin` (subclass), `UserDTO`, `UserMapper` (toDomain/toDTO/toNewSchema), `IdGenerator`, `PasswordUtils` (SHA-256 + salt), `ValidationUtils`, custom Exceptions | ✅ |

---

### Tuần 3 (06/04 – 12/04/2026): Kết nối hệ thống — Feature: Auth (Phần 2, End-to-End)

> **Mục tiêu:** Chạy được **MVP hoàn chỉnh**: Mở Client → Login/Register → Server xác thực → Client chuyển sang Dashboard.

| Thành viên | Tầng phụ trách | Branch | Nội dung chi tiết | Trạng thái |
|:----------:|----------------|--------|--------------------|:----------:|
| **Cường** | AuthService + SessionManager | `feature/tuan-3-cuong-auth-service` | `AuthService` (`login()`: query DAO → verify password → tạo session, `register()`: validate → hash → save), `SessionManager` (Singleton, ConcurrentHashMap token↔User, `createSession`, `validateToken`, `removeSession`, `invalidateByUserId`), `LoginResult` (POJO: token + user) | ✅ |
| **Khánh** | AuthController + Router | `feature/tuan-3-khanh-controller` | `AuthController` (`handleLogin`, `handleRegister`, `handleLogout` — bắt exception, trả Response), `RequestRouter` (switch-case route theo ActionType), cập nhật `ClientHandler` gọi Router | ✅ |
| **Công** | Client Network + GUI kết nối | `feature/tuan-3-cong-client-network` | `ServerConnection` (Singleton, TCP socket, `sendRequest` blocking), `AuthClient` (adapter Login/Register/Logout), cập nhật `LoginController`/`RegisterController` (background thread + `Platform.runLater`), `DashboardView.fxml` + `DashboardController` (hiện "Xin chào, {username}"), `ClientSession` | ✅ |
| **Anh** | Integration Test + Error Handling | `feature/tuan-3-anh-integration` | `AuthIntegrationTest` (E2E qua socket: Register → Login → token → Logout), test scenarios (duplicate username, wrong password, non-existent user, invalid JSON, multiple clients, PING), `TestHelper` utility | ✅ |

---

### Tuần 4 (14/04 – 20/04/2026): Feature: Auction (Sản phẩm, Phiên đấu giá, Phân quyền)

> **Mục tiêu:** Hoàn thiện luồng Auction xuyên suốt: Đăng sản phẩm → Tạo phiên đấu giá → Bắt đầu phiên → Xem danh sách.

| Thành viên | Tầng phụ trách | Branch | Nội dung chi tiết | Trạng thái |
|:----------:|----------------|--------|--------------------|:----------:|
| **Cường** | Persistence mở rộng + AuctionService | `feature/tuan-4-cuong-auction-persistence` | `ItemSchema` (abstract) + `ElectronicsSchema` / `ArtSchema` / `VehicleSchema` (subclass), `AuctionSchema`, `BidSchema`, `ItemDAO` (`findBySellerId`), `AuctionDAO` (`findByStatus`, `findBySellerId`), `BidDAO`, `RuntimeTypeAdapterFactory` cho Gson, `AuctionService` (createAuction, startAuction, endAuction, getAuctions), `ItemService` (createItem, getItemsBySellerId), `DataManager` (Singleton, quản lý tất cả DAO), `AuctionManager` (Singleton, quản lý LiveAuction trong RAM + ScheduledExecutorService auto-end) | ✅ |
| **Khánh** | Network mở rộng + Phân quyền | `feature/tuan-4-khanh-auction-network` | Mở rộng `ActionType` (CREATE_ITEM, GET_MY_ITEMS, CREATE_AUCTION, START_AUCTION, GET_AUCTIONS, GET_AUCTION_DETAIL), `AuctionController` (handleCreateAuction, handleStartAuction, handleGetAuctions, handleGetAuctionDetail), `ItemController` (handleCreateItem, handleGetMyItems), cập nhật `RequestRouter` (route tất cả action mới), hệ thống phân quyền (validate token + `hasPermission` trước khi xử lý) | ✅ |
| **Công** | Client GUI Auction + Network | `feature/tuan-4-cong-auction-gui` | `CreateItemView.fxml` + `CreateItemController` (form đăng sản phẩm), `CreateAuctionView.fxml` + `CreateAuctionController` (chọn item, set thời gian), `AuctionListView.fxml` + `AuctionListController` (danh sách phiên đấu giá), `AuctionDetailView.fxml` + `AuctionDetailController`, `ItemClient` + `AuctionClient` (adapter giao tiếp server), Sidebar Navigation | ✅ |
| **Anh** | Domain mở rộng + Integration Test | `feature/tuan-4-anh-auction-domain` | `ItemType` enum, `AuctionStatus` enum (OPEN, RUNNING, FINISHED, PAID, CANCELED), `Item` model (immutable, phẳng), `LiveAuction` (bidHistory, ReentrantLock, Observer pattern, `placeBid`), `BidRecord` (immutable POJO), `AuctionObserver` (interface: `onBidPlaced`, `onAuctionEnded`), `ItemMapper` / `AuctionMapper` / `BidMapper` (toDomain/toDTO/toNewSchema), `ItemDTO` / `AuctionDTO` / `BidDTO`, Integration test cho Auction flow | ✅ |

---

### Tổng hợp đóng góp theo tuần

| Thành viên | Tuần 1 | Tuần 2 | Tuần 3 | Tuần 4 | Vai trò xuyên suốt |
|:----------:|--------|--------|--------|--------|---------------------|
| **Cường** | Maven Setup | Persistence Layer | AuthService + SessionManager | AuctionService + Persistence mở rộng | Server Core, Database, Service Layer |
| **Khánh** | Git/CI-CD | Network Protocol + Socket | AuthController + Router | AuctionController + Phân quyền | Network Layer, Routing |
| **Công** | JavaFX Hello World | Login/Register GUI | Client Network + GUI kết nối | Auction GUI + Client Network mở rộng | Toàn bộ Client-side |
| **Anh** | Domain thiết kế sơ bộ | Domain Model + Mapper + Utils | Integration Test E2E Auth | Domain mở rộng + Auction Test | Domain Model, Mapper, Testing |

---

## Sơ đồ thiết kế lớp UML

### Sơ đồ lớp — Persistence Layer (Schema & DAO)

```mermaid
classDiagram
    direction TB

    class BaseSchema {
        <<abstract>>
        #String id
        #LocalDateTime createdAt
        #LocalDateTime updatedAt
        +getId() String
        +setId(String)
        +getCreatedAt() LocalDateTime
        +setCreatedAt(LocalDateTime)
        +getUpdatedAt() LocalDateTime
        +setUpdatedAt(LocalDateTime)
        +equals(Object) boolean
        +hashCode() int
    }

    class UserSchema {
        -String username
        -String hashedPassword
        -String passwordSalt
        -String email
        -UserRole role
        +getUsername() String
        +getHashedPassword() String
        +getPasswordSalt() String
        +getEmail() String
        +getRole() UserRole
    }

    class ItemSchema {
        <<abstract>>
        -String name
        -String description
        -double startingPrice
        -ItemType type
        -String sellerId
        -String imageUrl
        -String condition
        -int auctionCount
        +getName() String
        +getStartingPrice() double
        +getType() ItemType
        +getSellerId() String
    }

    class ElectronicsSchema {
        -String brand
        -int warrantyMonths
        +getBrand() String
        +getWarrantyMonths() int
    }

    class ArtSchema {
        -String artist
        -int year
        -String medium
        +getArtist() String
        +getYear() int
        +getMedium() String
    }

    class VehicleSchema {
        -String make
        -String model
        -int mileage
        -int vehicleYear
        +getMake() String
        +getModel() String
        +getMileage() int
    }

    class AuctionSchema {
        -String itemId
        -String sellerId
        -String title
        -String description
        -LocalDateTime startTime
        -LocalDateTime endTime
        -AuctionStatus status
        -double highestBid
        -String winnerId
        +getItemId() String
        +getStatus() AuctionStatus
        +getHighestBid() double
    }

    class BidSchema {
        -String auctionId
        -String bidderId
        -double amount
        -LocalDateTime timestamp
        +getAuctionId() String
        +getBidderId() String
        +getAmount() double
    }

    BaseSchema <|-- UserSchema
    BaseSchema <|-- ItemSchema
    BaseSchema <|-- AuctionSchema
    BaseSchema <|-- BidSchema
    ItemSchema <|-- ElectronicsSchema
    ItemSchema <|-- ArtSchema
    ItemSchema <|-- VehicleSchema
```

```mermaid
classDiagram
    direction TB

    class GenericDAO~T~ {
        <<interface>>
        +save(T entity) void
        +findById(String id) T
        +findAll() List~T~
        +update(T entity) void
        +delete(String id) void
    }

    class UserDAO {
        -String filePath
        +save(UserSchema) void
        +findById(String) UserSchema
        +findAll() List~UserSchema~
        +findByUsername(String) UserSchema
    }

    class ItemDAO {
        -String filePath
        +save(ItemSchema) void
        +findById(String) ItemSchema
        +findAll() List~ItemSchema~
        +findBySellerId(String) List~ItemSchema~
    }

    class AuctionDAO {
        -String filePath
        +save(AuctionSchema) void
        +findById(String) AuctionSchema
        +findAll() List~AuctionSchema~
        +findByStatus(AuctionStatus) List~AuctionSchema~
        +findBySellerId(String) List~AuctionSchema~
    }

    class BidDAO {
        -String filePath
        +save(BidSchema) void
        +findById(String) BidSchema
        +findAll() List~BidSchema~
    }

    class JsonFileHelper {
        +readList(String, Class~T~) List~T~
        +writeList(String, List~T~) void
    }

    class GsonFactory {
        +create() Gson
    }

    GenericDAO <|.. UserDAO : implements
    GenericDAO <|.. ItemDAO : implements
    GenericDAO <|.. AuctionDAO : implements
    GenericDAO <|.. BidDAO : implements

    UserDAO ..> JsonFileHelper : uses
    ItemDAO ..> JsonFileHelper : uses
    AuctionDAO ..> JsonFileHelper : uses
    BidDAO ..> JsonFileHelper : uses
    JsonFileHelper ..> GsonFactory : uses
```

---

### 2️⃣ Sơ đồ lớp — Domain Layer (Model & Enums)

```mermaid
classDiagram
    direction TB

    class User {
        <<abstract>>
        -final String id
        -final String username
        -final UserRole role
        +getId() String
        +getUsername() String
        +getRole() UserRole
        +hasPermission(String action)* boolean
        +getDisplayInfo()* String
    }

    class Bidder {
        +Bidder(String id, String username)
        +hasPermission(String) boolean
        +getDisplayInfo() String
    }

    class Seller {
        +Seller(String id, String name)
        +hasPermission(String) boolean
        +getDisplayInfo() String
    }

    class Admin {
        +Admin(String id, String name)
        +hasPermission(String) boolean
        +getDisplayInfo() String
    }

    class UserRole {
        <<enumeration>>
        BIDDER
        SELLER
        ADMIN
    }

    class Item {
        -final String id
        -final String name
        -final String description
        -final double startingPrice
        -final ItemType type
        -final String sellerId
        -final String imageUrl
        -final String condition
        +getId() String
        +getName() String
        +getStartingPrice() double
    }

    class LiveAuction {
        -final String id
        -final String itemId
        -final String sellerId
        -final LocalDateTime endTime
        -final List~BidRecord~ bidHistory
        -AuctionStatus status
        -double currentHighestBid
        -String currentWinnerId
        -final ReentrantLock bidLock
        -final List~AuctionObserver~ observers
        +placeBid(User, double) BidRecord
        +addObserver(AuctionObserver) void
        +removeObserver(AuctionObserver) void
        +notifyAuctionEnded() void
    }

    class BidRecord {
        -final String bidderId
        -final String bidderUsername
        -final double amount
        -final LocalDateTime timestamp
        +getBidderId() String
        +getAmount() double
    }

    class AuctionObserver {
        <<interface>>
        +onBidPlaced(BidRecord) void
        +onAuctionEnded(String, String, double) void
    }

    class AuctionStatus {
        <<enumeration>>
        OPEN
        RUNNING
        FINISHED
        PAID
        CANCELED
    }

    class ItemType {
        <<enumeration>>
        ELECTRONICS
        ART
        VEHICLE
    }

    User <|-- Bidder
    User <|-- Seller
    User <|-- Admin
    User --> UserRole : has

    Item --> ItemType : has
    LiveAuction --> AuctionStatus : has
    LiveAuction --> BidRecord : contains *
    LiveAuction --> AuctionObserver : notifies *
    LiveAuction ..> User : validates bidder
```

---

### Sơ đồ lớp — Network Layer (Protocol, Controller, Server)

```mermaid
classDiagram
    direction TB

    class ActionType {
        <<enumeration>>
        PING
        LOGIN
        REGISTER
        LOGOUT
        CREATE_ITEM
        GET_MY_ITEMS
        CREATE_AUCTION
        START_AUCTION
        GET_AUCTIONS
        GET_AUCTION_DETAIL
        PLACE_BID
        ...
    }

    class Request {
        -ActionType action
        -Map~String,Object~ data
        -String token
        +getAction() ActionType
        +getData() Map
        +getToken() String
        +getDataString(String) String
        +getDataInt(String) Integer
    }

    class Response {
        -String type
        -String status
        -String event
        -String message
        -Object data
        +ok(Object) Response$
        +ok(String) Response$
        +error(String) Response$
        +push(String, Object) Response$
        +getStatus() String
        +getData() Object
    }

    class MessageSerializer {
        +serialize(Response) String$
        +deserialize(String) Request$
    }

    class AuctionServer {
        -int port
        -ServerSocket serverSocket
        -boolean isRunning
        -RequestRouter router
        +start() void
        +stop() void
    }

    class ClientHandler {
        -Socket socket
        -PrintWriter out
        -BufferedReader in
        -User currentUser
        -String token
        -RequestRouter router
        +run() void
        +sendMessage(Response) void
    }

    class RequestRouter {
        -AuthController authController
        -ItemController itemController
        -AuctionController auctionController
        +route(Request) Response
    }

    class AuthController {
        -AuthService authService
        +handleLogin(Request) Response
        +handleRegister(Request) Response
        +handleLogout(Request) Response
    }

    class AuctionController {
        -AuctionService auctionService
        -ItemService itemService
        -SessionManager sessionManager
        +handleCreateAuction(Request) Response
        +handleStartAuction(Request) Response
        +handleGetAuctions(Request) Response
        +handleGetAuctionDetail(Request) Response
    }

    class ItemController {
        -ItemService itemService
        -SessionManager sessionManager
        +handleCreateItem(Request) Response
        +handleGetMyItems(Request) Response
    }

    Request --> ActionType : has
    AuctionServer --> ClientHandler : creates *
    AuctionServer --> RequestRouter : has
    ClientHandler --> RequestRouter : delegates to
    ClientHandler --> MessageSerializer : uses
    RequestRouter --> AuthController : delegates
    RequestRouter --> AuctionController : delegates
    RequestRouter --> ItemController : delegates
    AuthController --> AuthService : uses
    AuctionController --> AuctionService : uses
    AuctionController --> ItemService : uses
    ItemController --> ItemService : uses
```

---

### Sơ đồ lớp — Service & Manager Layer

```mermaid
classDiagram
    direction TB

    class AuthService {
        -UserDAO userDAO
        -SessionManager sessionManager
        +login(String, String) LoginResult
        +register(String, String, String, UserRole) void
    }

    class AuctionService {
        -ItemService itemService
        -AuctionDAO auctionDAO
        -AuctionManager auctionManager
        +createAuction(User, String, LocalDateTime, LocalDateTime, String, String) AuctionSchema
        +startAuction(User, String) void
        +endAuction(String) void
        +getAuctions() List~AuctionSchema~
        +getAuctionsByStatus(AuctionStatus) List~AuctionSchema~
        +getAuctionById(String) AuctionSchema
    }

    class ItemService {
        -ItemDAO itemDAO
        +createItem(User, ItemDTO) ItemSchema
        +getItemsBySellerId(String) List~ItemSchema~
        +getItemById(String) ItemSchema
        +updateItem(ItemSchema) void
    }

    class SessionManager {
        <<Singleton>>
        -static final SessionManager INSTANCE
        -Map~String,User~ tokenMap
        -SessionManager()
        +getInstance() SessionManager$
        +createSession(User) String
        +validateToken(String) User
        +removeSession(String) void
        +invalidateByUserId(String) void
    }

    class LoginResult {
        -String token
        -User user
        +getToken() String
        +getUser() User
    }

    class AuctionManager {
        <<Singleton>>
        -static final AuctionManager INSTANCE
        -Map~String,LiveAuction~ liveAuctions
        -ScheduledExecutorService scheduler
        +getInstance() AuctionManager$
        +loadAuction(AuctionSchema) LiveAuction
        +getAuction(String) LiveAuction
        +endAuction(String) void
    }

    class DataManager {
        <<Singleton>>
        -static final DataManager INSTANCE
        -UserDAO userDAO
        -ItemDAO itemDAO
        -AuctionDAO auctionDAO
        -BidDAO bidDAO
        +getInstance() DataManager$
        +getUserDAO() UserDAO
        +getItemDAO() ItemDAO
        +getAuctionDAO() AuctionDAO
    }

    AuthService --> UserDAO : uses
    AuthService --> SessionManager : uses
    AuthService --> LoginResult : returns
    AuthService ..> UserMapper : uses
    AuthService ..> PasswordUtils : uses

    AuctionService --> ItemService : uses
    AuctionService --> AuctionDAO : uses
    AuctionService --> AuctionManager : uses

    ItemService --> ItemDAO : uses
    ItemService ..> ItemMapper : uses

    DataManager --> UserDAO : creates
    DataManager --> ItemDAO : creates
    DataManager --> AuctionDAO : creates
    DataManager --> BidDAO : creates
```

---

### Sơ đồ lớp — Client Side

```mermaid
classDiagram
    direction TB

    class ClientApp {
        +start(Stage) void
        +main(String[]) void
    }

    class SceneManager {
        <<Singleton>>
        -static SceneManager instance
        -Stage primaryStage
        +getInstance() SceneManager$
        +init(Stage) void
        +switchScene(String) void
    }

    class LoginController {
        -TextField usernameField
        -PasswordField passwordField
        -Label errorLabel
        +handleLogin() void
        +handleBack() void
    }

    class RegisterController {
        +handleRegister() void
        +handleBack() void
    }

    class DashboardController {
        +initialize() void
        +handleLogout() void
    }

    class CreateItemController {
        +handleCreateItem() void
    }

    class CreateAuctionController {
        +handleCreateAuction() void
        +loadMyItems() void
    }

    class AuctionListController {
        +initialize() void
        +loadAuctions() void
    }

    class ServerConnection {
        <<Singleton>>
        -static ServerConnection instance
        -Socket socket
        -PrintWriter out
        -BufferedReader in
        -Gson gson
        +getInstance() ServerConnection$
        +connect(String, int) void
        +disconnect() void
        +sendRequest(Request) Response
        +isConnected() boolean
    }

    class AuthClient {
        +login(String, String) Response
        +register(String, String, String, String) Response
        +logout(String) Response
    }

    class ItemClient {
        +createItem(...) Response
        +getMyItems(String) Response
    }

    class AuctionClient {
        +createAuction(...) Response
        +getAuctions(String) Response
        +startAuction(String, String) Response
    }

    class ClientSession {
        <<Singleton>>
        -String token
        -UserDTO currentUser
        +login(String, UserDTO) void
        +logout() void
        +getToken() String
        +getCurrentUser() UserDTO
    }

    ClientApp --> SceneManager : initializes

    LoginController --> AuthClient : uses
    LoginController --> ClientSession : updates
    LoginController --> SceneManager : navigates

    RegisterController --> AuthClient : uses
    CreateItemController --> ItemClient : uses
    CreateAuctionController --> AuctionClient : uses
    CreateAuctionController --> ItemClient : loads items
    AuctionListController --> AuctionClient : uses
    DashboardController --> ClientSession : reads

    AuthClient --> ServerConnection : sends requests
    ItemClient --> ServerConnection : sends requests
    AuctionClient --> ServerConnection : sends requests
```

---

### Sơ đồ lớp — Mapper & DTO

```mermaid
classDiagram
    direction TB

    class UserMapper {
        +toDomain(UserSchema) User$
        +toDTO(User) UserDTO$
        +toNewSchema(String, String, String, String, UserRole) UserSchema$
    }

    class ItemMapper {
        +toDomain(ItemSchema) Item$
        +toDTO(ItemSchema, String) ItemDTO$
        +toNewSchema(ItemDTO, String) ItemSchema$
        +fromRequestData(Map) ItemDTO$
    }

    class AuctionMapper {
        +toDomain(AuctionSchema) LiveAuction$
        +toDTO(AuctionSchema, ItemDTO, String, String) AuctionDTO$
        +toNewSchema(...) AuctionSchema$
        +toSchema(LiveAuction) AuctionSchema$
    }

    class BidMapper {
        +toDTO(BidRecord, String) BidDTO$
    }

    class UserDTO {
        -String id
        -String username
        -String role
        +getId() String
        +getUsername() String
        +getRole() String
    }

    class ItemDTO {
        -String id
        -String name
        -String description
        -double startingPrice
        -String type
        -String sellerUsername
        -String imageUrl
        -String condition
    }

    class AuctionDTO {
        -String id
        -ItemDTO item
        -String sellerUsername
        -String startTime
        -String endTime
        -String status
        -double currentHighestBid
        -String currentWinnerUsername
        -String title
        -String description
    }

    class BidDTO {
        -String auctionId
        -String bidderUsername
        -double amount
        -String timestamp
    }

    UserMapper ..> UserDTO : creates
    UserMapper ..> User : creates
    UserMapper ..> UserSchema : reads/creates

    ItemMapper ..> ItemDTO : creates
    ItemMapper ..> Item : creates
    ItemMapper ..> ItemSchema : reads/creates

    AuctionMapper ..> AuctionDTO : creates
    AuctionMapper ..> LiveAuction : creates
    AuctionMapper ..> AuctionSchema : reads/creates

    BidMapper ..> BidDTO : creates
```

---

## Sơ đồ luồng dữ liệu (Data Flow Diagrams)

### Luồng 1: Đăng nhập (Login Flow)

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 Người dùng
    participant LC as LoginController
    participant AC as AuthClient
    participant SC as ServerConnection
    participant CH as ClientHandler
    participant RR as RequestRouter
    participant AuthC as AuthController
    participant AS as AuthService
    participant DAO as UserDAO
    participant PU as PasswordUtils
    participant UM as UserMapper
    participant SM as SessionManager

    User->>LC: Nhập username + password, bấm Login
    LC->>LC: Validate (không để trống)
    LC->>LC: Hiện "Đang kết nối Server..."
    LC->>LC: Tạo background Thread

    rect rgba(0, 150, 255, 0.15)
        Note over LC,SC: Background Thread (tránh block UI)
        LC->>AC: login(username, password)
        AC->>AC: Tạo Request(LOGIN, {username, password})
        AC->>SC: sendRequest(request)
        SC->>SC: Serialize → JSON string
        SC->>CH: Gửi JSON qua TCP Socket
    end

    rect rgba(0, 200, 100, 0.15)
        Note over CH,SM: Server xử lý
        CH->>CH: Deserialize JSON → Request
        CH->>RR: route(request)
        RR->>RR: switch(LOGIN)
        RR->>AuthC: handleLogin(request)
        AuthC->>AuthC: Lấy username, password từ request.data
        AuthC->>AS: login(username, password)
        AS->>DAO: findByUsername(username)
        DAO->>DAO: Đọc users.json
        DAO-->>AS: UserSchema (hoặc null)

        alt User không tồn tại
            AS-->>AuthC: throw UserNotFoundException
            AuthC-->>RR: Response.error("User không tồn tại")
        else User tồn tại
            AS->>PU: verify(password, salt, hashedPassword)
            alt Sai mật khẩu
                AS-->>AuthC: throw AuthenticationException
                AuthC-->>RR: Response.error("Sai mật khẩu")
            else Đúng mật khẩu
                AS->>UM: toDomain(schema) → User object
                AS->>SM: createSession(user) → token (UUID)
                AS-->>AuthC: LoginResult(token, user)
                AuthC->>UM: toDTO(user) → UserDTO
                AuthC-->>RR: Response.ok({token, userDTO})
            end
        end
    end

    RR-->>CH: Response
    CH->>CH: Serialize Response → JSON
    CH-->>SC: Gửi JSON response
    SC-->>AC: Response object
    AC-->>LC: Response

    rect rgba(255, 100, 100, 0.15)
        Note over LC,User: Platform.runLater (UI Thread)
        alt Login thành công
            LC->>LC: Parse token + UserDTO từ response.data
            LC->>LC: ClientSession.login(token, userDTO)
            LC->>LC: SceneManager.switchScene("DashboardView")
        else Login thất bại
            LC->>LC: errorLabel.setText(response.message)
            LC->>LC: errorLabel → màu đỏ
        end
    end
```

---

### Luồng 2: Tạo sản phẩm (Create Item Flow)

```mermaid
sequenceDiagram
    autonumber
    actor Seller as 👤 Seller
    participant CIC as CreateItemController
    participant IC as ItemClient
    participant SC as ServerConnection
    participant CH as ClientHandler
    participant RR as RequestRouter
    participant ItemC as ItemController
    participant SM as SessionManager
    participant IS as ItemService
    participant IM as ItemMapper
    participant DAO as ItemDAO

    Seller->>CIC: Nhập tên, mô tả, giá, loại, ảnh → Bấm Tạo
    CIC->>CIC: Validate dữ liệu trên Client

    rect rgba(0, 150, 255, 0.15)
        Note over CIC,SC: Background Thread
        CIC->>IC: createItem(token, itemData)
        IC->>IC: Tạo Request(CREATE_ITEM, data, token)
        IC->>SC: sendRequest(request)
        SC->>CH: Gửi JSON qua TCP Socket
    end

    rect rgba(0, 200, 100, 0.15)
        Note over CH,DAO: Server xử lý
        CH->>RR: route(request)
        RR->>RR: switch(CREATE_ITEM)
        RR->>ItemC: handleCreateItem(request)

        ItemC->>SM: validateToken(token)
        alt Token không hợp lệ
            SM-->>ItemC: throw AuthenticationException
            ItemC-->>RR: Response.error("Token không hợp lệ")
        else Token hợp lệ
            SM-->>ItemC: User (Seller)
            ItemC->>IM: fromRequestData(data) → ItemDTO
            ItemC->>IS: createItem(seller, itemDTO)

            IS->>IS: seller.hasPermission("CREATE_ITEM")?
            alt Không có quyền (VD: Bidder)
                IS-->>ItemC: throw AuctionException
                ItemC-->>RR: Response.error("Không có quyền")
            else Có quyền (Seller)
                IS->>IS: Validate (name not null, price > 0)
                IS->>IM: toNewSchema(itemDTO, sellerId)
                IM->>IM: Switch itemType → tạo đúng Schema subclass
                IM-->>IS: ItemSchema (Electronics/Art/Vehicle)
                IS->>DAO: save(itemSchema)
                DAO->>DAO: Đọc items.json → thêm → ghi lại
                IS-->>ItemC: ItemSchema đã lưu
                ItemC->>IM: toDTO(schema, username)
                ItemC-->>RR: Response.ok(itemDTO)
            end
        end
    end

    RR-->>CH: Response
    CH-->>SC: JSON response
    SC-->>IC: Response
    IC-->>CIC: Response

    rect rgba(255, 100, 100, 0.15)
        Note over CIC,Seller: Platform.runLater
        alt Thành công
            CIC->>CIC: Hiện thông báo "Tạo sản phẩm thành công!"
            CIC->>CIC: Quay về Dashboard
        else Thất bại
            CIC->>CIC: Hiện lỗi từ Server
        end
    end
```

---

### Luồng 3: Tạo phiên đấu giá (Create Auction Flow)

```mermaid
sequenceDiagram
    autonumber
    actor Seller as 👤 Seller
    participant CAC as CreateAuctionController
    participant IC as ItemClient
    participant AuC as AuctionClient
    participant SC as ServerConnection
    participant CH as ClientHandler
    participant RR as RequestRouter
    participant AuCtrl as AuctionController
    participant SM as SessionManager
    participant AS as AuctionService
    participant IS as ItemService
    participant AM as AuctionManager
    participant DAO as AuctionDAO

    Note over Seller,CAC: Bước 1 - Load danh sách item của Seller
    Seller->>CAC: Mở màn hình Tạo Auction
    CAC->>IC: getMyItems(token)
    IC->>SC: Request(GET_MY_ITEMS, token)
    SC->>CH: JSON
    CH->>RR: route → ItemController.handleGetMyItems
    RR-->>CH: Response.ok(List of ItemDTO)
    CH-->>SC: JSON
    SC-->>CAC: List~ItemDTO~ → Hiển thị ComboBox

    Note over Seller,CAC: Bước 2 - Tạo Auction
    Seller->>CAC: Chọn Item, nhập thời gian, title → Bấm Tạo
    CAC->>AuC: createAuction(token, itemId, startTime, endTime, title, desc)
    AuC->>SC: Request(CREATE_AUCTION, data, token)
    SC->>CH: JSON
    CH->>RR: route(request)
    RR->>AuCtrl: handleCreateAuction(request)
    AuCtrl->>SM: validateToken(token) → User
    AuCtrl->>AS: createAuction(user, itemId, startTime, endTime, title, desc)

    AS->>AS: user.hasPermission("CREATE_AUCTION")?
    AS->>IS: getItemById(itemId)
    IS-->>AS: ItemSchema
    AS->>AS: Kiểm tra ownership (seller == item.sellerId)
    AS->>AS: Validate thời gian (end > start, start > now)
    AS->>DAO: save(AuctionSchema) - status = OPEN
    AS-->>AuCtrl: AuctionSchema

    AuCtrl-->>RR: Response.ok(AuctionDTO)
    RR-->>CH: Response
    CH-->>SC: JSON
    SC-->>CAC: Hiện thông báo thành công
```

---

### Luồng 4: Bắt đầu phiên đấu giá (Start Auction Flow)

```mermaid
sequenceDiagram
    autonumber
    actor Seller as 👤 Seller
    participant Client as Client GUI
    participant SC as ServerConnection
    participant CH as ClientHandler
    participant RR as RequestRouter
    participant AuCtrl as AuctionController
    participant SM as SessionManager
    participant AS as AuctionService
    participant DAO as AuctionDAO
    participant AM as AuctionManager
    participant LA as LiveAuction

    Seller->>Client: Bấm "Start Auction"
    Client->>SC: Request(START_AUCTION, {auctionId}, token)
    SC->>CH: JSON via TCP
    CH->>RR: route(request)
    RR->>AuCtrl: handleStartAuction(request)

    AuCtrl->>SM: validateToken(token) → User
    AuCtrl->>AS: startAuction(user, auctionId)

    AS->>DAO: findById(auctionId) → AuctionSchema
    AS->>AS: Kiểm tra ownership
    AS->>AS: Kiểm tra status == OPEN

    AS->>DAO: update(schema) → status = RUNNING
    AS->>AM: loadAuction(schema)

    rect rgba(255, 200, 0, 0.15)
        Note over AM,LA: Nạp vào RAM + Schedule auto-end
        AM->>AM: AuctionMapper.toDomain(schema) → LiveAuction
        AM->>AM: liveAuctions.put(id, auction)
        AM->>AM: scheduler.schedule(endAuction, delayMs)
    end

    AS-->>AuCtrl: Thành công
    AuCtrl-->>Client: Response.ok("Đã bắt đầu phiên đấu giá")

    Note over AM,LA: Khi hết thời gian...
    AM->>AM: endAuction(auctionId) [scheduled]
    AM->>LA: setStatus(FINISHED)
    AM->>LA: notifyAuctionEnded()
    LA->>LA: Notify all AuctionObservers
```

---

## Công nghệ & Design Patterns sử dụng

### Công nghệ

| Công nghệ | Mục đích |
|-----------|----------|
| **Java 21** | Ngôn ngữ lập trình chính |
| **JavaFX 21** | Framework GUI cho Client |
| **Maven** | Build tool, quản lý dependencies |
| **Gson** | JSON serialization/deserialization |
| **JUnit 5** | Unit testing & Integration testing |
| **TCP Socket** | Giao tiếp Client-Server |
| **GitHub Actions** | CI/CD tự động |

### Design Patterns

| Pattern | Áp dụng tại | Giải thích |
|---------|------------|------------|
| **Singleton** | `SessionManager`, `AuctionManager`, `DataManager`, `ServerConnection`, `SceneManager`, `ClientSession` | Đảm bảo chỉ có 1 instance duy nhất cho các manager/connection |
| **Observer** | `LiveAuction` + `AuctionObserver` | Thông báo realtime khi có bid mới hoặc phiên kết thúc |
| **Factory Method** | `UserMapper.toDomain()`, `ItemMapper.toNewSchema()` | Tạo đúng subclass (Bidder/Seller/Admin, Electronics/Art/Vehicle) dựa trên enum |
| **Template Method** | `BaseSchema` → các Schema con | Cung cấp cấu trúc base cho tất cả entity (id, timestamps) |
| **MVC** | Client GUI (FXML + Controller) | Tách biệt View (FXML), Controller (Java), Model (DTO) |
| **DAO** | `GenericDAO<T>`, `UserDAO`, `ItemDAO`, ... | Tách biệt logic truy xuất dữ liệu khỏi business logic |
| **DTO** | `UserDTO`, `ItemDTO`, `AuctionDTO`, `BidDTO` | Truyền dữ liệu an toàn qua mạng (không lộ password, internal state) |

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
