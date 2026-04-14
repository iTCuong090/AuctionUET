# 🅰️ NHIỆM VỤ CHI TIẾT — CƯỜNG (Tuần 4: Persistence mở rộng + AuctionService)

> **Tuần:** 4 · **Branch:** `feature/tuan-4-cuong-auction-persistence`
>
> **Tài liệu tham chiếu bắt buộc đọc trước khi code:**
> - `docs/server_architecture_overhaul.md` — Mục 3 (Package), Mục 4.1 (Persistence), Mục 10.1 (Item quyết định thiết kế)
> - `docs/diagrams_class_hierarchy.md` — Mục 1 (Schema), Mục 2 (DAO)
> - `docs/diagrams_flow_auction.md` — TOÀN BỘ (Create Item, Create/Start/End Auction, Get Auctions)
> - `planning/WEEK4/WEEK4_OVERVIEW.md` — Contracts 5, 6

---

## MỤC LỤC

1. [PHẦN A — Hiểu mở rộng Persistence Layer](#phần-a--hiểu-mở-rộng-persistence-layer)
2. [PHẦN B — Nửa đầu tuần: Schema + DAO mới](#phần-b--nửa-đầu-tuần-schema--dao-mới)
3. [PHẦN C — Nửa sau tuần: AuctionService + DataManager](#phần-c--nửa-sau-tuần-auctionservice--datamanager)
4. [PHẦN D — Test bắt buộc](#phần-d--test-bắt-buộc)
5. [PHẦN E — Dependency & Nhắc nhở phối hợp](#phần-e--dependency--nhắc-nhở-phối-hợp)
6. [PHẦN F — Checklist hoàn thành](#phần-f--checklist-hoàn-thành)

---

## PHẦN A — Hiểu mở rộng Persistence Layer

### A.1 Tuần 4 Cường làm gì?

Tuần 2-3, Cường đã xây nền tảng Persistence (`BaseSchema`, `UserSchema`, `GenericDAO`, `UserDAO`, `JsonFileHelper`, `GsonFactory`) và tầng Service (`AuthService`, `SessionManager`).

Tuần 4 **mở rộng cùng pattern** đã có để phục vụ Auction Feature:

```
TUẦN 2-3 (đã có):                   TUẦN 4 (MỚI):
├── schema/                          ├── schema/
│   ├── BaseSchema ✅                │   ├── BaseSchema ✅ (không sửa)
│   └── UserSchema ✅                │   ├── UserSchema ✅ (không sửa)
│                                    │   ├── ItemSchema (abstract) ← MỚI
│                                    │   ├── ElectronicsSchema ← MỚI
│                                    │   ├── ArtSchema ← MỚI
│                                    │   ├── VehicleSchema ← MỚI
│                                    │   ├── AuctionSchema ← MỚI
│                                    │   └── BidSchema ← MỚI
├── dao/                             ├── dao/
│   ├── GenericDAO ✅                │   ├── GenericDAO ✅ (không sửa)
│   └── UserDAO ✅                   │   ├── UserDAO ✅ (không sửa)
│                                    │   ├── ItemDAO ← MỚI
│                                    │   ├── AuctionDAO ← MỚI
│                                    │   └── BidDAO ← MỚI
├── json/                            ├── json/
│   ├── JsonFileHelper ✅            │   ├── JsonFileHelper ✅ (không sửa)
│   └── GsonFactory ✅               │   └── GsonFactory ← CẬP NHẬT (thêm ItemSchema TypeAdapter)
│
│                                    domain/service/
│                                    ├── AuctionService ← MỚI
│                                    domain/manager/
│                                    └── DataManager ← MỚI
```

### A.2 Thách thức chính tuần 4: ItemSchema và cây kế thừa

`ItemSchema` là **abstract**, có 3 subclass: `ElectronicsSchema`, `ArtSchema`, `VehicleSchema`. Mỗi subclass có trường riêng.

**Vấn đề:** Khi Gson đọc file `items.json`, nó không biết nên tạo `ElectronicsSchema` hay `ArtSchema` — vì JSON chỉ là text, không chứa thông tin class.

**Giải pháp:** Dùng `RuntimeTypeAdapterFactory` (một utility class của Gson). Nó sẽ tự thêm trường `"itemType"` vào JSON để phân biệt subclass:

```json
[
  {
    "itemType": "ElectronicsSchema",
    "name": "iPhone 15",
    "brand": "Apple",
    "warrantyMonths": 12,
    ...
  },
  {
    "itemType": "ArtSchema",
    "name": "Bức tranh Mona Lisa replica",
    "artist": "Leonardo da Vinci",
    "year": 1503,
    ...
  }
]
```

Cường cần **đăng ký** `RuntimeTypeAdapterFactory` trong `GsonFactory.create()`.

### A.3 Thuộc tính nâng cao cho sản phẩm

Ngoài các trường cơ bản (`name`, `description`, `startingPrice`), sản phẩm trên nền tảng đấu giá chuyên nghiệp cần thêm:

| Trường | Kiểu | Ý nghĩa | Trong class nào |
|--------|------|---------|----------------|
| `imageUrl` | `String` | Đường dẫn/URL ảnh sản phẩm | `ItemSchema` (chung) |
| `condition` | `String` | Tình trạng: NEW, LIKE_NEW, GOOD, FAIR, POOR | `ItemSchema` (chung) |
| `sellerId` | `String` | ID người bán (liên kết ownership) | `ItemSchema` (chung) |
| `auctionCount` | `int` | Số phiên đấu giá đã tạo cho item này | `ItemSchema` (chung) |
| `brand` | `String` | Thương hiệu | `ElectronicsSchema` |
| `warrantyMonths` | `int` | Số tháng bảo hành | `ElectronicsSchema` |
| `artist` | `String` | Tên nghệ sĩ | `ArtSchema` |
| `year` | `int` | Năm sáng tác | `ArtSchema` |
| `medium` | `String` | Chất liệu: Sơn dầu, Acrylic, Bút chì... | `ArtSchema` |
| `make` | `String` | Hãng xe | `VehicleSchema` |
| `model` | `String` | Dòng xe | `VehicleSchema` |
| `mileage` | `int` | Số km đã đi | `VehicleSchema` |
| `vehicleYear` | `int` | Năm sản xuất | `VehicleSchema` |

---

## PHẦN B — Nửa đầu tuần: Schema + DAO mới

### Package đích (bổ sung vào persistence/):
```
src/main/java/com/auctionuet/server/persistence/
├── schema/
│   ├── BaseSchema.java          ← ĐÃ CÓ, không sửa
│   ├── UserSchema.java          ← ĐÃ CÓ, không sửa
│   ├── ItemSchema.java          ← MỚI (abstract)
│   ├── ElectronicsSchema.java   ← MỚI
│   ├── ArtSchema.java           ← MỚI
│   ├── VehicleSchema.java       ← MỚI
│   ├── AuctionSchema.java       ← MỚI
│   └── BidSchema.java           ← MỚI
├── dao/
│   ├── GenericDAO.java          ← ĐÃ CÓ, không sửa
│   ├── UserDAO.java             ← ĐÃ CÓ, không sửa
│   ├── ItemDAO.java             ← MỚI
│   ├── AuctionDAO.java          ← MỚI
│   └── BidDAO.java              ← MỚI
└── json/
    ├── JsonFileHelper.java      ← ĐÃ CÓ, không sửa
    └── GsonFactory.java         ← CẬP NHẬT
```

---

### B.1 File: `ItemSchema.java` (abstract)

**Đường dẫn:** `persistence/schema/ItemSchema.java`

**Vai trò:** Lớp cha trừu tượng cho TẤT CẢ schema sản phẩm. Chứa những trường chung mà Electronics, Art, Vehicle đều có.

**Khai báo:**
```
public abstract class ItemSchema extends BaseSchema
```

**Trường dữ liệu (private):**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `name` | `String` | Tên sản phẩm |
| `description` | `String` | Mô tả chi tiết |
| `startingPrice` | `double` | Giá khởi điểm (VNĐ) |
| `type` | `ItemType` | Enum: ELECTRONICS, ART, VEHICLE |
| `sellerId` | `String` | ID người bán (liên kết 1:1 với UserSchema.id) |
| `imageUrl` | `String` | URL/đường dẫn ảnh sản phẩm |
| `condition` | `String` | Tình trạng: NEW, LIKE_NEW, GOOD, FAIR, POOR |
| `auctionCount` | `int` | Số phiên đấu giá đã tạo cho item này (mặc định 0) |

**Constructor:**
1. **Constructor rỗng:** `protected ItemSchema()` — cho Gson
2. **Constructor tường minh:** nhận tất cả tham số BaseSchema (id, createdAt, updatedAt) + 7 trường riêng

**Methods:** Getter + Setter cho tất cả trường.

---

### B.2 File: `ElectronicsSchema.java`

**Khai báo:** `public class ElectronicsSchema extends ItemSchema`

**Trường bổ sung (private):**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `brand` | `String` | Thương hiệu (Apple, Samsung...) |
| `warrantyMonths` | `int` | Số tháng bảo hành còn lại |

**Constructor:**
1. `protected ElectronicsSchema()` — cho Gson
2. Constructor tường minh: nhận tất cả tham số của cha + brand + warrantyMonths

---

### B.3 File: `ArtSchema.java`

**Khai báo:** `public class ArtSchema extends ItemSchema`

**Trường bổ sung (private):**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `artist` | `String` | Tên nghệ sĩ |
| `year` | `int` | Năm sáng tác |
| `medium` | `String` | Chất liệu (sơn dầu, acrylic...) |

---

### B.4 File: `VehicleSchema.java`

**Khai báo:** `public class VehicleSchema extends ItemSchema`

**Trường bổ sung (private):**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `make` | `String` | Hãng xe (Toyota, Honda...) |
| `model` | `String` | Dòng xe (Camry, Civic...) |
| `mileage` | `int` | Số km đã đi |
| `vehicleYear` | `int` | Năm sản xuất |

---

### B.5 File: `AuctionSchema.java`

**Đường dẫn:** `persistence/schema/AuctionSchema.java`

**Khai báo:** `public class AuctionSchema extends BaseSchema`

**Trường dữ liệu (private):**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `itemId` | `String` | ID sản phẩm đấu giá |
| `sellerId` | `String` | ID người bán |
| `title` | `String` | Tiêu đề phiên đấu giá |
| `description` | `String` | Mô tả phiên |
| `startTime` | `LocalDateTime` | Thời gian bắt đầu dự kiến |
| `endTime` | `LocalDateTime` | Thời gian kết thúc dự kiến |
| `status` | `AuctionStatus` | Trạng thái: OPEN → RUNNING → FINISHED/CANCELED |
| `highestBid` | `double` | Giá cao nhất hiện tại (mặc định 0) |
| `winnerId` | `String` | ID người thắng (null nếu chưa có) |

**Constructor:**
1. `protected AuctionSchema()` — cho Gson
2. Constructor tường minh nhận tất cả tham số

---

### B.6 File: `BidSchema.java`

**Khai báo:** `public class BidSchema extends BaseSchema`

**Trường dữ liệu (private):**

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `auctionId` | `String` | ID phiên đấu giá |
| `bidderId` | `String` | ID người đặt giá |
| `amount` | `double` | Số tiền đặt |
| `timestamp` | `LocalDateTime` | Thời điểm đặt giá |

---

### B.7 File: `ItemDAO.java`

**Khai báo:** `public class ItemDAO implements GenericDAO<ItemSchema>`

> [!WARNING]
> **Lưu ý đặc biệt:** Vì `ItemSchema` là abstract, Gson cần `RuntimeTypeAdapterFactory` để deserialize đúng subclass. `JsonFileHelper.readList` hiện nhận `Class<T>`, nhưng `ItemSchema.class` không đủ — Gson cần biết đó là `ElectronicsSchema` hay `ArtSchema`.
>
> **Giải pháp:** Trong `GsonFactory`, đăng ký `RuntimeTypeAdapterFactory` cho `ItemSchema`. Khi đó `readList("items.json", ItemSchema.class)` sẽ tự tạo đúng subclass dựa trên trường `"itemType"` trong JSON.

**Trường:**
- `filePath` — mặc định `"data/items.json"`

**Methods**: Implement tất cả 5 methods từ `GenericDAO` + methods riêng:

| Method riêng | Mô tả |
|-------------|--------|
| `List<ItemSchema> findBySellerId(String sellerId)` | Tìm tất cả item của 1 seller |
| `List<ItemSchema> findByType(ItemType type)` | Tìm item theo loại |

---

### B.8 File: `AuctionDAO.java`

**Khai báo:** `public class AuctionDAO implements GenericDAO<AuctionSchema>`

**Trường:**
- `filePath` — mặc định `"data/auctions.json"`

**Methods riêng:**

| Method | Mô tả |
|--------|--------|
| `List<AuctionSchema> findByStatus(AuctionStatus status)` | Lọc theo trạng thái |
| `List<AuctionSchema> findBySellerId(String sellerId)` | Tìm tất cả auction của 1 seller |
| `AuctionSchema findByItemId(String itemId)` | Tìm auction theo item (có thể null) |

---

### B.9 File: `BidDAO.java`

**Khai báo:** `public class BidDAO implements GenericDAO<BidSchema>`

**Trường:**
- `filePath` — mặc định `"data/bids.json"`

**Methods riêng:**

| Method | Mô tả |
|--------|--------|
| `List<BidSchema> findByAuctionId(String auctionId)` | Lịch sử bid cho 1 phiên |

---

### B.10 Cập nhật: `GsonFactory.java`

**Thêm `RuntimeTypeAdapterFactory` cho `ItemSchema`:**

```java
public static Gson create() {
    RuntimeTypeAdapterFactory<ItemSchema> itemFactory =
        RuntimeTypeAdapterFactory.of(ItemSchema.class, "itemType")
            .registerSubtype(ElectronicsSchema.class, "ELECTRONICS")
            .registerSubtype(ArtSchema.class, "ART")
            .registerSubtype(VehicleSchema.class, "VEHICLE");

    return new GsonBuilder()
        .registerTypeAdapterFactory(itemFactory)
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
        .setPrettyPrinting()
        .create();
}
```

> [!WARNING]
> `RuntimeTypeAdapterFactory` **KHÔNG nằm** trong Gson core. Nó nằm trong package `com.google.gson.typeadapters`. Cường cần thêm dependency hoặc copy class này vào project. Kiểm tra `pom.xml`, nếu chưa có:
> ```xml
> <dependency>
>     <groupId>com.google.code.gson</groupId>
>     <artifactId>gson-extras</artifactId>
>     <version>2.8.5</version>
> </dependency>
> ```
> Hoặc copy file `RuntimeTypeAdapterFactory.java` từ GitHub Gson vào `persistence/json/`.

---

## PHẦN C — Nửa sau tuần: AuctionService + DataManager

### C.1 File: `DataManager.java` (Singleton)

**Đường dẫn:** `domain/manager/DataManager.java`

**Vai trò:** Singleton quản lý TẤT CẢ DAO instances. Mọi Service đều lấy DAO qua đây, không tự `new UserDAO()`.

**Khai báo:**
```
public class DataManager
```

**Trường:**

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `INSTANCE` | `DataManager` (static final) | Singleton instance |
| `userDAO` | `UserDAO` (final) | Quản lý users.json |
| `itemDAO` | `ItemDAO` (final) | Quản lý items.json |
| `auctionDAO` | `AuctionDAO` (final) | Quản lý auctions.json |
| `bidDAO` | `BidDAO` (final) | Quản lý bids.json |

**Constructor:** `private DataManager()` — khởi tạo tất cả DAO.

**Methods:**
- `static DataManager getInstance()`
- `getUserDAO()`, `getItemDAO()`, `getAuctionDAO()`, `getBidDAO()`

---

### C.2 File: `AuctionService.java`

**Đường dẫn:** `domain/service/AuctionService.java`

**Vai trò:** Business logic cho toàn bộ luồng Auction: tạo item, tạo/bắt đầu/kết thúc phiên đấu giá, xem danh sách.

**Trường:**

| Trường | Kiểu | Nguồn |
|--------|------|-------|
| `itemDAO` | `ItemDAO` | Qua DataManager hoặc constructor (DI) |
| `auctionDAO` | `AuctionDAO` | Qua DataManager |
| `auctionManager` | `AuctionManager` | `AuctionManager.getInstance()` |

**Constructor:**
```
public AuctionService(ItemDAO itemDAO, AuctionDAO auctionDAO)
```

---

#### Method 1: `createItem`

```
public ItemSchema createItem(User seller, Map<String, Object> itemData)
    throws AuctionException, IllegalArgumentException
```

**Luồng xử lý:**

| Bước | Hành động | Gọi đến |
|------|-----------|---------|
| 1 | Kiểm tra quyền: seller.hasPermission("CREATE_ITEM") | User.hasPermission() |
| 2 | Validate dữ liệu: name không rỗng, startingPrice > 0, type hợp lệ | ValidationUtils (nếu có) |
| 3 | Tạo ItemSchema qua Mapper | `ItemMapper.toNewSchema(itemData, seller.getId())` |
| 4 | Lưu DB (WRITE-THROUGH) | `itemDAO.save(schema)` |
| 5 | Trả về ItemSchema | — |

---

#### Method 2: `createAuction`

```
public AuctionSchema createAuction(User seller, String itemId,
    LocalDateTime startTime, LocalDateTime endTime, String title, String description)
    throws AuctionException
```

**Luồng xử lý:**

| Bước | Hành động | Gọi đến |
|------|-----------|---------|
| 1 | Kiểm tra quyền: seller.hasPermission("CREATE_AUCTION") | — |
| 2 | Tìm item | `itemDAO.findById(itemId)` |
| 3 | Nếu null → throw | `AuctionException("Item không tồn tại")` |
| 4 | Validate: item.getSellerId() == seller.getId() | Ownership check |
| 5 | Validate: endTime > startTime | — |
| 6 | Validate: startTime > now | — |
| 7 | Tạo AuctionSchema qua Mapper | `AuctionMapper.toNewSchema(...)` |
| 8 | Lưu DB (WRITE-THROUGH) | `auctionDAO.save(schema)` |
| 9 | Cập nhật item.auctionCount += 1 | `itemDAO.update(item)` |
| 10 | Trả về AuctionSchema | — |

---

#### Method 3: `startAuction`

```
public void startAuction(User seller, String auctionId) throws AuctionException
```

**Luồng xử lý:**

| Bước | Hành động |
|------|-----------|
| 1 | Tìm auction: `auctionDAO.findById(auctionId)` |
| 2 | Nếu null → throw |
| 3 | Validate: schema.getSellerId() == seller.getId() |
| 4 | Validate: schema.getStatus() == OPEN |
| 5 | Cập nhật status → RUNNING |
| 6 | `auctionDAO.update(schema)` (WRITE-THROUGH) |
| 7 | Nạp LiveAuction vào RAM: `AuctionManager.getInstance().loadAuction(schema)` |

---

#### Method 4: `endAuction`

```
public void endAuction(String auctionId) throws AuctionException
```

**Luồng:**

| Bước | Hành động |
|------|-----------|
| 1 | Lấy LiveAuction từ AuctionManager |
| 2 | Set status = FINISHED |
| 3 | Ghi kết quả vào DB: `AuctionMapper.toSchema(liveAuction)` → `auctionDAO.update(...)` |
| 4 | Remove khỏi RAM: `AuctionManager.getInstance().endAuction(auctionId)` |

---

#### Method 5: `getAuctions`

```
public List<AuctionSchema> getAuctions()
public List<AuctionSchema> getAuctionsByStatus(AuctionStatus status)
public List<AuctionSchema> getAuctionsBySeller(String sellerId)
```

**Logic đơn giản:** Gọi DAO tương ứng, trả kết quả.

---

## PHẦN D — Test bắt buộc

### D.1 Test Schema + DAO (JUnit — `ItemDAOTest.java`, `AuctionDAOTest.java`)

| # | Test name | Mô tả |
|---|-----------|-------|
| 1 | `testSaveAndFindElectronics` | Save ElectronicsSchema → findById → đúng brand, warrantyMonths |
| 2 | `testSaveAndFindArt` | Save ArtSchema → findById → đúng artist, year |
| 3 | `testSaveAndFindVehicle` | Save VehicleSchema → findById → đúng make, model |
| 4 | `testItemPolymorphism` | Save 1 Electronics + 1 Art → findAll → 2 items đúng subclass |
| 5 | `testFindBySellerId` | Save 3 items (2 seller A, 1 seller B) → findBySellerId(A) → 2 items |
| 6 | `testAuctionSaveAndFind` | Save AuctionSchema → findById → đúng status, itemId |
| 7 | `testFindByStatus` | Save 3 auctions (2 OPEN, 1 RUNNING) → findByStatus(OPEN) → 2 |
| 8 | `testGsonItemSubclass` | Serialize ElectronicsSchema → JSON → deserialize → vẫn là ElectronicsSchema |
| 9 | `testBidSaveAndFindByAuction` | Save 3 bids (2 auction A, 1 auction B) → findByAuctionId(A) → 2 |

### D.2 Test AuctionService (JUnit — `AuctionServiceTest.java`)

| # | Test name | Mô tả |
|---|-----------|-------|
| 1 | `testCreateItemBySeller` | Seller tạo item → thành công, lưu vào file |
| 2 | `testCreateItemByBidder` | Bidder tạo item → throw (không có quyền) |
| 3 | `testCreateAuction` | Seller tạo auction cho item của mình → thành công |
| 4 | `testCreateAuctionWrongOwner` | Seller A tạo auction cho item của Seller B → throw |
| 5 | `testStartAuction` | Start auction → status = RUNNING |
| 6 | `testStartAuctionAlreadyRunning` | Start auction đã RUNNING → throw |
| 7 | `testGetAuctions` | Tạo 3 auction → getAuctions → 3 kết quả |
| 8 | `testEndAuction` | End auction → status = FINISHED, ghi file |

---

## PHẦN E — Dependency & Nhắc nhở phối hợp

### E.1 Nửa đầu tuần: Cường KHÔNG phụ thuộc ai

Schema + DAO hoàn toàn độc lập. Cường chỉ cần `ItemType` enum (của Anh) và `AuctionStatus` enum (của Anh).

**Nếu Anh chưa push enum:**
- Cường tự tạo tạm: `ItemType { ELECTRONICS, ART, VEHICLE }` và `AuctionStatus { OPEN, RUNNING, FINISHED, PAID, CANCELED }`
- Xóa bản tạm khi merge

### E.2 Nửa sau tuần: Cường phụ thuộc Anh (nửa đầu)

| Cường cần | Anh cung cấp | Nếu Anh chưa xong? |
|-----------|-------------|---------------------|
| `ItemMapper.toNewSchema()` | `mapper/ItemMapper.java` | Cường tự tạo schema trực tiếp |
| `AuctionMapper.toNewSchema()` | `mapper/AuctionMapper.java` | Cường tự tạo schema trực tiếp |
| `AuctionMapper.toSchema(LiveAuction)` | `mapper/AuctionMapper.java` | Tạm bỏ qua, test thủ công |
| `AuctionManager.loadAuction()` | `domain/manager/AuctionManager.java` | Mock: tạm chỉ gọi DAO |

### E.3 Khánh phụ thuộc Cường (nửa sau)

| Khánh cần | Cường cung cấp | Deadline |
|-----------|----------------|---------|
| `AuctionService.createItem()` | `domain/service/AuctionService.java` | **Thứ 5** |
| `AuctionService.createAuction()` | Tương tự | **Thứ 5** |
| `AuctionService.startAuction()` | Tương tự | **Thứ 5** |
| `AuctionService.getAuctions()` | Tương tự | **Thứ 5** |

> [!WARNING]
> **AuctionService là CRITICAL PATH tuần 4.** Cường phải push trước thứ 5 để Khánh kết nối Controller.

---

## PHẦN F — Checklist hoàn thành

### Nửa đầu tuần — Deadline: Tối thứ 4

- [ ] `ItemSchema.java` (abstract) — Compile, extends BaseSchema, đủ 8 trường + getter/setter
- [ ] `ElectronicsSchema.java` — Compile, extends ItemSchema, 2 trường riêng
- [ ] `ArtSchema.java` — Compile, extends ItemSchema, 3 trường riêng
- [ ] `VehicleSchema.java` — Compile, extends ItemSchema, 4 trường riêng
- [ ] `AuctionSchema.java` — Compile, extends BaseSchema, 9 trường
- [ ] `BidSchema.java` — Compile, extends BaseSchema, 4 trường
- [ ] `ItemDAO.java` — Compile, implements GenericDAO + findBySellerId + findByType
- [ ] `AuctionDAO.java` — Compile, implements GenericDAO + findByStatus + findBySellerId
- [ ] `BidDAO.java` — Compile, implements GenericDAO + findByAuctionId
- [ ] `GsonFactory.java` — Cập nhật RuntimeTypeAdapterFactory cho ItemSchema
- [ ] **9 Schema/DAO tests ALL PASS** ✅
- [ ] Code push lên branch

### Nửa sau tuần — Deadline: Tối thứ 7

- [ ] `DataManager.java` — Compile, Singleton, quản lý 4 DAO
- [ ] `AuctionService.java` — Compile, 5 methods logic hoàn chỉnh
- [ ] AuctionService push lên branch trước **thứ 5** để Khánh dùng
- [ ] **8 AuctionService tests ALL PASS** ✅
- [ ] Tham gia meeting cuối tuần, demo live

---

> **Ghi chú:** Pattern giống hệt tuần 2. Nếu Cường làm tốt `UserDAO`, thì `ItemDAO`, `AuctionDAO`, `BidDAO` chỉ cần copy-paste + đổi tên. Phần khó nhất tuần 4 là `RuntimeTypeAdapterFactory` cho cây kế thừa Item — dành thời gian tự học phần này trước.
