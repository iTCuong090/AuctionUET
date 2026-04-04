# 💰 Sơ đồ Luồng Đấu giá (Bidding Flows)

> Tài liệu bổ trợ cho [server_architecture_overhaul.md](./server_architecture_overhaul.md)

---

## 1. Luồng Đặt giá (Place Bid) — Critical Data Flow

Đây là luồng quan trọng nhất, cần write-through và thread-safe.

```mermaid
sequenceDiagram
    actor Bidder as Client (Bidder)
    participant CH as ClientHandler
    participant RR as RequestRouter
    participant BC as BidController
    participant SM as SessionManager
    participant AM as AuctionManager
    participant LA as LiveAuction
    participant Map as BidMapper
    participant DAO as BidDAO
    participant JF as bids.json

    Bidder->>CH: JSON: action=PLACE_BID, auctionId, amount, token
    CH->>RR: deserialize → Request
    RR->>BC: route(PLACE_BID)

    Note over BC,SM: Bước 1 — Xác thực & phân quyền
    BC->>SM: validateToken(token)
    SM-->>BC: User (Bidder)
    BC->>BC: user.hasPermission("PLACE_BID") → true

    Note over BC,LA: Bước 2 — Xử lý bid (thread-safe)
    BC->>AM: getAuction(auctionId)
    AM-->>BC: LiveAuction
    BC->>LA: placeBid(bidder, amount)

    Note over LA: 🔒 bidLock.lock()
    LA->>LA: Validate: status == RUNNING?
    LA->>LA: Validate: amount > currentHighestBid?
    LA->>LA: Validate: bidder ≠ seller?

    alt Validation thất bại
        LA-->>BC: throw InvalidBidException / AuctionClosedException
        Note over LA: 🔓 bidLock.unlock()
        BC-->>CH: Response.error(exception.message)
        CH-->>Bidder: JSON: type=RESPONSE, status=ERROR
    end

    LA->>LA: Tạo BidRecord(bidderId, amount, now)
    LA->>LA: Cập nhật currentHighestBid, currentWinnerId
    LA->>LA: bidHistory.add(record)
    LA->>LA: notifyObservers(record)
    Note over LA: 🔓 bidLock.unlock()
    LA-->>BC: BidRecord

    Note over BC,JF: Bước 3 — WRITE-THROUGH (ghi ngay vào DB)
    BC->>Map: toSchema(bidRecord, auctionId)
    Map-->>BC: BidSchema
    BC->>DAO: save(bidSchema)
    DAO->>JF: Ghi vào bids.json

    Note over BC,Bidder: Bước 4 — Trả response
    BC->>Map: toDTO(bidRecord)
    Map-->>BC: BidDTO
    BC-->>CH: Response.ok(BidDTO)
    CH-->>Bidder: JSON: type=RESPONSE, status=OK, data=BidDTO
```

---

## 2. Luồng Realtime Push — Thông báo đến các Client đang xem

Khi Bidder A đặt giá, tất cả Client đang subscribe cùng phiên sẽ nhận được push.

```mermaid
sequenceDiagram
    actor A as Client A (đặt giá)
    actor B as Client B (đang xem)
    actor C as Client C (đang xem)
    participant LA as LiveAuction
    participant CHA as ClientHandler A
    participant CHB as ClientHandler B
    participant CHC as ClientHandler C

    Note over A,LA: Client A đặt giá thành công
    A->>CHA: Request PLACE_BID 500k

    CHA->>LA: placeBid(bidderA, 500k)
    Note over LA: Lock → Validate → Update → Unlock

    Note over LA,CHC: notifyObservers() — gọi tất cả observer
    LA->>CHA: onBidPlaced(bidRecord)
    LA->>CHB: onBidPlaced(bidRecord)
    LA->>CHC: onBidPlaced(bidRecord)

    Note over CHA: «synchronized» sendMessage
    CHA-->>A: RESPONSE: {type: RESPONSE, status: OK, data: BidDTO}

    Note over CHB: «synchronized» sendMessage
    CHB-->>B: PUSH: {type: PUSH, event: BID_UPDATE, data: BidDTO}

    Note over CHC: «synchronized» sendMessage
    CHC-->>C: PUSH: {type: PUSH, event: BID_UPDATE, data: BidDTO}

    Note over B: NotificationListener nhận PUSH
    B->>B: Platform.runLater → cập nhật UI giá mới

    Note over C: NotificationListener nhận PUSH
    C->>C: Platform.runLater → cập nhật UI giá mới
```

---

## 3. Luồng Subscribe / Unsubscribe với LiveAuction

Client cần subscribe để nhận realtime push khi xem chi tiết phiên đấu giá.

```mermaid
sequenceDiagram
    actor Client
    participant CH as ClientHandler
    participant BC as AuctionController
    participant AM as AuctionManager
    participant LA as LiveAuction

    Note over Client,LA: Subscribe — Khi mở màn hình chi tiết
    Client->>CH: JSON: action=SUBSCRIBE, auctionId, token
    CH->>BC: route(SUBSCRIBE)
    BC->>AM: getAuction(auctionId)
    AM-->>BC: LiveAuction
    BC->>LA: addObserver(clientHandler)
    BC->>CH: subscribedAuctionId = auctionId
    BC-->>CH: Response.ok(AuctionDTO snapshot)
    CH-->>Client: JSON: RESPONSE OK + AuctionDTO hiện tại

    Note over Client: Từ đây, mọi bid mới sẽ được PUSH tự động

    Note over Client,LA: Unsubscribe — Khi rời màn hình
    Client->>CH: JSON: action=UNSUBSCRIBE, auctionId, token
    CH->>BC: route(UNSUBSCRIBE)
    BC->>LA: removeObserver(clientHandler)
    BC->>CH: subscribedAuctionId = null
    BC-->>CH: Response.ok
    CH-->>Client: JSON: RESPONSE OK
```

---

## 4. Xử lý Concurrent Bidding — Race Condition

Khi 2 bidder đặt giá gần như cùng lúc, `ReentrantLock` đảm bảo tính nhất quán.

```mermaid
sequenceDiagram
    actor A as Bidder A
    actor B as Bidder B
    participant LA as LiveAuction

    Note over LA: currentHighestBid = 100k

    par Gần như đồng thời
        A->>LA: placeBid(A, 150k)
        B->>LA: placeBid(B, 120k)
    end

    Note over LA: 🔒 Bidder A acquire lock TRƯỚC

    LA->>LA: Validate: 150k > 100k → ✅ OK
    LA->>LA: currentHighestBid = 150k
    LA->>LA: currentWinnerId = A
    LA->>LA: notifyObservers

    Note over LA: 🔓 Bidder A release lock

    Note over LA: 🔒 Bidder B acquire lock SAU

    LA->>LA: Validate: 120k > 150k → ❌ THẤT BẠI
    LA-->>B: throw InvalidBidException "Giá phải cao hơn 150k"

    Note over LA: 🔓 Bidder B release lock

    LA-->>A: BidRecord (thành công)
    LA-->>B: InvalidBidException (thất bại)
```

---

## 5. Luồng Xem lịch sử đấu giá (Get Bid History)

```mermaid
sequenceDiagram
    actor Client
    participant CH as ClientHandler
    participant BC as BidController
    participant SM as SessionManager
    participant DAO as BidDAO
    participant JF as bids.json
    participant Map as BidMapper

    Client->>CH: JSON: action=GET_BID_HISTORY, auctionId, token
    CH->>BC: route(GET_BID_HISTORY)
    BC->>SM: validateToken(token) → User

    BC->>DAO: findByAuctionId(auctionId)
    DAO->>JF: Đọc file
    JF-->>DAO: List of BidSchema
    DAO-->>BC: List〈BidSchema〉

    BC->>Map: toDTO(each bidSchema)
    Map-->>BC: List〈BidDTO〉

    BC-->>CH: Response.ok(List〈BidDTO〉)
    CH-->>Client: JSON: RESPONSE OK + danh sách BidDTO
```
