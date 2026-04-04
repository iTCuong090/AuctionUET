# 🏛️ Sơ đồ Luồng Quản lý Phiên đấu giá & Sản phẩm

> Tài liệu bổ trợ cho [server_architecture_overhaul.md](./server_architecture_overhaul.md)

---

## 1. Vòng đời phiên đấu giá (Auction Lifecycle)

Trạng thái của một phiên đấu giá từ khi tạo đến khi kết thúc.

```mermaid
stateDiagram-v2
    [*] --> OPEN : Seller tạo auction
    OPEN --> RUNNING : Seller bấm Start hoặc đến startTime
    RUNNING --> FINISHED : Hết thời gian (endTime)
    FINISHED --> PAID : Người thắng thanh toán
    FINISHED --> CANCELED : Không ai đặt giá / Seller hủy
    OPEN --> CANCELED : Seller hủy trước khi bắt đầu

    state OPEN {
        [*] --> Chờ_bắt_đầu
        Chờ_bắt_đầu : Auction đã tạo\nNhưng chưa mở cho bidding\nCó thể sửa/xóa item
    }

    state RUNNING {
        [*] --> Đang_đấu_giá
        Đang_đấu_giá : LiveAuction tồn tại trong RAM\nNhận bid từ Bidder\nPush realtime updates\nCountdown timer đang chạy
    }

    state FINISHED {
        [*] --> Xác_định_người_thắng
        Xác_định_người_thắng : LiveAuction bị remove khỏi RAM\nKết quả ghi vào AuctionSchema\nThông báo cho tất cả subscriber
    }
```

---

## 2. Luồng Đăng bán sản phẩm mới (Create Item)

Seller đăng một sản phẩm lên hệ thống (chưa tạo phiên đấu giá).

```mermaid
sequenceDiagram
    actor Seller
    participant CH as ClientHandler
    participant RR as RequestRouter
    participant AC as AuctionController
    participant SM as SessionManager
    participant VU as ValidationUtils
    participant Map as ItemMapper
    participant DAO as ItemDAO
    participant JF as items.json

    Seller->>CH: JSON: action=CREATE_ITEM, token, itemData
    Note over CH: itemData = {name, description, startingPrice, type, ...extraFields}
    CH->>RR: deserialize → Request
    RR->>AC: route(CREATE_ITEM)

    Note over AC,SM: Bước 1 — Xác thực & phân quyền
    AC->>SM: validateToken(token)
    SM-->>AC: User (Seller)
    AC->>AC: user.hasPermission("CREATE_ITEM") → true

    alt User không phải Seller
        AC-->>CH: Response.error("Chỉ Seller mới được đăng sản phẩm")
        CH-->>Seller: JSON: RESPONSE ERROR
    end

    Note over AC,VU: Bước 2 — Validate dữ liệu
    AC->>VU: validateItemData(itemData)
    Note over VU: Kiểm tra: name không rỗng, startingPrice > 0, type hợp lệ

    alt Validation thất bại
        AC-->>CH: Response.error("Dữ liệu sản phẩm không hợp lệ")
        CH-->>Seller: JSON: RESPONSE ERROR
    end

    Note over AC,JF: Bước 3 — Tạo Schema & Lưu DB
    AC->>Map: toNewSchema(itemData, sellerId)
    Note over Map: Tạo đúng subclass schema theo type
    Note over Map: ELECTRONICS → ElectronicsSchema
    Note over Map: ART → ArtSchema, VEHICLE → VehicleSchema
    Map-->>AC: ItemSchema (với UUID mới + timestamps)
    AC->>DAO: save(itemSchema)
    DAO->>JF: Ghi vào items.json (WRITE-THROUGH)

    Note over AC,Seller: Bước 4 — Trả response
    AC->>Map: toDTO(itemSchema)
    Map-->>AC: ItemDTO
    AC-->>CH: Response.ok(ItemDTO)
    CH-->>Seller: JSON: RESPONSE OK + ItemDTO
```

---

## 3. Luồng Tạo phiên đấu giá (Create Auction)

Seller tạo phiên đấu giá cho một sản phẩm đã đăng.

```mermaid
sequenceDiagram
    actor Seller
    participant CH as ClientHandler
    participant AC as AuctionController
    participant SM as SessionManager
    participant AServ as AuctionService
    participant IDAO as ItemDAO
    participant ADAO as AuctionDAO
    participant Map as AuctionMapper
    participant JF as auctions.json

    Seller->>CH: JSON: action=CREATE_AUCTION, token, itemId, startTime, endTime
    CH->>AC: route(CREATE_AUCTION)

    Note over AC,SM: Bước 1 — Xác thực
    AC->>SM: validateToken(token)
    SM-->>AC: User (Seller)

    Note over AServ,IDAO: Bước 2 — Validate item
    AC->>AServ: createAuction(seller, itemId, startTime, endTime)
    AServ->>IDAO: findById(itemId)
    IDAO-->>AServ: ItemSchema

    alt Item không tồn tại
        AServ-->>AC: throw AuctionException("Item không tồn tại")
        AC-->>CH: Response.error
        CH-->>Seller: JSON: RESPONSE ERROR
    end

    AServ->>AServ: Validate: item.sellerId == seller.id?
    AServ->>AServ: Validate: endTime > startTime?
    AServ->>AServ: Validate: startTime > now?

    Note over AServ,JF: Bước 3 — Tạo AuctionSchema & Lưu
    AServ->>Map: toNewSchema(itemId, sellerId, startTime, endTime)
    Map-->>AServ: AuctionSchema (status = OPEN)
    AServ->>ADAO: save(auctionSchema)
    ADAO->>JF: Ghi vào auctions.json

    Note over AC,Seller: Bước 4 — Trả response
    AServ-->>AC: AuctionSchema
    AC->>Map: toDTO(auctionSchema)
    Map-->>AC: AuctionDTO
    AC-->>CH: Response.ok(AuctionDTO)
    CH-->>Seller: JSON: RESPONSE OK + AuctionDTO
```

---

## 4. Luồng Bắt đầu phiên đấu giá (Start Auction)

Chuyển trạng thái từ OPEN → RUNNING, nạp LiveAuction vào RAM.

```mermaid
sequenceDiagram
    actor Seller
    participant CH as ClientHandler
    participant AC as AuctionController
    participant SM as SessionManager
    participant AServ as AuctionService
    participant DAO as AuctionDAO
    participant JF as auctions.json
    participant AM as AuctionManager
    participant LA as LiveAuction
    participant Map as AuctionMapper

    Seller->>CH: JSON: action=START_AUCTION, auctionId, token
    CH->>AC: route(START_AUCTION)
    AC->>SM: validateToken(token) → Seller

    Note over AServ,JF: Bước 1 — Cập nhật status trong DB
    AC->>AServ: startAuction(seller, auctionId)
    AServ->>DAO: findById(auctionId)
    DAO-->>AServ: AuctionSchema (status = OPEN)
    AServ->>AServ: Validate: seller.id == schema.sellerId
    AServ->>AServ: schema.status = RUNNING
    AServ->>DAO: update(schema)
    DAO->>JF: Ghi file (WRITE-THROUGH)

    Note over AM,LA: Bước 2 — Nạp LiveAuction vào RAM
    AServ->>AM: loadAuction(auctionId)
    AM->>Map: toDomain(auctionSchema)
    Map-->>AM: LiveAuction mới
    AM->>AM: liveAuctions.put(auctionId, liveAuction)
    Note over AM: Bắt đầu countdown timer cho endTime

    Note over AC,Seller: Bước 3 — Trả response
    AServ-->>AC: Thành công
    AC-->>CH: Response.ok("Phiên đấu giá đã bắt đầu")
    CH-->>Seller: JSON: RESPONSE OK
```

---

## 5. Luồng Kết thúc phiên đấu giá (Auction End — Tự động)

Khi hết thời gian, hệ thống tự động kết thúc phiên.

```mermaid
sequenceDiagram
    participant Timer as Countdown Timer
    participant AM as AuctionManager
    participant LA as LiveAuction
    participant CHA as ClientHandler A
    participant CHB as ClientHandler B
    participant Map as AuctionMapper
    participant DAO as AuctionDAO
    participant JF as auctions.json
    actor A as Client A
    actor B as Client B

    Note over Timer: endTime đã đến!
    Timer->>AM: endAuction(auctionId)

    Note over AM,LA: Bước 1 — Đóng phiên trong RAM
    AM->>LA: status = FINISHED
    LA->>LA: Tạo AuctionResult(winnerId, finalPrice)

    Note over LA,CHB: Bước 2 — Thông báo tất cả observers
    LA->>CHA: onAuctionEnded(result)
    LA->>CHB: onAuctionEnded(result)
    CHA-->>A: PUSH: {type: PUSH, event: AUCTION_ENDED, data: result}
    CHB-->>B: PUSH: {type: PUSH, event: AUCTION_ENDED, data: result}

    Note over AM,JF: Bước 3 — Ghi kết quả vào DB (WRITE-THROUGH)
    AM->>Map: toSchema(liveAuction)
    Map-->>AM: AuctionSchema (status=FINISHED, winnerId, highestBid)
    AM->>DAO: update(auctionSchema)
    DAO->>JF: Ghi file

    Note over AM: Bước 4 — Dọn dẹp RAM
    AM->>AM: liveAuctions.remove(auctionId)
    Note over AM: LiveAuction bị GC thu hồi
```

---

## 6. Luồng Xem danh sách phiên đấu giá (Get Auctions)

```mermaid
sequenceDiagram
    actor Client
    participant CH as ClientHandler
    participant AC as AuctionController
    participant SM as SessionManager
    participant DAO as AuctionDAO
    participant JF as auctions.json
    participant IMap as ItemMapper
    participant AMap as AuctionMapper
    participant IDAO as ItemDAO

    Client->>CH: JSON: action=GET_AUCTIONS, token
    CH->>AC: route(GET_AUCTIONS)
    AC->>SM: validateToken(token) → User

    AC->>DAO: findAll()
    DAO->>JF: Đọc file
    JF-->>DAO: List〈AuctionSchema〉
    DAO-->>AC: List〈AuctionSchema〉

    loop Mỗi AuctionSchema
        AC->>IDAO: findById(schema.itemId)
        IDAO-->>AC: ItemSchema
        AC->>IMap: toDTO(itemSchema)
        IMap-->>AC: ItemDTO
        AC->>AMap: toDTO(schema, itemDTO)
        AMap-->>AC: AuctionDTO
    end

    AC-->>CH: Response.ok(List〈AuctionDTO〉)
    CH-->>Client: JSON: RESPONSE OK + danh sách AuctionDTO
```

---

## 7. Tổng quan luồng dữ liệu toàn hệ thống

```mermaid
flowchart TB
    subgraph Client["🖥️ Client (JavaFX)"]
        UI[Giao diện người dùng]
        ML[Main Thread — gửi Request]
        NL[NotificationListener Thread — nhận Push]
    end

    subgraph Network["🌐 Network Layer"]
        SS[AuctionServer — ServerSocket]
        CH1[ClientHandler 1]
        CH2[ClientHandler 2]
        CH3[ClientHandler N...]
        RR[RequestRouter]
        UC[UserController]
        AC[AuctionController]
        BC[BidController]
    end

    subgraph Domain["⚙️ Domain Layer"]
        Auth[AuthService]
        AServ[AuctionService]
        BServ[BidService]
        SM[SessionManager — Singleton]
        AM[AuctionManager — Singleton]
        DM[DataManager — Singleton]
        LA1[LiveAuction 1]
        LA2[LiveAuction N...]
    end

    subgraph Mapper["🔄 Mapper Layer"]
        UM[UserMapper]
        IM[ItemMapper]
        AMap[AuctionMapper]
        BM[BidMapper]
    end

    subgraph Persistence["💾 Persistence Layer"]
        UDAO[UserDAO]
        IDAO[ItemDAO]
        ADAO[AuctionDAO]
        BDAO[BidDAO]
        JH[JsonFileHelper]
        UJ[(users.json)]
        IJ[(items.json)]
        AJ[(auctions.json)]
        BJ[(bids.json)]
    end

    UI --> ML
    ML --> SS
    SS --> CH1 & CH2 & CH3
    CH1 & CH2 & CH3 --> RR
    RR --> UC & AC & BC

    UC --> Auth
    AC --> AServ
    BC --> BServ

    Auth --> SM
    AServ --> AM
    BServ --> AM

    AM --> LA1 & LA2

    LA1 -.->|"Push"| CH1 & CH2
    LA2 -.->|"Push"| CH3

    Auth --> UM
    AServ --> IM & AMap
    BServ --> BM

    DM --> UDAO & IDAO & ADAO & BDAO

    UDAO --> JH --> UJ
    IDAO --> JH --> IJ
    ADAO --> JH --> AJ
    BDAO --> JH --> BJ

    CH1 -.->|"Push"| NL
    NL --> UI
```
