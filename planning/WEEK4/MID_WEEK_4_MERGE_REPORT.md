# 📋 Báo Cáo Merge Tuần 4 - `develop-week-4`

> **Ngày thực hiện**: 2026-04-16  
> **Nhánh đích**: `develop-week-4`  
> **Trạng thái**: ✅ **HOÀN TẤT** — Đã push lên remote

---

## 📦 Tổng Quan Merge

| # | Nhánh | Tác giả | Kết quả | Xung đột |
|---|-------|---------|---------|-----------|
| 1 | `feature/tuan-4-cuong-auction-persistence` | iTCuong090 | ✅ Fast-forward | Không |
| 2 | `feature/tuan-4-anh-auction-domain` | Danh25020015 | ⚠️ Có xung đột | **5 xung đột** |
| 3 | `feature/tuan-4-khanh-auction-network` | Khanh | ✅ Auto merge | Không |
| 4 | `feature/tuan-4-cong-auction-gui` | 25020046-nguyencaocong | ✅ Auto merge | Không |

---

## 🔀 Chi Tiết Từng Merge

### Merge 1: `feature/tuan-4-cuong-auction-persistence` ✅

**Commits**: 2 (`ffd620d`, `e4c7327`)

Merge dạng **fast-forward** (không có thay đổi nào trên `develop-week-4` trước đó). Thêm:
- 20 files changed, +1450 lines
- Triển khai đầy đủ `AuctionDAO`, `BidDAO`, `ItemDAO` với logic CRUD
- Thêm polymorphic schemas: `ArtSchema`, `ElectronicsSchema`, `VehicleSchema`
- Thêm `RuntimeTypeAdapterFactory` cho Gson deserialization
- Thêm `AuctionService` business logic
- Thêm 4 bộ test: `AuctionServiceTest`, `AuctionDAOTest`, `BidDAOTest`, `ItemDAOTest`

---

### Merge 2: `feature/tuan-4-anh-auction-domain` ⚠️ CÓ XUNG ĐỘT

**Commits**: 1 (`2917ce2`)

#### Xung đột 1: `AuctionStatus.java` — Content Conflict

```diff
- // HEAD (persistence): giá trị enum trên 1 dòng
- OPEN, RUNNING, FINISHED, PAID, CANCELED

+ // domain: giá trị enum có comment tiếng Việt
+ OPEN,       // Phiên đã tạo, chưa bắt đầu
+ RUNNING,    // Đang diễn ra, nhận bid từ Bidder
+ FINISHED,   // Hết thời gian, đã xác định người thắng
+ PAID,       // Người thắng đã thanh toán
+ CANCELED    // Bị hủy bởi Seller hoặc không ai đặt giá
```

> **Giải quyết**: Chọn phiên bản domain (có comment) — cùng giá trị enum nhưng có tài liệu tốt hơn.

#### Xung đột 2: `ItemType.java` — Content Conflict

```diff
- // HEAD (persistence): giá trị enum trên 1 dòng
- ELECTRONICS, ART, VEHICLE

+ // domain: giá trị enum có comment
+ ELECTRONICS, // Đồ điện tử
+ ART,         // Đồ nghệ thuật
+ VEHICLE      // Phương tiện
```

> **Giải quyết**: Tương tự, chọn phiên bản domain có comment.

#### Xung đột 3-5: `AuctionDAO.java`, `BidDAO.java`, `ItemDAO.java` — Modify/Delete Conflict

Đây là xung đột nghiêm trọng nhất:

| Vấn đề | Nhánh `domain` | Nhánh `persistence` |
|--------|----------------|---------------------|
| Hành động | **Xóa** file ở `persistence/dao/` và tạo **stub rỗng** ở `persistence/schema/dao/` | **Triển khai đầy đủ** logic CRUD ở `persistence/dao/` |
| Nội dung | Class rỗng (0 logic) | 75-97 dòng code với đầy đủ CRUD + query methods |

> **Giải quyết**: 
> - **Giữ** phiên bản đầy đủ từ `persistence` ở vị trí `persistence/dao/`
> - **Xóa** các file stub rỗng ở `persistence/schema/dao/` (AuctionDAO, BidDAO, ItemDAO)
> - Nhánh domain cũng đã di chuyển `GenericDAO` và `UserDAO` → giữ vị trí của Cường như ban đầu.

---

### Merge 3: `feature/tuan-4-khanh-auction-network` ✅

**Commits**: 1 (`c166eba`)

Merge tự động thành công. Thêm:
- `AuctionController.java` — Controller xử lý auction requests (+67 lines)
- Mở rộng `ActionType.java` — Thêm các action types mới (+22 lines)

---

### Merge 4: `feature/tuan-4-cong-auction-gui` ✅

**Commits**: 1 (`9b5de72`)

Merge tự động thành công. Thêm:
- 15 files, +1427 lines
- `ClientSession.java` — Quản lý session phía client
- 4 Controller mới: `AuctionDetailController`, `AuctionListController`, `CreateAuctionController`, `CreateItemController`
- Cập nhật `DashboardController` và `LoginController`
- 4 FXML views mới + cập nhật CSS styles

---

## 🐛 Lỗi Biên Dịch Sau Merge

Sau khi merge xong 4 nhánh, **9 lỗi biên dịch** xuất hiện:

### Lỗi 1: `DataManager.java` — Sai import path `UserDAO` (4 errors)

```
cannot find symbol: class UserDAO
location: package com.auctionuet.server.persistence.dao
```

**Nguyên nhân**: Nhánh `domain` di chuyển `UserDAO` từ `persistence.dao` → `persistence.schema.dao`, nhưng `DataManager` vẫn import từ package cũ.

**Fix**: Đổi import path:
```diff
- import com.auctionuet.server.persistence.dao.UserDAO;
+ import com.auctionuet.server.persistence.schema.dao.UserDAO;
```

### Lỗi 2: `LiveAuction.java` — Thiếu interface `AuctionObserver` (5 errors)

```
cannot find symbol: class AuctionObserver
location: class com.auctionuet.server.domain.model.LiveAuction
```

**Nguyên nhân**: Nhánh `domain` thêm code Observer pattern trong `LiveAuction` nhưng **quên tạo** interface `AuctionObserver`.

**Fix**: Tạo mới [AuctionObserver.java](file:///d:/Project/AuctionUET/auction-server/src/main/java/com/auctionuet/server/domain/model/AuctionObserver.java) với 2 methods:
- `void onBidPlaced(BidRecord record)` — Khi có bid mới
- `void onAuctionEnded(String auctionId, String winnerId, double finalPrice)` — Khi kết thúc

### Lỗi 3: Thiếu import `GenericDAO` trong 3 DAO files (ẩn, sửa preventive)

**Nguyên nhân**: `GenericDAO` di chuyển sang package `persistence.schema.dao` nhưng các DAO ở `persistence.dao` dùng nó qua cùng package (không cần import). Sau khi `GenericDAO` đổi package → cần import explicit.

**Fix**: Thêm import cho cả 3 files:
```java
import com.auctionuet.server.persistence.schema.dao.GenericDAO;
```

---

## ✅ Kết Quả Cuối Cùng

| Metric | Kết quả |
|--------|---------|
| Server compilation | ✅ BUILD SUCCESS |
| Client compilation | ✅ BUILD SUCCESS |
| Server tests | ✅ **60/60 passed** (0 failures, 0 errors) |
| Push to remote | ✅ `7ad0746..80067ce develop-week-4 -> develop-week-4` |

### Git Log Sau Merge

```
80067ce fix: resolve post-merge compilation errors
0e2ef7d Merge feature/tuan-4-cong-auction-gui
6b8df0f Merge feature/tuan-4-khanh-auction-network
54c4708 Merge feature/tuan-4-anh-auction-domain (resolve conflicts)
e4c7327 fix DAO not using stream
ffd620d feat(persistence): implement item schema poly and auction service logic
```

---

## ⚠️ Lưu Ý Cho Team

> [!WARNING]
> **Vấn đề cấu trúc package DAO**: Hiện tại có 2 vị trí chứa DAO:
> - `persistence/dao/` — chứa `AuctionDAO`, `BidDAO`, `ItemDAO` (có logic)
> - `persistence/schema/dao/` — chứa `GenericDAO`, `UserDAO`
> 
> Team nên thống nhất đưa tất cả DAO về **một package duy nhất** trong sprint tiếp theo.

> [!IMPORTANT]
> Nhánh `domain` đã thêm nhiều model mới (`Item`, `BidRecord` mở rộng, `LiveAuction`, DTOs) nhưng thiếu interface `AuctionObserver`. Đã tạo bổ sung — Duy Anh cần review lại interface này.
