# 📊 Sơ đồ Cây kế thừa & Quan hệ giữa các lớp

> Tài liệu bổ trợ cho [server_architecture_overhaul.md](./server_architecture_overhaul.md)

---

## 1. Tầng Persistence — Cây kế thừa Schema

Tất cả các lớp Schema đều kế thừa từ `BaseSchema`. Cây kế thừa `Item` chỉ tồn tại ở tầng này.

```mermaid
classDiagram
    class BaseSchema {
        <<abstract>>
        #id : String
        #createdAt : LocalDateTime
        #updatedAt : LocalDateTime
        #BaseSchema()
        #BaseSchema(id, createdAt, updatedAt)
    }

    class UserSchema {
        -username : String
        -hashedPassword : String
        -passwordSalt : String
        -email : String
        -role : UserRole
    }

    class ItemSchema {
        <<abstract>>
        -name : String
        -description : String
        -startingPrice : double
        -type : ItemType
        -sellerId : String
    }

    class ElectronicsSchema {
        -brand : String
        -warranty : int
    }

    class ArtSchema {
        -artist : String
        -year : int
    }

    class VehicleSchema {
        -make : String
        -model : String
        -mileage : int
    }

    class AuctionSchema {
        -itemId : String
        -sellerId : String
        -startTime : LocalDateTime
        -endTime : LocalDateTime
        -status : AuctionStatus
        -highestBid : double
        -winnerId : String
    }

    class BidSchema {
        -auctionId : String
        -bidderId : String
        -amount : double
        -timestamp : LocalDateTime
    }

    BaseSchema <|-- UserSchema
    BaseSchema <|-- ItemSchema
    BaseSchema <|-- AuctionSchema
    BaseSchema <|-- BidSchema
    ItemSchema <|-- ElectronicsSchema
    ItemSchema <|-- ArtSchema
    ItemSchema <|-- VehicleSchema
```

---

## 2. Tầng Persistence — Cây DAO

Tất cả DAO implement `GenericDAO<T>`, chỉ làm việc với Schema.

```mermaid
classDiagram
    class GenericDAO~T extends BaseSchema~ {
        <<interface>>
        +save(T entity) void
        +findById(String id) T
        +findAll() List~T~
        +update(T entity) void
        +delete(String id) void
    }

    class UserDAO {
        +findByUsername(String username) UserSchema
    }

    class ItemDAO {
        +findBySellerId(String sellerId) List~ItemSchema~
    }

    class AuctionDAO {
        +findByStatus(AuctionStatus status) List~AuctionSchema~
    }

    class BidDAO {
        +findByAuctionId(String auctionId) List~BidSchema~
    }

    GenericDAO <|.. UserDAO
    GenericDAO <|.. ItemDAO
    GenericDAO <|.. AuctionDAO
    GenericDAO <|.. BidDAO
```

---

## 3. Tầng Domain — Cây kế thừa User

Đây là cây kế thừa thể hiện **Polymorphism chính** của hệ thống qua `hasPermission()`.

```mermaid
classDiagram
    class User {
        <<abstract>>
        -id : String
        -username : String
        -role : UserRole
        #User(id, username, role)
        +hasPermission(String action)* boolean
        +getDisplayInfo()* String
        +getId() String
        +getUsername() String
        +getRole() UserRole
    }

    class Bidder {
        +Bidder(id, username)
        +hasPermission(action) boolean
        +getDisplayInfo() String
    }

    class Seller {
        +Seller(id, username)
        +hasPermission(action) boolean
        +getDisplayInfo() String
    }

    class Admin {
        +Admin(id, username)
        +hasPermission(action) boolean
        +getDisplayInfo() String
    }

    User <|-- Bidder : role = BIDDER
    User <|-- Seller : role = SELLER
    User <|-- Admin : role = ADMIN
```

---

## 4. Tầng Domain — LiveAuction & BidRecord

`LiveAuction` là class trung tâm cho logic đấu giá realtime.

```mermaid
classDiagram
    class LiveAuction {
        -id : String
        -itemId : String
        -sellerId : String
        -endTime : LocalDateTime
        -status : AuctionStatus
        -currentHighestBid : double
        -currentWinnerId : String
        -bidHistory : List~BidRecord~
        -bidLock : ReentrantLock ❮transient❯
        -observers : List~AuctionObserver~ ❮transient❯
        +placeBid(User bidder, double amount) BidRecord
        +addObserver(AuctionObserver obs) void
        +removeObserver(AuctionObserver obs) void
        -notifyObservers(BidRecord record) void
    }

    class BidRecord {
        -bidderId : String
        -amount : double
        -timestamp : LocalDateTime
    }

    class AuctionObserver {
        <<interface>>
        +onBidPlaced(BidRecord bid) void
        +onAuctionEnded(AuctionResult result) void
    }

    LiveAuction "1" *-- "*" BidRecord : bidHistory
    LiveAuction "1" o-- "*" AuctionObserver : observers
```

---

## 5. Tầng Network — ClientHandler & Observer

`ClientHandler` vừa là Runnable (đọc request) vừa implement `AuctionObserver` (nhận push).

```mermaid
classDiagram
    class Runnable {
        <<interface>>
        +run() void
    }

    class AuctionObserver {
        <<interface>>
        +onBidPlaced(BidRecord bid) void
        +onAuctionEnded(AuctionResult result) void
    }

    class ClientHandler {
        -socket : Socket
        -out : PrintWriter
        -in : BufferedReader
        -subscribedAuctionId : String
        -currentUser : User
        +run() void
        +onBidPlaced(BidRecord bid) void «synchronized»
        +onAuctionEnded(AuctionResult result) void «synchronized»
        -sendMessage(Response res) void «synchronized»
        -cleanup() void
    }

    Runnable <|.. ClientHandler
    AuctionObserver <|.. ClientHandler
    ClientHandler --> LiveAuction : subscribes to
    ClientHandler --> User : currentUser
```

---

## 6. Tầng Transfer — DTO Classes

Các DTO đều immutable, chỉ có getter.

```mermaid
classDiagram
    class UserDTO {
        -id : String
        -username : String
        -role : UserRole
    }

    class ItemDTO {
        -id : String
        -name : String
        -description : String
        -startingPrice : double
        -type : ItemType
    }

    class AuctionDTO {
        -id : String
        -item : ItemDTO
        -sellerUsername : String
        -endTime : LocalDateTime
        -status : AuctionStatus
        -currentHighestBid : double
        -currentWinnerUsername : String
    }

    class BidDTO {
        -auctionId : String
        -bidderUsername : String
        -amount : double
        -timestamp : LocalDateTime
    }

    AuctionDTO --> ItemDTO : chứa
```

---

## 7. Toàn cảnh — Quan hệ giữa 3 tầng (qua Mapper)

Sơ đồ tổng hợp thể hiện cách 3 tầng liên kết với nhau thông qua Mapper.

```mermaid
graph TB
    subgraph "Tầng 1: Persistence"
        BS[BaseSchema]
        US[UserSchema]
        IS[ItemSchema]
        AS[AuctionSchema]
        BiS[BidSchema]
        UD[UserDAO]
        ID[ItemDAO]
        AD[AuctionDAO]
        BD[BidDAO]
        JF[(JSON Files)]
    end

    subgraph "Mapper Layer"
        UM[UserMapper]
        IM[ItemMapper]
        AM[AuctionMapper]
        BM[BidMapper]
    end

    subgraph "Tầng 2: Domain"
        U[User / Bidder / Seller / Admin]
        I[Item]
        LA[LiveAuction]
        BR[BidRecord]
        AServ[AuthService]
        SM[SessionManager]
        AucM[AuctionManager]
    end

    subgraph "Tầng 3: Network"
        UDTO[UserDTO]
        IDTO[ItemDTO]
        ADTO[AuctionDTO]
        BDTO[BidDTO]
        CH[ClientHandler]
        RR[RequestRouter]
        Ctrl[Controllers]
    end

    UD --> JF
    ID --> JF
    AD --> JF
    BD --> JF

    US --> UM
    UM --> U
    UM --> UDTO

    IS --> IM
    IM --> I
    IM --> IDTO

    AS --> AM
    AM --> LA
    AM --> ADTO

    BiS --> BM
    BM --> BR
    BM --> BDTO

    CH --> RR --> Ctrl
    Ctrl --> AServ
    Ctrl --> AucM
    AServ --> SM
    LA -->|"notifyObservers"| CH
```

---

## 8. Enums dùng chung

```mermaid
classDiagram
    class UserRole {
        <<enumeration>>
        BIDDER
        SELLER
        ADMIN
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

    class ActionType {
        <<enumeration>>
        LOGIN
        REGISTER
        LOGOUT
        GET_PROFILE
        UPDATE_PROFILE
        GET_AUCTIONS
        GET_AUCTION_DETAIL
        CREATE_AUCTION
        START_AUCTION
        SUBSCRIBE
        UNSUBSCRIBE
        PLACE_BID
        GET_BID_HISTORY
        GET_ALL_USERS
        DELETE_USER
        UPDATE_ROLE
        PING
    }
```

---

## 9. Singleton Managers

```mermaid
classDiagram
    class SessionManager {
        <<Singleton>>
        -instance : SessionManager
        -tokenMap : Map~String‚ User~
        +getInstance() SessionManager
        +createSession(User user) String
        +validateToken(String token) User
        +removeSession(String token) void
    }

    class DataManager {
        <<Singleton>>
        -instance : DataManager
        -userDAO : UserDAO
        -itemDAO : ItemDAO
        -auctionDAO : AuctionDAO
        -bidDAO : BidDAO
        +getInstance() DataManager
        +getUserDAO() UserDAO
        +getItemDAO() ItemDAO
        +getAuctionDAO() AuctionDAO
        +getBidDAO() BidDAO
    }

    class AuctionManager {
        <<Singleton>>
        -instance : AuctionManager
        -liveAuctions : Map~String‚ LiveAuction~
        +getInstance() AuctionManager
        +loadAuction(String auctionId) LiveAuction
        +getAuction(String auctionId) LiveAuction
        +endAuction(String auctionId) void
    }

    DataManager --> UserDAO
    DataManager --> ItemDAO
    DataManager --> AuctionDAO
    DataManager --> BidDAO
    AuctionManager --> LiveAuction
    SessionManager --> User
```
