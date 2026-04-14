# 🅳️ NHIỆM VỤ CHI TIẾT — ANH (Tuần 4: Domain Model + Mapper + DTO + Integration Test)

> **Tuần:** 4 · **Branch:** `feature/tuan-4-anh-auction-domain`
>
> **Tài liệu tham chiếu bắt buộc đọc trước khi code:**
> - `docs/server_architecture_overhaul.md` — Mục 4.2 (Domain), Mục 4.3 (Mapper), Mục 10.1-10.3 (Quyết định thiết kế Item)
> - `docs/diagrams_class_hierarchy.md` — Mục 3-6 (Domain User, LiveAuction, ClientHandler, DTO)
> - `docs/diagrams_flow_auction.md` — TOÀN BỘ
> - `docs/diagrams_flow_admin_user.md` — Mục 6 (Permission Matrix)
> - `planning/WEEK4/WEEK4_OVERVIEW.md` — Contracts 7, 8, 9, 10

---

## MỤC LỤC

1. [PHẦN A — Hiểu vai trò tuần 4 của Anh](#phần-a--hiểu-vai-trò-tuần-4-của-anh)
2. [PHẦN B — Nửa đầu tuần: Domain Model + Mapper + DTO](#phần-b--nửa-đầu-tuần-domain-model--mapper--dto)
3. [PHẦN C — Nửa sau tuần: AuctionManager + Integration Test](#phần-c--nửa-sau-tuần-auctionmanager--integration-test)
4. [PHẦN D — Test bắt buộc](#phần-d--test-bắt-buộc)
5. [PHẦN E — Dependency & Nhắc nhở phối hợp](#phần-e--dependency--nhắc-nhở-phối-hợp)
6. [PHẦN F — Checklist hoàn thành](#phần-f--checklist-hoàn-thành)

---

## PHẦN A — Hiểu vai trò tuần 4 của Anh

### A.1 Tuần 4 Anh làm gì?

Tuần 2-3, Anh đã xây Domain Model (User, Bidder, Seller, Admin), UserMapper, UserDTO, Utilities, Exceptions, và Integration Tests cho Auth.

Tuần 4 Anh **mở rộng Domain cho Auction Feature** + tiếp tục Integration Testing:

```
TUẦN 2-3 (đã có):                   TUẦN 4 (MỚI):
domain/                              domain/
├── model/                           ├── model/
│   ├── User ✅                      │   ├── User ✅ (không sửa)
│   ├── Bidder ✅                    │   ├── Bidder ← CẬP NHẬT hasPermission
│   ├── Seller ✅                    │   ├── Seller ← CẬP NHẬT hasPermission
│   ├── Admin ✅                     │   ├── Admin ← CẬP NHẬT hasPermission
│   ├── Item (rỗng)                  │   ├── Item ← VIẾT ĐẦY ĐỦ
│   ├── LiveAuction (rỗng)          │   ├── LiveAuction ← VIẾT ĐẦY ĐỦ
│   └── BidRecord (rỗng)            │   └── BidRecord ← VIẾT ĐẦY ĐỦ
├── enums/                           ├── enums/
│   ├── UserRole ✅                  │   ├── AuctionStatus ← VIẾT ĐẦY ĐỦ
│   ├── AuctionStatus (rỗng)        │   └── ItemType ← VIẾT ĐẦY ĐỦ
│   └── ItemType (rỗng)             │
│                                    ├── manager/
│                                    │   └── AuctionManager ← MỚI (Singleton)
│
mapper/                              mapper/
├── UserMapper ✅                    ├── ItemMapper ← VIẾT ĐẦY ĐỦ
├── ItemMapper (rỗng)               ├── AuctionMapper ← VIẾT ĐẦY ĐỦ
├── AuctionMapper (rỗng)            └── BidMapper ← VIẾT ĐẦY ĐỦ
└── BidMapper (rỗng)
                                     network/dto/
network/dto/                         ├── ItemDTO ← VIẾT ĐẦY ĐỦ
├── UserDTO ✅                       ├── AuctionDTO ← VIẾT ĐẦY ĐỦ
├── ItemDTO (rỗng)                   └── BidDTO ← VIẾT ĐẦY ĐỦ
├── AuctionDTO (rỗng)
└── BidDTO (rỗng)
```

### A.2 Quyết định thiết kế quan trọng nhắc lại

> [!IMPORTANT]
> **Item ở Domain là 1 class phẳng, KHÔNG kế thừa.**
>
> Cây kế thừa `Electronics/Art/Vehicle` chỉ tồn tại ở tầng **Persistence (Schema)**. Ở Domain, `Item` là 1 class duy nhất với `ItemType type` (enum). Lý do: logic đấu giá không phân biệt loại sản phẩm.
>
> Xem: `server_architecture_overhaul.md` → Mục 10.1, 10.3

---

## PHẦN B — Nửa đầu tuần: Domain Model + Mapper + DTO

### B.1 File: `AuctionStatus.java` (enum)

**Đường dẫn:** `domain/enums/AuctionStatus.java`

```java
public enum AuctionStatus {
    OPEN,       // Phiên đã tạo, chưa bắt đầu. Có thể sửa/xóa item
    RUNNING,    // Đang diễn ra, nhận bid từ Bidder
    FINISHED,   // Hết thời gian, đã xác định người thắng
    PAID,       // Người thắng đã thanh toán (tùy chọn, mở rộng sau)
    CANCELED    // Bị hủy bởi Seller hoặc không ai đặt giá
}
```

### B.2 File: `ItemType.java` (enum)

**Đường dẫn:** `domain/enums/ItemType.java`

```java
public enum ItemType {
    ELECTRONICS,
    ART,
    VEHICLE
}
```

---

### B.3 File: `Item.java` (Domain Model — class phẳng)

**Đường dẫn:** `domain/model/Item.java`

**Khai báo:** `public class Item` — **KHÔNG** abstract, **KHÔNG** kế thừa.

> [!NOTE]
> Khác với Schema (có cây kế thừa), Domain Item là class phẳng. Khi cần thông tin đặc thù (brand, artist...), Client lấy từ ItemDTO (mà ItemMapper tạo trực tiếp từ Schema).

**Trường dữ liệu (private final — immutable):**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `id` | `String` | ID sản phẩm (liên kết với ItemSchema.id) |
| `name` | `String` | Tên sản phẩm |
| `description` | `String` | Mô tả |
| `startingPrice` | `double` | Giá khởi điểm |
| `type` | `ItemType` | Loại sản phẩm |
| `sellerId` | `String` | ID người bán |
| `imageUrl` | `String` | URL ảnh |
| `condition` | `String` | Tình trạng |

**Constructor:** Nhận tất cả 8 tham số, gán trực tiếp. **Tất cả trường đều `final`.**

**Methods:** Chỉ getter, **KHÔNG setter** (immutable).

---

### B.4 File: `LiveAuction.java` (Domain Model — RAM-only state)

**Đường dẫn:** `domain/model/LiveAuction.java`

**Khai báo:** `public class LiveAuction`

**Vai trò:** Phiên đấu giá đang RUNNING trong RAM. Chứa cả dữ liệu nghiệp vụ lẫn trạng thái runtime (observer list, lock).

**Trường dữ liệu nghiệp vụ:**

| Trường | Kiểu | Final? | Ý nghĩa |
|--------|------|--------|---------|
| `id` | `String` | ✅ | ID phiên |
| `itemId` | `String` | ✅ | ID sản phẩm |
| `sellerId` | `String` | ✅ | ID người bán |
| `endTime` | `LocalDateTime` | ✅ | Thời gian kết thúc |
| `status` | `AuctionStatus` | ❌ | Trạng thái hiện tại (thay đổi được) |
| `currentHighestBid` | `double` | ❌ | Giá cao nhất hiện tại |
| `currentWinnerId` | `String` | ❌ | ID người đang dẫn đầu |
| `bidHistory` | `List<BidRecord>` | ✅ | Danh sách lệnh đặt giá (khởi tạo rỗng) |

**Trường runtime-only (transient):**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `bidLock` | `ReentrantLock` (final) | Đảm bảo 1 thread xử lý bid tại 1 thời điểm |
| `observers` | `List<AuctionObserver>` (final) | ClientHandler đang subscribe phiên này |

**Constructor:**
```java
public LiveAuction(String id, String itemId, String sellerId,
                   LocalDateTime endTime, AuctionStatus status,
                   double currentHighestBid, String currentWinnerId) {
    this.id = id;
    this.itemId = itemId;
    this.sellerId = sellerId;
    this.endTime = endTime;
    this.status = status;
    this.currentHighestBid = currentHighestBid;
    this.currentWinnerId = currentWinnerId;
    this.bidHistory = new ArrayList<>();
    this.bidLock = new ReentrantLock();
    this.observers = new CopyOnWriteArrayList<>(); // thread-safe list
}
```

**Method chính — `placeBid` (sẽ dùng tuần 5, nhưng VIẾT NGAY tuần 4):**

```java
public BidRecord placeBid(User bidder, double amount)
    throws InvalidBidException, AuctionClosedException {

    bidLock.lock();
    try {
        // 1. Validate
        if (status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Phiên đấu giá đã kết thúc");
        }
        if (amount <= currentHighestBid) {
            throw new InvalidBidException(
                "Giá phải lớn hơn giá hiện tại: " + currentHighestBid);
        }
        if (bidder.getId().equals(sellerId)) {
            throw new InvalidBidException("Người bán không được tự đấu giá");
        }

        // 2. Update state
        BidRecord record = new BidRecord(
            bidder.getId(), bidder.getUsername(), amount, LocalDateTime.now());
        this.currentHighestBid = amount;
        this.currentWinnerId = bidder.getId();
        this.bidHistory.add(record);

        // 3. Notify observers
        notifyObservers(record);

        return record;
    } finally {
        bidLock.unlock();
    }
}
```

**Các method Observer:**

```java
public void addObserver(AuctionObserver observer) {
    observers.add(observer);
}

public void removeObserver(AuctionObserver observer) {
    observers.remove(observer);
}

private void notifyObservers(BidRecord record) {
    for (AuctionObserver obs : observers) {
        obs.onBidPlaced(record);
    }
}

public void notifyAuctionEnded() {
    for (AuctionObserver obs : observers) {
        obs.onAuctionEnded(id, currentWinnerId, currentHighestBid);
    }
}
```

**Interface `AuctionObserver`** (tạo file riêng hoặc inner interface):

```java
public interface AuctionObserver {
    void onBidPlaced(BidRecord bid);
    void onAuctionEnded(String auctionId, String winnerId, double finalPrice);
}
```

---

### B.5 File: `BidRecord.java`

**Đường dẫn:** `domain/model/BidRecord.java`

**Khai báo:** `public class BidRecord` — immutable POJO

**Trường (private final):**

| Trường | Kiểu |
|--------|------|
| `bidderId` | `String` |
| `bidderUsername` | `String` |
| `amount` | `double` |
| `timestamp` | `LocalDateTime` |

**Constructor:** Nhận 4 tham số, gán trực tiếp.

**Methods:** Chỉ getter, không setter.

---

### B.6 Cập nhật: `Bidder.java`, `Seller.java`, `Admin.java` — hasPermission

**Cập nhật bảng quyền để hỗ trợ Auction actions:**

#### `Bidder.java`:
```java
@Override
public boolean hasPermission(String action) {
    return switch (action) {
        case "PLACE_BID", "VIEW_AUCTION", "VIEW_BID_HISTORY",
             "GET_PROFILE", "UPDATE_PROFILE", "SUBSCRIBE", "UNSUBSCRIBE"
             -> true;
        default -> false;
    };
}
```

#### `Seller.java`:
```java
@Override
public boolean hasPermission(String action) {
    return switch (action) {
        case "CREATE_ITEM", "UPDATE_ITEM", "DELETE_ITEM", "GET_MY_ITEMS",
             "CREATE_AUCTION", "START_AUCTION",
             "VIEW_AUCTION", "VIEW_BID_HISTORY",
             "GET_PROFILE", "UPDATE_PROFILE"
             -> true;
        default -> false;
    };
}
```

#### `Admin.java`:
```java
@Override
public boolean hasPermission(String action) {
    return switch (action) {
        case "MANAGE_USERS", "GET_ALL_USERS", "DELETE_USER", "UPDATE_ROLE",
             "VIEW_AUCTION", "VIEW_BID_HISTORY",
             "GET_PROFILE", "UPDATE_PROFILE"
             -> true;
        default -> false;
    };
}
```

> [!IMPORTANT]
> **Phối hợp với Khánh:** Anh cần đảm bảo action name strings trong hasPermission() **KHỚP CHÍNH XÁC** với action names mà Khánh dùng trong Controller. Nên dùng constants hoặc enum values.

---

### B.7 File: `ItemDTO.java`

**Đường dẫn:** `network/dto/ItemDTO.java`

**Khai báo:** `public class ItemDTO` — immutable

**Trường (private final):**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `id` | `String` | ID sản phẩm |
| `name` | `String` | Tên |
| `description` | `String` | Mô tả |
| `startingPrice` | `double` | Giá khởi điểm |
| `type` | `ItemType` | Loại |
| `sellerUsername` | `String` | Tên seller (để hiển thị, không dùng sellerId) |
| `imageUrl` | `String` | URL ảnh |
| `condition` | `String` | Tình trạng |
| `extraFields` | `Map<String, Object>` | Các trường đặc thù (brand, artist...) |

> [!NOTE]
> `extraFields` chứa thuộc tính riêng của từng loại. Ví dụ Electronics: `{"brand": "Apple", "warrantyMonths": 12}`. Cách này giữ ItemDTO phẳng nhưng vẫn mang đủ thông tin.

---

### B.8 File: `AuctionDTO.java`

**Khai báo:** `public class AuctionDTO` — immutable

**Trường (private final):**

| Trường | Kiểu |
|--------|------|
| `id` | `String` |
| `item` | `ItemDTO` |
| `sellerUsername` | `String` |
| `title` | `String` |
| `description` | `String` |
| `startTime` | `LocalDateTime` |
| `endTime` | `LocalDateTime` |
| `status` | `AuctionStatus` |
| `currentHighestBid` | `double` |
| `currentWinnerUsername` | `String` |

---

### B.9 File: `BidDTO.java`

**Khai báo:** `public class BidDTO` — immutable

**Trường (private final):**

| Trường | Kiểu |
|--------|------|
| `auctionId` | `String` |
| `bidderUsername` | `String` |
| `amount` | `double` |
| `timestamp` | `LocalDateTime` |

---

### B.10 File: `ItemMapper.java`

**Đường dẫn:** `mapper/ItemMapper.java`

**Methods (tất cả static):**

#### `toDomain`
```java
public static Item toDomain(ItemSchema schema) {
    return new Item(
        schema.getId(), schema.getName(), schema.getDescription(),
        schema.getStartingPrice(), schema.getType(), schema.getSellerId(),
        schema.getImageUrl(), schema.getCondition()
    );
}
```
> Đơn giản — copy các trường chung. Thuộc tính đặc thù (brand, artist...) không cần ở Domain.

#### `toDTO`
```java
public static ItemDTO toDTO(ItemSchema schema, String sellerUsername) {
    Map<String, Object> extraFields = new HashMap<>();

    if (schema instanceof ElectronicsSchema e) {
        extraFields.put("brand", e.getBrand());
        extraFields.put("warrantyMonths", e.getWarrantyMonths());
    } else if (schema instanceof ArtSchema a) {
        extraFields.put("artist", a.getArtist());
        extraFields.put("year", a.getYear());
        extraFields.put("medium", a.getMedium());
    } else if (schema instanceof VehicleSchema v) {
        extraFields.put("make", v.getMake());
        extraFields.put("model", v.getModel());
        extraFields.put("mileage", v.getMileage());
        extraFields.put("vehicleYear", v.getVehicleYear());
    }

    return new ItemDTO(
        schema.getId(), schema.getName(), schema.getDescription(),
        schema.getStartingPrice(), schema.getType(), sellerUsername,
        schema.getImageUrl(), schema.getCondition(), extraFields
    );
}
```

#### `toNewSchema`
```java
public static ItemSchema toNewSchema(Map<String, Object> data, String sellerId) {
    String typeStr = (String) data.get("type");
    ItemType type = ItemType.valueOf(typeStr);

    String id = IdGenerator.generate();
    LocalDateTime now = LocalDateTime.now();
    String name = (String) data.get("name");
    String description = (String) data.get("description");
    double startingPrice = ((Number) data.get("startingPrice")).doubleValue();
    String imageUrl = (String) data.getOrDefault("imageUrl", "");
    String condition = (String) data.getOrDefault("condition", "GOOD");

    return switch (type) {
        case ELECTRONICS -> new ElectronicsSchema(
            id, now, now, name, description, startingPrice, type, sellerId,
            imageUrl, condition, 0,
            (String) data.getOrDefault("brand", ""),
            data.containsKey("warrantyMonths")
                ? ((Number) data.get("warrantyMonths")).intValue() : 0
        );
        case ART -> new ArtSchema(
            id, now, now, name, description, startingPrice, type, sellerId,
            imageUrl, condition, 0,
            (String) data.getOrDefault("artist", ""),
            data.containsKey("year") ? ((Number) data.get("year")).intValue() : 0,
            (String) data.getOrDefault("medium", "")
        );
        case VEHICLE -> new VehicleSchema(
            id, now, now, name, description, startingPrice, type, sellerId,
            imageUrl, condition, 0,
            (String) data.getOrDefault("make", ""),
            (String) data.getOrDefault("model", ""),
            data.containsKey("mileage") ? ((Number) data.get("mileage")).intValue() : 0,
            data.containsKey("vehicleYear") ? ((Number) data.get("vehicleYear")).intValue() : 0
        );
    };
}
```

> **Đây chính là Factory Method pattern** — tạo đúng subclass dựa trên `type` enum.

---

### B.11 File: `AuctionMapper.java`

**Methods (tất cả static):**

```java
public static LiveAuction toDomain(AuctionSchema schema) {
    return new LiveAuction(
        schema.getId(), schema.getItemId(), schema.getSellerId(),
        schema.getEndTime(), schema.getStatus(),
        schema.getHighestBid(), schema.getWinnerId()
    );
}

public static AuctionDTO toDTO(AuctionSchema schema, ItemDTO itemDTO) {
    return new AuctionDTO(
        schema.getId(), itemDTO, /* sellerUsername - cần lookup */,
        schema.getTitle(), schema.getDescription(),
        schema.getStartTime(), schema.getEndTime(),
        schema.getStatus(), schema.getHighestBid(),
        /* winnerUsername - cần lookup */
    );
}

public static AuctionSchema toNewSchema(String itemId, String sellerId,
        LocalDateTime startTime, LocalDateTime endTime,
        String title, String description) {
    return new AuctionSchema(
        IdGenerator.generate(), LocalDateTime.now(), LocalDateTime.now(),
        itemId, sellerId, title, description,
        startTime, endTime, AuctionStatus.OPEN, 0.0, null
    );
}

public static AuctionSchema toSchema(LiveAuction auction) {
    // Chuyển từ RAM state về Schema để ghi file
    // Dùng khi startAuction hoặc endAuction
    AuctionSchema schema = new AuctionSchema();
    schema.setId(auction.getId());
    schema.setStatus(auction.getStatus());
    schema.setHighestBid(auction.getCurrentHighestBid());
    schema.setWinnerId(auction.getCurrentWinnerId());
    schema.setUpdatedAt(LocalDateTime.now());
    return schema;
}
```

---

### B.12 File: `BidMapper.java`

```java
public static BidDTO toDTO(BidRecord record, String auctionId) {
    return new BidDTO(auctionId, record.getBidderUsername(),
                      record.getAmount(), record.getTimestamp());
}

public static BidSchema toSchema(BidRecord record, String auctionId) {
    return new BidSchema(
        IdGenerator.generate(), LocalDateTime.now(), LocalDateTime.now(),
        auctionId, record.getBidderId(), record.getAmount(), record.getTimestamp()
    );
}
```

---

## PHẦN C — Nửa sau tuần: AuctionManager + Integration Test

### C.1 File: `AuctionManager.java` (Singleton)

**Đường dẫn:** `domain/manager/AuctionManager.java`

**Vai trò:** Singleton quản lý tất cả `LiveAuction` đang RUNNING trong RAM.

**Trường:**

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `INSTANCE` | `AuctionManager` (static final) | Singleton |
| `liveAuctions` | `ConcurrentHashMap<String, LiveAuction>` | Map auctionId → LiveAuction |
| `scheduler` | `ScheduledExecutorService` | Timer để tự kết thúc auction khi hết giờ |

**Methods:**

```java
public LiveAuction loadAuction(AuctionSchema schema) {
    LiveAuction auction = AuctionMapper.toDomain(schema);
    liveAuctions.put(auction.getId(), auction);

    // Schedule auto-end
    long delayMs = Duration.between(LocalDateTime.now(), schema.getEndTime()).toMillis();
    if (delayMs > 0) {
        scheduler.schedule(() -> endAuction(auction.getId()), delayMs, TimeUnit.MILLISECONDS);
    }

    return auction;
}

public LiveAuction getAuction(String auctionId) {
    return liveAuctions.get(auctionId);
}

public void endAuction(String auctionId) {
    LiveAuction auction = liveAuctions.remove(auctionId);
    if (auction != null) {
        auction.setStatus(AuctionStatus.FINISHED);
        auction.notifyAuctionEnded();
        // Cường's AuctionService sẽ ghi kết quả vào DB
    }
}
```

---

### C.2 Integration Test: `AuctionIntegrationTest.java`

**Test E2E qua socket (như tuần 2-3, nhưng cho Auction flow):**

```java
@Test void testFullAuctionFlow() {
    // 1. Register seller
    // 2. Login seller → lấy token
    // 3. CREATE_ITEM → nhận ItemDTO
    // 4. CREATE_AUCTION → nhận AuctionDTO (status = OPEN)
    // 5. START_AUCTION → OK (status = RUNNING)
    // 6. Register bidder
    // 7. Login bidder → lấy token
    // 8. GET_AUCTIONS → danh sách có auction vừa tạo
    // 9. GET_AUCTION_DETAIL → chi tiết đúng
}

@Test void testPermissionCreateItem_BidderDenied() {
    // 1. Register bidder, login
    // 2. CREATE_ITEM → ERROR "không có quyền"
}

@Test void testPermissionCreateAuction_BidderDenied() {
    // 1. Register bidder, login
    // 2. CREATE_AUCTION → ERROR "không có quyền"
}

@Test void testSellerCannotCreateAuctionForOtherItem() {
    // 1. Seller A tạo item
    // 2. Seller B login
    // 3. Seller B CREATE_AUCTION cho item của A → ERROR
}

@Test void testStartAuctionAlreadyRunning() {
    // 1. Seller tạo item + auction + start
    // 2. Start lần 2 → ERROR "không ở trạng thái OPEN"
}

@Test void testGetAuctionsNoAuth() {
    // 1. GET_AUCTIONS không có token → ERROR "Token không hợp lệ"
}

@Test void testCreateItemWithAllTypes() {
    // 1. Seller tạo Electronics → OK, có brand
    // 2. Seller tạo Art → OK, có artist
    // 3. Seller tạo Vehicle → OK, có make
}

@Test void testMultipleSellersCreateItems() {
    // 1. Seller A tạo 2 items
    // 2. Seller B tạo 1 item
    // 3. GET_AUCTIONS → tất cả đều hiện
}
```

---

## PHẦN D — Test bắt buộc

### D.1 Test Domain Model (JUnit)

| # | Test | Mô tả |
|---|------|-------|
| 1 | `testItemImmutable` | Tất cả trường là final, không có setter |
| 2 | `testBidRecordImmutable` | Tương tự |
| 3 | `testLiveAuctionPlaceBid` | placeBid → currentHighestBid cập nhật, bidHistory tăng size |
| 4 | `testLiveAuctionBidTooLow` | Bid ≤ currentHighest → throw InvalidBidException |
| 5 | `testLiveAuctionSellerCannotBid` | Seller bid own auction → throw |
| 6 | `testLiveAuctionClosed` | status = FINISHED → placeBid → throw AuctionClosedException |
| 7 | `testSellerPermissions` | CREATE_ITEM=true, PLACE_BID=false, MANAGE_USERS=false |
| 8 | `testBidderPermissions` | PLACE_BID=true, CREATE_ITEM=false |
| 9 | `testAdminPermissions` | MANAGE_USERS=true, CREATE_ITEM=false, PLACE_BID=false |

### D.2 Test Mapper (JUnit)

| # | Test | Mô tả |
|---|------|-------|
| 1 | `testItemMapperToDomain` | ElectronicsSchema → Item (phẳng, có type=ELECTRONICS) |
| 2 | `testItemMapperToDTO` | ElectronicsSchema → ItemDTO có extraFields.brand |
| 3 | `testItemMapperToNewSchema` | Map data(type=ART) → ArtSchema instance |
| 4 | `testAuctionMapperToDomain` | AuctionSchema → LiveAuction |
| 5 | `testAuctionMapperToDTO` | AuctionSchema + ItemDTO → AuctionDTO |
| 6 | `testBidMapperToDTO` | BidRecord → BidDTO |

### D.3 Integration Tests (JUnit)

| # | Test | Mô tả |
|---|------|-------|
| 1-8 | 8 test scenarios từ C.2 | E2E qua socket |

---

## PHẦN E — Dependency & Nhắc nhở phối hợp

### E.1 Nửa đầu tuần: Anh hầu như KHÔNG phụ thuộc ai

Enums, Domain Model, Mapper, DTO — Anh viết hoàn toàn độc lập.

**Duy nhất cần:** `IdGenerator.generate()` (Anh đã viết tuần 2) — sử dụng lại.

### E.2 Nửa đầu tuần: Cường + Khánh phụ thuộc Anh

| Họ cần | Anh cung cấp | Deadline |
|--------|-------------|---------|
| `ItemType`, `AuctionStatus` enum | Anh | **Thứ 2** (ngay đầu tuần!) |
| `ItemMapper`, `AuctionMapper` | Anh | **Thứ 4** (merge giữa tuần) |
| `ItemDTO`, `AuctionDTO`, `BidDTO` | Anh | **Thứ 4** |
| `hasPermission()` cập nhật | Anh | **Thứ 4** |

> [!WARNING]
> **Anh là DEPENDENCY chính tuần 4.** Cường cần enum để viết Schema. Khánh cần hasPermission để kiểm tra quyền. **Push enum + hasPermission trước thứ 3!**

### E.3 Nửa sau tuần: Anh cần TẤT CẢ merge xong

Integration test cần:
- Server chạy được (Cường + Khánh)
- AuctionController xử lý requests (Khánh)
- AuctionService logic (Cường)

→ Anh viết test skeleton trước, chạy thật sau khi merge.

---

## PHẦN F — Checklist hoàn thành

### Nửa đầu tuần — Deadline: Tối thứ 4

- [ ] `AuctionStatus.java` — Compile, 5 giá trị ← **Push trước thứ 3!**
- [ ] `ItemType.java` — Compile, 3 giá trị ← **Push trước thứ 3!**
- [ ] `Item.java` — Compile, immutable, 8 trường
- [ ] `LiveAuction.java` — Compile, placeBid() với ReentrantLock
- [ ] `BidRecord.java` — Compile, immutable
- [ ] `AuctionObserver.java` — Interface 2 methods
- [ ] `Bidder/Seller/Admin` — hasPermission() cập nhật action mới
- [ ] `ItemDTO.java` — Compile, immutable, có extraFields
- [ ] `AuctionDTO.java` — Compile, immutable
- [ ] `BidDTO.java` — Compile, immutable
- [ ] `ItemMapper.java` — 3 static methods (toDomain, toDTO, toNewSchema)
- [ ] `AuctionMapper.java` — 4 static methods
- [ ] `BidMapper.java` — 2 static methods
- [ ] **9 domain + 6 mapper tests ALL PASS** ✅
- [ ] Code push lên branch

### Nửa sau tuần — Deadline: Tối thứ 7

- [ ] `AuctionManager.java` — Singleton, loadAuction, endAuction, auto-timer
- [ ] `AuctionIntegrationTest.java` — **8 integration tests ALL PASS** ✅
- [ ] Test data cleanup tự động
- [ ] Code push lên branch
- [ ] Tham gia meeting cuối tuần, demo integration tests

---

> **Ghi chú:** Phần phức tạp nhất tuần 4 của Anh là `LiveAuction.placeBid()` với `ReentrantLock`. Tuy tuần 4 chưa cần gọi thực tế (PLACE_BID là tuần 5), nhưng **viết sẵn + unit test kỹ** sẽ tiết kiệm rất nhiều thời gian tuần 5. `AuctionManager` với `ScheduledExecutorService` cũng cần tự học — xem phần TỰ HỌC trong WEEK4_OVERVIEW.
